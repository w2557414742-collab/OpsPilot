"""对实际 MySQL 与 Redis 上的告警接入执行有界并发验收。"""

from concurrent.futures import ThreadPoolExecutor
from contextlib import ExitStack
from datetime import datetime, timedelta, timezone
import argparse
import json
import os
from pathlib import Path
import secrets
import socket
from uuid import uuid4

import httpx

from verify_phase1 import ADMIN_PASSWORD, JAR, ROOT, api, login, port, start, stop_process, wait_for


def iso(instant):
    """让验收时间使用同一个 UTC 时间基准。"""
    return instant.isoformat().replace("+00:00", "Z")


def zcard(redis_port, key):
    """通过本机 Redis 的原生 RESP 协议读取投影计数。"""
    with socket.create_connection(("127.0.0.1", redis_port), timeout=2) as sock:
        command = f"*2\r\n$5\r\nZCARD\r\n${len(key)}\r\n{key}\r\n".encode()
        sock.sendall(command)
        with sock.makefile("rb") as response:
            return int(response.readline().removeprefix(b":"))


def verify(args, output):
    """覆盖去重、乱序、水位驱逐、缓存降级、审计和重启。"""
    tenant = f"phase2-{uuid4().hex[:10]}"
    second_tenant = tenant + "-b"
    source, service = "alerts", "documents"
    token = secrets.token_urlsafe(40)
    java_port = port()
    base_url = f"http://127.0.0.1:{java_port}"
    base_env = {**os.environ, "SERVER_ADDRESS": "127.0.0.1", "SERVER_PORT": str(java_port),
                "OPSPILOT_JWT_KEY": secrets.token_urlsafe(40), "OPSPILOT_AGENT_TOKEN": secrets.token_urlsafe(40),
                "OPSPILOT_BOOTSTRAP_USER": "admin", "OPSPILOT_BOOTSTRAP_PASSWORD": ADMIN_PASSWORD,
                "OPSPILOT_ALERT_TENANT": tenant, "OPSPILOT_ALERT_SOURCE": source, "OPSPILOT_ALERT_TOKEN": token,
                "OPSPILOT_ALERT_REDIS_ENABLED": "true", "OPSPILOT_REDIS_HOST": "127.0.0.1"}
    cmd = [args.java, "-jar", str(JAR)]
    base_time = datetime.now(timezone.utc) - timedelta(seconds=90)
    prefix = f"/ingest/v1/{tenant}/{source}"

    def event(key, offset=0, severity="WARNING"):
        """构造明确标注的测试数据，均指向受控服务。"""
        return {"eventId": key, "serviceId": service, "name": "TrafficErrors", "severity": severity,
                "status": "FIRING", "occurredAt": iso(base_time + timedelta(seconds=offset)),
                "labels": {"instance": "test-worker"}}

    with ExitStack() as stack:
        java = start(stack, cmd, {**base_env, "OPSPILOT_BOOTSTRAP_TENANT": tenant,
                                  "OPSPILOT_REDIS_PORT": str(args.redis_port)}, output / "java.log")
        web = stack.enter_context(httpx.Client(base_url=base_url, trust_env=False, timeout=5))
        wait_for(lambda: web.get("/actuator/health").status_code == 200)
        admin = stack.enter_context(login(base_url, tenant))
        api(admin, "POST", "/api/v1/services", 201, json={"id": service, "name": "Document Service"})
        api(web, "POST", prefix + "/events", 401, json=event("unauthorized"))
        ingest = stack.enter_context(httpx.Client(base_url=base_url, trust_env=False, timeout=15,
                                     headers={"Authorization": "Bearer " + token}))

        def send(payload, expected=202):
            """通过实际的标准事件入口提交并断言 HTTP 状态。"""
            return api(ingest, "POST", prefix + "/events", expected, json=payload)

        send({"eventId": "incomplete"}, 400)
        api(ingest, "POST", prefix + "/events", 400, content=b"",
            headers={"Content-Type": "application/json"})
        api(ingest, "POST", prefix + "/events", 413, content=b" " * (128 * 1024 + 1),
            headers={"Content-Type": "application/json"})
        send({**event("unknown"), "serviceId": "other"}, 404)
        send(event("future", 600), 400)
        original = json.dumps(event("first"), indent=2).encode()
        receipt = api(ingest, "POST", prefix + "/events", 202, content=original,
                      headers={"Content-Type": "application/json"})
        rid = receipt["receiptId"]
        assert api(admin, "GET", f"/api/v1/alerts/receipts/{rid}") == receipt
        response = admin.get(f"/api/v1/alerts/receipts/{rid}/raw")
        assert response.status_code == 200 and response.content == original
        assert response.headers["content-disposition"].startswith("attachment")

        with ThreadPoolExecutor(max_workers=8) as pool:
            replays = list(pool.map(lambda _: send(event("replay")), range(8)))
            unique = list(pool.map(lambda i: send(event(f"unique-{i}")), range(10)))
        records = [r["events"][0] for r in replays]
        assert len({r["eventId"] for r in records}) == 1
        assert sum(not r["duplicateEvent"] for r in records) == 1
        assert len({r["events"][0]["eventId"] for r in unique}) == 10
        send(event("replay", severity="CRITICAL"), 409)
        assert not send(event("within-window", -20))["events"][0]["late"]
        assert send(event("outside-window", -61))["events"][0]["late"]
        groups = api(admin, "GET", "/api/v1/alerts/groups")
        assert len(groups) == 1
        [group] = groups
        assert group["warningCount"] == 13 and group["suppressedCount"] == 8, group
        [candidate] = api(admin, "GET", "/api/v1/alerts/candidates")
        assert candidate["reason"] == "ALERT_STORM"
        key = "opspilot:alert:{" + tenant + ":" + group["id"] + "}:events"
        assert zcard(args.redis_port, key) == 13
        print("PASS 8 concurrent replays yield one durable event, 10 distinct events aggregate, storm cooldown holds",
              flush=True)

        new_group = {"version": "4", "alerts": [
            {"status": "firing", "startsAt": iso(base_time), "endsAt": iso(base_time + timedelta(minutes=4)),
             "labels": {"alertname": "ControllerDown", "service": service, "severity": "critical"},
             "annotations": {"summary": "Integration fixture only"}}
        ]}
        webhook = api(ingest, "POST", prefix + "/alertmanager", 202, json=new_group)
        new_group["alerts"][0]["endsAt"] = iso(base_time + timedelta(minutes=6))
        duplicate = api(ingest, "POST", prefix + "/alertmanager", 202, json=new_group)
        assert duplicate["events"][0]["duplicateEvent"]
        assert duplicate["events"][0]["eventId"] == webhook["events"][0]["eventId"]
        new_group["alerts"][0]["status"] = "resolved"
        new_group["alerts"][0]["endsAt"] = iso(base_time + timedelta(seconds=4))
        assert not api(ingest, "POST", prefix + "/alertmanager", 202, json=new_group)["events"][0]["duplicateEvent"]
        assert len(api(admin, "GET", "/api/v1/alerts/candidates")) == 2

        send(event("advance", 61))
        updated = next(g for g in api(admin, "GET", "/api/v1/alerts/groups") if g["id"] == group["id"])
        assert updated["warningCount"] == 1 and zcard(args.redis_port, key) == 1
        print("PASS Alertmanager v4 normalization, raw evidence, late archival and event-time eviction", flush=True)
        stop_process(java)

        start(stack, cmd, {**base_env, "OPSPILOT_BOOTSTRAP_TENANT": second_tenant,
                           "OPSPILOT_REDIS_PORT": str(port())}, output / "java-restart.log")
        wait_for(lambda: web.get("/actuator/health").status_code == 200)
        outsider = stack.enter_context(login(base_url, second_tenant))
        assert api(outsider, "GET", "/api/v1/alerts/groups") == []
        api(outsider, "GET", f"/api/v1/alerts/receipts/{rid}", 404)
        api(outsider, "GET", f"/api/v1/alerts/receipts/{rid}/raw", 404)
        assert send(event("first"))["events"][0]["eventId"] == receipt["events"][0]["eventId"]
        fallback = send(event("redis-offline", 62))
        assert not fallback["events"][0]["duplicateEvent"]
        final = next(g for g in api(admin, "GET", "/api/v1/alerts/groups") if g["id"] == group["id"])
        assert final["warningCount"] == 2
        metrics = admin.get("/actuator/prometheus")
        assert metrics.status_code == 200
        assert 'opspilot_alert_cache_projection_total{outcome="unavailable"} 1.0' in metrics.text
        print("PASS durable restart, cross-tenant evidence denial, healthy ingest and recorded fallback with Redis unavailable",
              flush=True)
        snapshot = {"groupBeforeEviction": group, "groupAfterEviction": updated, "groupWithRedisUnavailable": final,
                    "originalReceipt": receipt, "lastReceipt": fallback, "firstCandidate": candidate}
        (output / "snapshot.json").write_text(json.dumps(snapshot, indent=2), encoding="utf-8")


def main():
    """解析复现参数并在指定输出路径保留证据。"""
    parser = argparse.ArgumentParser()
    parser.add_argument("--java", default="java")
    parser.add_argument("--redis-port", type=int, default=16379)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    required = {"OPSPILOT_DB_URL", "OPSPILOT_DB_USER", "OPSPILOT_DB_PASSWORD"}
    if not JAR.is_file() or not required.issubset(os.environ):
        parser.error("Build the Java jar and set OPSPILOT_DB_URL, OPSPILOT_DB_USER and OPSPILOT_DB_PASSWORD")
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    verify(args, output)


if __name__ == "__main__":
    main()
