"""SQLite 运行记录与状态流水，和 Java 的 MySQL 独立部署。"""

from contextlib import contextmanager
import hashlib
from pathlib import Path
import sqlite3
import time
from uuid import uuid4

from .models import DiagnosisRequest, ProviderName, Report, Run, RunEvent


class PayloadConflict(Exception):
    """同一租户与 run ID 复用了不同负载。"""


class Store:
    """在同一 SQLite 写事务内维护 run 与事件。每次操作使用独立连接。"""

    def __init__(self, path: Path):
        """设置持久化路径，并初始化 WAL 和数据表。"""
        path.parent.mkdir(parents=True, exist_ok=True)
        self.path = path
        with self._connection() as conn:
            conn.execute("PRAGMA journal_mode=WAL")
            conn.executescript("""
                CREATE TABLE IF NOT EXISTS run (
                    tenant TEXT NOT NULL, id TEXT NOT NULL, payload TEXT, digest TEXT, provider TEXT,
                    status TEXT NOT NULL, result TEXT, error TEXT, lease TEXT, expiry REAL,
                    created_at REAL NOT NULL, PRIMARY KEY (tenant, id)
                );
                CREATE INDEX IF NOT EXISTS run_queue ON run(status, created_at);
                CREATE TABLE IF NOT EXISTS run_event (
                    id INTEGER PRIMARY KEY, tenant TEXT NOT NULL, run_id TEXT NOT NULL,
                    status TEXT NOT NULL, at REAL NOT NULL,
                    FOREIGN KEY (tenant, run_id) REFERENCES run(tenant, id)
                );
                CREATE INDEX IF NOT EXISTS event_cursor ON run_event(tenant, run_id, id);
            """)

    @contextmanager
    def _connection(self, write=False):
        """写事务先取得保留锁，使读改写和事件提交不可分割。"""
        conn = sqlite3.connect(self.path, timeout=2, isolation_level=None)
        try:
            conn.row_factory = sqlite3.Row
            conn.execute("PRAGMA foreign_keys=ON")
            if write:
                conn.execute("BEGIN IMMEDIATE")
            yield conn
            if write:
                conn.commit()
        finally:
            conn.close()

    @staticmethod
    def _row(conn, tenant, run_id):
        """用复合主键查询运行快照。"""
        return conn.execute("SELECT * FROM run WHERE tenant=? AND id=?", (tenant, run_id)).fetchone()

    @staticmethod
    def _event(conn, tenant, run_id, status):
        """在调用方事务内记录状态变更。"""
        conn.execute("INSERT INTO run_event(tenant,run_id,status,at) VALUES(?,?,?,?)",
                     (tenant, run_id, status, time.time()))

    @staticmethod
    def _view(row):
        """将持久化 JSON 验证为对外契约。"""
        return Run(runId=row["id"], status=row["status"], errorCode=row["error"],
                   result=Report.model_validate_json(row["result"]) if row["result"] else None)

    def submit(self, tenant: str, run_id: str, request: DiagnosisRequest, provider: ProviderName) -> Run:
        """以租户和稳定 run ID 去重，支持提交响应丢失后的相同请求重放。"""
        payload = request.model_dump_json()
        digest = hashlib.sha256(payload.encode()).hexdigest()
        with self._connection(write=True) as conn:
            row = self._row(conn, tenant, run_id)
            if row:
                if row["digest"] is not None and row["digest"] != digest:
                    raise PayloadConflict
                return self._view(row)
            conn.execute("""
                INSERT INTO run(tenant,id,payload,digest,provider,status,created_at)
                VALUES(?,?,?,?,?,'QUEUED',?)
            """, (tenant, run_id, payload, digest, provider, time.time()))
            self._event(conn, tenant, run_id, "QUEUED")
            return self._view(self._row(conn, tenant, run_id))

    def get(self, tenant: str, run_id: str) -> Run | None:
        """读取租户内已知运行状态。"""
        with self._connection() as conn:
            row = self._row(conn, tenant, run_id)
            return self._view(row) if row else None

    def cancel(self, tenant: str, run_id: str) -> Run:
        """取消未启动任务或登记取消请求，支持取消先于延迟提交到达。"""
        with self._connection(write=True) as conn:
            row = self._row(conn, tenant, run_id)
            if row is None:
                conn.execute("INSERT INTO run(tenant,id,status,created_at) VALUES(?,?,'CANCELLED',?)",
                             (tenant, run_id, time.time()))
                self._event(conn, tenant, run_id, "CANCELLED")
            elif row["status"] in ("QUEUED", "RUNNING"):
                status = "CANCELLED" if row["status"] == "QUEUED" else "CANCEL_REQUESTED"
                conn.execute("UPDATE run SET status=? WHERE tenant=? AND id=?", (status, tenant, run_id))
                self._event(conn, tenant, run_id, status)
            return self._view(self._row(conn, tenant, run_id))

    def events(self, tenant: str, run_id: str, after: int) -> list[RunEvent] | None:
        """以序号增量读取最多 100 条状态事件。"""
        with self._connection() as conn:
            if self._row(conn, tenant, run_id) is None:
                return None
            rows = conn.execute("""
                SELECT id,status,at FROM run_event WHERE tenant=? AND run_id=? AND id>?
                ORDER BY id LIMIT 100
            """, (tenant, run_id, after))
            return [RunEvent(**dict(row)) for row in rows]

    def claim(self, lease_seconds: float = 30):
        """回收过期执行并领取下一项运行，租约内执行发生在 SQLite 事务外。"""
        with self._connection(write=True) as conn:
            now = time.time()
            stale = conn.execute("""
                SELECT tenant,id,provider,status FROM run
                WHERE status IN ('RUNNING','CANCEL_REQUESTED') AND expiry<=? LIMIT 100
            """, (now,)).fetchall()
            for row in stale:
                if row["provider"] != "DETERMINISTIC_TEST":
                    status, error = "UNKNOWN_OUTCOME", "REMOTE_EXECUTION_UNCERTAIN"
                elif row["status"] == "CANCEL_REQUESTED":
                    status, error = "CANCELLED", None
                else:
                    status, error = "QUEUED", None
                conn.execute("UPDATE run SET status=?,error=?,lease=NULL,expiry=NULL WHERE tenant=? AND id=?",
                             (status, error, row["tenant"], row["id"]))
                self._event(conn, row["tenant"], row["id"], status)
            row = conn.execute("SELECT * FROM run WHERE status='QUEUED' ORDER BY created_at,id LIMIT 1").fetchone()
            if row is None:
                return None
            lease = str(uuid4())
            conn.execute("UPDATE run SET status='RUNNING',lease=?,expiry=? WHERE tenant=? AND id=?",
                         (lease, now + lease_seconds, row["tenant"], row["id"]))
            self._event(conn, row["tenant"], row["id"], "RUNNING")
            return row, lease

    def finish(self, tenant, run_id, lease, report=None, error=None, uncertain=False) -> bool:
        """只有当前有效令牌可结算，远程执行结果不明时进入 UNKNOWN_OUTCOME。"""
        with self._connection(write=True) as conn:
            row = self._row(conn, tenant, run_id)
            if row["lease"] != lease or row["expiry"] <= time.time():
                return False
            if uncertain:
                status = "UNKNOWN_OUTCOME"
            elif row["status"] == "CANCEL_REQUESTED":
                status, report, error = "CANCELLED", None, None
            else:
                status = "SUCCEEDED" if report is not None else "FAILED"
            result = report.model_dump_json() if report else None
            conn.execute("""
                UPDATE run SET status=?, result=?, error=?, lease=NULL, expiry=NULL WHERE tenant=? AND id=?
            """, (status, result, error, tenant, run_id))
            self._event(conn, tenant, run_id, status)
            return True
