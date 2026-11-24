# Phase 1 功能测试报告

环境为 Windows amd64、Microsoft OpenJDK 21.0.9+10、Maven 3.9.4、Python 3.13.5 和 Docker Desktop Linux 引擎。MySQL 8.4.8 绑定本机。诊断使用 DETERMINISTIC_TEST。

| 检查 | 结果 | 原始证据 |
| --- | --- | --- |
| JUnit 状态矩阵 | 2 项通过 | [Maven 构建](artifacts/phase1/java-build.log) |
| FastAPI／SQLite | 4 项通过 | [pytest](artifacts/phase1/python-tests.log) |
| 双进程与 MySQL | 通过 | [端到端日志](artifacts/phase1/end-to-end.log) |
| 工单、任务、Java 审计、Python run 流水 | 跨进程断言通过 | [接口快照](artifacts/phase1/snapshot.json) |

端到端验收观察到两次同版本转换仅一项成功。四次重复诊断请求得到同一任务。Python 已提交后，代理主动丢弃首次响应，Java 重放经同一 run 恢复，Python 仅产生一组 QUEUED、RUNNING、SUCCEEDED 事件。过期工单版本的结果写入历史任务，工单保持更新后的版本。Agent 停止时 Java 继续办理工单，双进程重启后排队任务完成、已有运行流水仍可读、挂起取消得到确认。

pytest 发出 Starlette TestClient 使用旧 anyio 接口的弃用提示。未运行真实模型、压力评测或生产外部操作。

复现见 [Windows 指南](WINDOWS_QUICKSTART.md)。提交与命令见 [Phase 1 归档说明](artifacts/phase1/README.md)。
