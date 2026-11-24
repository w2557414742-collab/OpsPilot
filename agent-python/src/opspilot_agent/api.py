"""FastAPI 内部接口及本地队列消费者。"""

import asyncio
from contextlib import asynccontextmanager
import hmac
import logging
import os
from pathlib import Path
import sqlite3
from typing import Annotated
from uuid import UUID

from fastapi import Depends, FastAPI, Header, HTTPException, Path as PathParam, Query

from .models import DiagnosisRequest, Run, RunEvent
from .providers import Provider, configured_provider
from .store import PayloadConflict, Store

LOG = logging.getLogger(__name__)
Tenant = Annotated[str, PathParam(pattern=r"^[a-z0-9][a-z0-9_-]{0,47}$")]


def work_once(store: Store, provider: Provider) -> bool:
    """领取并执行一个 run。校验固定的 provider，区分失败与付费调用结果未知。"""
    claimed = store.claim()
    if claimed is None:
        return False
    row, lease = claimed
    tenant, run_id = row["tenant"], row["id"]
    if row["provider"] != provider.name:
        store.finish(tenant, run_id, lease, error="PROVIDER_MISMATCH")
        return True
    try:
        result = provider.diagnose(DiagnosisRequest.model_validate_json(row["payload"]))
    except Exception as exc:
        LOG.warning("Diagnosis failed run=%s class=%s", run_id, type(exc).__name__)
        store.finish(tenant, run_id, lease, error="PROVIDER_EXECUTION_FAILED",
                     uncertain=provider.name != "DETERMINISTIC_TEST")
    else:
        store.finish(tenant, run_id, lease, report=result)
    return True


def create_app(path: Path, token: str, provider: Provider, start_worker: bool = True) -> FastAPI:
    """构建独立 Agent 服务。测试可手动推进 worker 以断言状态边界。"""
    if len(token) < 32:
        raise ValueError("Agent token requires at least 32 characters")
    store = Store(path)

    async def dispatch(stop: asyncio.Event):
        """在后台轮询数据库，SQLite 锁冲突时下轮继续。"""
        while not stop.is_set():
            try:
                await asyncio.to_thread(work_once, store, provider)
            except sqlite3.Error as exc:
                LOG.warning("SQLite worker error class=%s", type(exc).__name__)
            try:
                await asyncio.wait_for(stop.wait(), timeout=0.1)
            except TimeoutError:
                continue

    @asynccontextmanager
    async def lifespan(_app):
        """启停调度循环，关闭服务前等待正在执行的步骤结束。"""
        stop = asyncio.Event()
        loop = asyncio.create_task(dispatch(stop)) if start_worker else None
        try:
            yield
        finally:
            stop.set()
            if loop:
                await loop

    def authorize(authorization: Annotated[str | None, Header()] = None) -> None:
        """用常量时间比较校验 Java 专用服务令牌。"""
        if not hmac.compare_digest((authorization or "").encode(), f"Bearer {token}".encode()):
            raise HTTPException(401, "Invalid service credentials")

    app = FastAPI(title="OpsPilot Agent", version="0.1.0", lifespan=lifespan,
                  docs_url=None, redoc_url=None, openapi_url=None)
    internal = "/internal/v1/tenants/{tenant}/runs/{run_id}"
    service_auth = [Depends(authorize)]

    @app.get("/health")
    def health():
        """暴露进程健康与当前 provider 模式。"""
        return {"status": "UP", "provider": provider.name}

    @app.put(internal, response_model=Run, status_code=202, dependencies=service_auth)
    def submit(tenant: Tenant, run_id: UUID, request: DiagnosisRequest):
        """异步接受运行任务。重复请求返回已持久化状态。"""
        try:
            return store.submit(tenant, str(run_id), request, provider.name)
        except PayloadConflict as exc:
            raise HTTPException(409, "Run ID reused with different payload") from exc

    @app.get(internal, response_model=Run, dependencies=service_auth)
    def get(tenant: Tenant, run_id: UUID):
        """获取运行状态与结果。"""
        run = store.get(tenant, str(run_id))
        if run is None:
            raise HTTPException(404, "Run not found")
        return run

    @app.put(internal + "/cancel", response_model=Run, status_code=202, dependencies=service_auth)
    def cancel(tenant: Tenant, run_id: UUID):
        """提交幂等取消请求或读取终态。"""
        return store.cancel(tenant, str(run_id))

    @app.get(internal + "/events", response_model=list[RunEvent], dependencies=service_auth)
    def events(tenant: Tenant, run_id: UUID, after: Annotated[int, Query(ge=0)] = 0):
        """获取增量运行状态流水。"""
        result = store.events(tenant, str(run_id), after)
        if result is None:
            raise HTTPException(404, "Run not found")
        return result

    return app


def from_env() -> FastAPI:
    """Uvicorn 工厂，从环境加载存储路径、内部凭证和 provider。"""
    return create_app(Path(os.environ.get("OPSPILOT_AGENT_DB", "data/agent.sqlite3")),
                      os.environ["OPSPILOT_AGENT_TOKEN"], configured_provider())
