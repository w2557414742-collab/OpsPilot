# Phase 1 与 Phase 2 功能测试报告

## Phase 1

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

## Phase 2

在 Java 21、MySQL 8.4.8 和 Redis 7.4.8 上运行。

- 8 次并发发送相同事件只生成一行告警记录。10 个同时提交的不同事件各自入库。计数达到设定阈值后只生成一个同组候选，并记录冷却期间的抑制数。
- 迟到且在窗口内的事件计数，越过左边界的事件只归档。水位推进后，MySQL 与 Redis 都将目标分组的窗口计数从 13 更新到 1。
- Alertmanager v4 封装按状态解析，重复的 FIRING 通知复用事件 UUID。原始字节可比对，跨租户下载返回 404。
- Java 重启并指向无法连接的 Redis 后，仍成功接收事件，MySQL 计数再增加一条，Micrometer 记录一次缓存投影失败。Phase 1 全链路回归通过。

原始 [端到端日志](artifacts/phase2/end-to-end.log)、[接口快照](artifacts/phase2/snapshot.json) 和 [Phase 1 回归](artifacts/phase2/phase1-regression.log) 已归档。本轮使用 Alertmanager v4 请求夹具通过真实 HTTP 验收，Alertmanager 进程联调与高负载性能测试为 NOT_RUN。

Flyway 对 MySQL 8.4 输出兼容性警告。本项目在实际 8.4.8 实例上验证了 V1 建库、V2 增量迁移和重启后校验。[Phase 2 Java 原始日志](artifacts/phase2/java.log)

本机 Docker 默认地址池已耗尽。为验收项目单独配置私有子网后，Compose 中的 MySQL 和 Redis 均通过健康检查。[首次启动日志](artifacts/phase2/compose-default-pool-failure.log) · [独立子网启动与停止日志](artifacts/phase2/compose.log)
