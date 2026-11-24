"""验证 SQLite 恢复、租约和内部 HTTP 契约的边界行为。"""

from concurrent.futures import ThreadPoolExecutor
from uuid import uuid4

from fastapi.testclient import TestClient

from opspilot_agent.api import create_app, work_once
from opspilot_agent.models import DiagnosisRequest
from opspilot_agent.providers import DeterministicProvider
from opspilot_agent.store import Store

TOKEN = "test-agent-shared-token-for-ci-only"
HEADERS = {"Authorization": f"Bearer {TOKEN}"}
SAMPLE = {
    "incidentId": "6fbc94cf-2e1a-43b8-99ed-912ee8d1bf72",
    "serviceId": "documents",
    "title": "Test request latency",
    "description": "Test fixture without direct telemetry",
    "traceId": "12345678901234567890123456789012",
}


def test_idempotent_http_and_restart(tmp_path):
    """重复提交只执行一个 run，重建应用后保留完整流水。"""
    path = tmp_path / "agent.db"
    provider = DeterministicProvider()
    url = f"/internal/v1/tenants/tenant-a/runs/{uuid4()}"
    with TestClient(create_app(path, TOKEN, provider, False)) as client:
        assert client.get(url).status_code == 401
        with ThreadPoolExecutor(max_workers=6) as pool:
            responses = list(pool.map(lambda _: client.put(url, json=SAMPLE, headers=HEADERS), range(6)))
        assert all(r.status_code == 202 and r.json()["status"] == "QUEUED" for r in responses)
        assert client.put(url, json={**SAMPLE, "title": "changed"}, headers=HEADERS).status_code == 409

    assert work_once(Store(path), provider)
    assert not work_once(Store(path), provider)
    with TestClient(create_app(path, TOKEN, provider, False)) as client:
        result = client.get(url, headers=HEADERS).json()
        assert result["status"] == "SUCCEEDED"
        assert result["result"]["evidence"] == []
        assert result["result"]["provider"] == "DETERMINISTIC_TEST"
        assert client.get(url.replace("tenant-a", "tenant-b"), headers=HEADERS).status_code == 404
        events = client.get(url + "/events", headers=HEADERS).json()
        assert [e["status"] for e in events] == ["QUEUED", "RUNNING", "SUCCEEDED"]
        assert client.get(url + "/events", params={"after": events[1]["id"]}, headers=HEADERS).json() == events[2:]


def test_cancel_before_delayed_submit(tmp_path):
    """取消标记屏蔽迟到 PUT，不会创建第二次执行。"""
    path = tmp_path / "agent.db"
    provider = DeterministicProvider()
    url = f"/internal/v1/tenants/tenant-a/runs/{uuid4()}"
    with TestClient(create_app(path, TOKEN, provider, False)) as client:
        assert client.put(url + "/cancel", headers=HEADERS).json()["status"] == "CANCELLED"
        assert client.put(url, json=SAMPLE, headers=HEADERS).json()["status"] == "CANCELLED"
        assert not work_once(Store(path), provider)


def test_expired_lease_reclaims_only_deterministic(tmp_path):
    """重启可以恢复确定性 run。未知的模型执行进入对账状态。"""
    store = Store(tmp_path / "agent.db")
    request = DiagnosisRequest(**SAMPLE)
    provider = DeterministicProvider()
    deterministic, external = str(uuid4()), str(uuid4())
    store.submit("tenant-a", deterministic, request, provider.name)
    _, stale_lease = store.claim(lease_seconds=0)
    assert work_once(store, provider)
    assert not store.finish("tenant-a", deterministic, stale_lease, provider.diagnose(request))
    assert store.get("tenant-a", deterministic).status == "SUCCEEDED"
    store.submit("tenant-a", external, request, "OPENAI_COMPATIBLE")
    store.claim(lease_seconds=0)
    assert not work_once(store, provider)
    assert store.get("tenant-a", external).status == "UNKNOWN_OUTCOME"


def test_running_cancel_requires_worker_ack(tmp_path):
    """执行中取消先记录请求，工作完成时丢弃报告并确认。"""
    store = Store(tmp_path / "agent.db")
    request, run_id = DiagnosisRequest(**SAMPLE), str(uuid4())
    provider = DeterministicProvider()
    store.submit("tenant-a", run_id, request, provider.name)
    _, lease = store.claim()
    assert store.cancel("tenant-a", run_id).status == "CANCEL_REQUESTED"
    assert store.finish("tenant-a", run_id, lease, report=provider.diagnose(request))
    assert store.get("tenant-a", run_id).status == "CANCELLED"
    assert store.get("tenant-a", run_id).result is None
