"""以真实 Java、MySQL、Python 进程校验最小闭环。"""

import argparse
from concurrent.futures import ThreadPoolExecutor
from contextlib import ExitStack
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import threading
import time
from uuid import uuid4

import httpx

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "backend-java/ops-bootstrap/target/ops-bootstrap-0.1.0-SNAPSHOT.jar"
ADMIN_PASSWORD = "disposable-phase1-account"
TERMINAL = {"SUCCEEDED", "FAILED", "CANCELLED", "UNKNOWN_OUTCOME"}


def port():
    """分配一个空闲的回环测试端口。"""
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def wait_for(check, timeout=45):
    """在有界等待内轮询真实进程状态。"""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            result = check()
            if result:
                return result
        except httpx.TransportError:
            pass
        time.sleep(0.15)
    raise AssertionError(f"Condition did not resolve within {timeout}s")


def stop_process(proc):
    """仅停止本脚本启动的子进程。"""
    if proc.poll() is None:
        proc.terminate()
        try:
            proc.wait(5)
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait(5)


def start(stack, cmd, env, log):
    """启动服务并保留进程输出，退出测试时回收该进程。"""
    handle = stack.enter_context(log.open("wb"))
    proc = subprocess.Popen(cmd, cwd=ROOT, env=env, stdout=handle, stderr=subprocess.STDOUT)
    stack.callback(stop_process, proc)
    return proc


class AgentProxy(BaseHTTPRequestHandler):
    """转发到本机 Agent。仅第一次提交在 Python 确认持久化后丢弃响应。"""

    def do_PUT(self):
        """转发或丢弃一次提交响应，以验证稳定 run ID 重放。"""
        self.forward()

    def do_GET(self):
        """转发轮询请求。"""
        self.forward()

    def forward(self):
        """固定上游，注入受控丢包与迟到结果。"""
        if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
            body = self.read_chunks()
        else:
            body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
        try:
            response = httpx.request(self.command, self.server.upstream + self.path, content=body,
                                     headers={"Authorization": self.headers["Authorization"],
                                              "Content-Type": "application/json"},
                                     timeout=3, trust_env=False)
        except httpx.TransportError:
            self.reply(503, b'{"error":"local Agent unavailable"}')
            return
        is_submit = self.command == "PUT" and not self.path.endswith("/cancel")
        if is_submit and response.status_code == 202:
            with self.server.fault_lock:
                if self.server.drop_once:
                    self.server.drop_once = False
                    self.server.dropped_id = response.json()["runId"]
                    self.close_connection = True
                    return
            if json.loads(body)["title"] == "Controlled late result":
                self.server.late_arrived.set()
                self.server.release_late.wait(3)
        self.reply(response.status_code, response.content)

    def reply(self, status, content):
        """返回本机上游响应。被停止的测试客户端可能已关闭连接。"""
        try:
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(content)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def read_chunks(self):
        """从 Java HTTP 客户端读取由分块传输发送的 JSON。"""
        chunks = []
        while size := int(self.rfile.readline(64).partition(b";")[0], 16):
            chunks.append(self.rfile.read(size))
            self.rfile.read(2)
        while self.rfile.readline(1024) not in (b"\r\n", b""):
            pass
        return b"".join(chunks)

    def log_message(self, *_args):
        """避免将请求凭证记入测试日志。"""


def api(client, method, route, expected=200, **kwargs):
    """断言 HTTP 状态并返回 JSON 负载。"""
    response = client.request(method, route, **kwargs)
    assert response.status_code == expected, (method, route, response.status_code, response.text)
    return response.json() if response.content else None


def login(base, tenant, username="admin", password=ADMIN_PASSWORD):
    """向实际认证服务申请一枚租户访问令牌。"""
    with httpx.Client(base_url=base, trust_env=False) as client:
        response = wait_for(lambda: login_ready(client, tenant, username, password), timeout=10)
    return httpx.Client(base_url=base, headers={"Authorization": "Bearer " + response.json()["accessToken"]},
                        trust_env=False)

def login_ready(client, tenant, username, password):
    """等待启动初始化写入管理员账号。"""
    response = client.post("/api/v1/auth/token", json={"tenant": tenant, "username": username, "password": password})
    return response if response.status_code == 200 else None


def create_incident(client, asset, title):
    """创建关联服务的工单并进入 TRIAGING。"""
    incident = api(client, "POST", "/api/v1/incidents", 201,
                   json={"serviceId": asset, "title": title, "description": "Integration test fixture"})
    return api(client, "POST", f"/api/v1/incidents/{incident['id']}/transitions",
               json={"target": "TRIAGING", "expectedVersion": incident["version"]})


def submit(client, incident, key):
    """以当前版本与幂等键请求诊断。"""
    return api(client, "POST", f"/api/v1/incidents/{incident['id']}/diagnoses", 202,
               json={"expectedVersion": incident["version"]}, headers={"Idempotency-Key": key})


def terminal_task(client, task_id):
    """读取真实任务终态。"""
    task = api(client, "GET", f"/api/v1/tasks/{task_id}")
    return task if task["status"] in TERMINAL else None


def verify(args, output):
    """运行竞争写、跨租户、丢响应、迟到、取消与双进程重启场景。"""
    token, jwt = secrets.token_urlsafe(40), secrets.token_urlsafe(40)
    java_port, agent_port = port(), port()
    java_base, agent_base = f"http://127.0.0.1:{java_port}", f"http://127.0.0.1:{agent_port}"
    prefix = uuid4().hex[:10]
    tenant_a, tenant_b = f"phase1-{prefix}-a", f"phase1-{prefix}-b"
    common = {**os.environ, "OPSPILOT_AGENT_TOKEN": token, "OPSPILOT_PROVIDER": "DETERMINISTIC_TEST",
              "PYTHONPATH": str(ROOT / "agent-python/src"),
              "OPSPILOT_AGENT_DB": str(output / "runtime" / "agent.sqlite3"),
              "OPSPILOT_JWT_KEY": jwt, "SERVER_ADDRESS": "127.0.0.1", "SERVER_PORT": str(java_port),
              "OPSPILOT_BOOTSTRAP_USER": "admin", "OPSPILOT_BOOTSTRAP_PASSWORD": ADMIN_PASSWORD,
              "OPSPILOT_TASK_TICK_MS": "100"}
    agent_cmd = [sys.executable, "-m", "uvicorn", "opspilot_agent.api:from_env", "--factory",
                 "--host", "127.0.0.1", "--port", str(agent_port)]
    java_cmd = [args.java, "-jar", str(JAR)]

    with ExitStack() as stack:
        proxy = ThreadingHTTPServer(("127.0.0.1", 0), AgentProxy)
        proxy.upstream, proxy.drop_once, proxy.dropped_id = agent_base, True, None
        proxy.fault_lock = threading.Lock()
        proxy.late_arrived, proxy.release_late = threading.Event(), threading.Event()
        thread = threading.Thread(target=proxy.serve_forever, daemon=True)
        thread.start()
        stack.callback(proxy.server_close)
        stack.callback(proxy.shutdown)
        common["OPSPILOT_AGENT_URL"] = f"http://127.0.0.1:{proxy.server_port}"
        plain = stack.enter_context(httpx.Client(trust_env=False, timeout=2))
        python = start(stack, agent_cmd, common, output / "python.log")
        wait_for(lambda: plain.get(agent_base + "/health").status_code == 200)
        java = start(stack, java_cmd, {**common, "OPSPILOT_BOOTSTRAP_TENANT": tenant_a}, output / "java.log")
        wait_for(lambda: plain.get(java_base + "/actuator/health").status_code == 200)
        a = stack.enter_context(login(java_base, tenant_a))

        api(a, "POST", "/api/v1/auth/users", 201,
            json={"username": "reader", "password": ADMIN_PASSWORD, "role": "VIEWER"})
        reader = stack.enter_context(login(java_base, tenant_a, "reader"))
        asset = "document-service"
        api(a, "POST", "/api/v1/services", 201, json={"id": asset, "name": "Document Service"})
        api(reader, "GET", "/api/v1/services")
        api(reader, "POST", "/api/v1/services", 403, json={"id": "forbidden", "name": "Denied"})
        incident = api(a, "POST", "/api/v1/incidents", 201,
                       json={"serviceId": asset, "title": "Drop response case", "description": "HTTP recovery fixture"})
        route = f"/api/v1/incidents/{incident['id']}"
        with ThreadPoolExecutor(max_workers=2) as pool:
            results = list(pool.map(lambda _: a.post(route + "/transitions",
                                    json={"target": "TRIAGING", "expectedVersion": 0}), range(2)))
        assert sorted(r.status_code for r in results) == [200, 409]
        incident = api(a, "GET", route)
        with ThreadPoolExecutor(max_workers=4) as pool:
            submissions = list(pool.map(lambda _: submit(a, incident, "same-key"), range(4)))
        task_id = submissions[0]["id"]
        assert {item["id"] for item in submissions} == {task_id}
        task = wait_for(lambda: terminal_task(a, task_id))
        assert task["status"] == "SUCCEEDED" and task["runId"] == task_id == proxy.dropped_id
        assert task["attempt"] >= 2 and task["result"]["provider"] == "DETERMINISTIC_TEST"
        assert task["result"]["conclusion"] == "INSUFFICIENT_EVIDENCE" and task["result"]["evidence"] == []
        after = api(a, "GET", route)
        assert (after["status"], after["version"]) == ("MANUAL_REVIEW", 3)
        events = api(a, "GET", route + "/events")
        assert [event["eventType"] for event in events] == [
            "INCIDENT_CREATED", "INCIDENT_TRANSITION", "INCIDENT_TRANSITION", "TASK_QUEUED",
            "INCIDENT_TRANSITION", "TASK_SUCCEEDED"]
        agent_route = f"{agent_base}/internal/v1/tenants/{tenant_a}/runs/{task_id}"
        agent_headers = {"Authorization": "Bearer " + token}
        agent_events = api(plain, "GET", agent_route + "/events", headers=agent_headers)
        assert [event["status"] for event in agent_events] == ["QUEUED", "RUNNING", "SUCCEEDED"]
        print("PASS MySQL optimistic concurrency, RBAC, stable submission replay, one Python execution", flush=True)

        late = create_incident(a, asset, "Controlled late result")
        late_route = f"/api/v1/incidents/{late['id']}"
        late_task = submit(a, late, "late")
        assert proxy.late_arrived.wait(6)
        api(a, "POST", late_route + "/transitions",
            json={"target": "MANUAL_REVIEW", "expectedVersion": 2})
        api(a, "POST", late_route + "/transitions",
            json={"target": "TRIAGING", "expectedVersion": 3})
        proxy.release_late.set()
        assert wait_for(lambda: terminal_task(a, late_task["id"]))["status"] == "SUCCEEDED"
        assert api(a, "GET", late_route)["version"] == 4
        assert api(a, "GET", late_route + "/events")[-1]["eventType"] == "TASK_RESULT_STALE"
        print("PASS late diagnosis is retained while the newer incident state remains unchanged", flush=True)

        stop_process(python)
        pending = create_incident(a, asset, "Agent down recovery")
        offline_task = submit(a, pending, "recovery")
        wait_for(lambda: api(a, "GET", f"/api/v1/tasks/{offline_task['id']}")["errorCode"] == "AGENT_HTTP_UNAVAILABLE")
        cancel_incident = create_incident(a, asset, "Cancel while Agent unavailable")
        cancel_task = submit(a, cancel_incident, "cancel")
        cancel_route = f"/api/v1/tasks/{cancel_task['id']}"
        wait_for(lambda: api(a, "GET", cancel_route)["errorCode"] == "AGENT_HTTP_UNAVAILABLE")
        assert api(a, "POST", cancel_route + "/cancel", 202)["status"] == "CANCEL_REQUESTED"
        stop_process(java)
        java = start(stack, java_cmd, {**common, "OPSPILOT_BOOTSTRAP_TENANT": tenant_b},
                     output / "java-restart.log")
        wait_for(lambda: plain.get(java_base + "/actuator/health").status_code == 200)
        b = stack.enter_context(login(java_base, tenant_b))
        api(b, "GET", route, 404)
        api(b, "GET", f"/api/v1/tasks/{task_id}", 404)
        api(b, "GET", route + "/events", 404)
        assert api(b, "GET", "/api/v1/services") == []
        assert api(a, "GET", route)["status"] == "MANUAL_REVIEW"
        start(stack, agent_cmd, common, output / "python-restart.log")
        wait_for(lambda: plain.get(agent_base + "/health").status_code == 200)
        assert api(plain, "GET", agent_route + "/events", headers=agent_headers) == agent_events
        assert wait_for(lambda: terminal_task(a, offline_task["id"]))["status"] == "SUCCEEDED"
        assert wait_for(lambda: terminal_task(a, cancel_task["id"]))["status"] == "CANCELLED"
        print("PASS Java business remains available during Agent outage, durable recovery, confirmed cancellation, tenant isolation",
              flush=True)
        snapshot = {"incident": after, "replayedTask": task, "javaIncidentEvents": events,
                    "pythonRunEvents": agent_events,
                    "cancelledTask": api(a, "GET", cancel_route),
                    "recoveredTask": api(a, "GET", f"/api/v1/tasks/{offline_task['id']}")}
        (output / "snapshot.json").write_text(json.dumps(snapshot, indent=2), encoding="utf-8")


def main():
    """校验依赖并运行可重复的回环验收。"""
    parser = argparse.ArgumentParser()
    parser.add_argument("--java", default="java")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    expected = {"OPSPILOT_DB_URL", "OPSPILOT_DB_USER", "OPSPILOT_DB_PASSWORD"}
    if not JAR.is_file() or not expected.issubset(os.environ):
        parser.error("Build the Java jar and set OPSPILOT_DB_URL, OPSPILOT_DB_USER and OPSPILOT_DB_PASSWORD")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    verify(args, output)


if __name__ == "__main__":
    main()
