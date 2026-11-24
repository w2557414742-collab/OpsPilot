# OpsPilot 源码审查交接

| 字段 | 当前值 |
| --- | --- |
| 项目 | OpsPilot |
| GitHub | 待用户完成 GitHub 登录并确认目标，当前无远程地址 |
| 当前分支 | `main` |
| 最新业务验收归档 SHA | `44f5fb2ece09b45df8b0842ce94740675812055c` |
| 源码审查与发布候选 SHA | `d530c8a21c8507efca2fa63f0be6e6e21cd7f49b` |
| 最新归档提交 | 在实际仓库执行 `git rev-parse HEAD`。发布整理提交见 [发布报告](GITHUB_PUBLISH_REPORT.md) |

## 从这里读源码

1. 先读 [项目状态](PROJECT_STATUS.md)、[目录树](PROJECT_TREE.txt) 和 [架构](ARCHITECTURE.md)。
2. 对照 [工单状态机](INCIDENT_STATE_MACHINE.md) 与 Java 方法参考。顺着 `OperationsApi`、`IncidentService`、`OpsStore` 理解多租户、事务和版本检查。
3. 随 [任务引擎](AGENT_TASK_ENGINE.md) 和 [调用流程](CALL_FLOW.md) 阅读 `TaskWorker`、`AgentClient`、Python `create_app`、`work_once` 与 `Store`。
4. 用 [告警设计](docs/alerts.md) 阅读 `AlertNormalization`、`AlertIngestion`、`AlertStore` 与 `AlertCache`，对照验收证据理解窗口推进、重复事件与 Redis 降级。
5. 复现 [测试报告](TEST_REPORT.md)，再从 [面试指南](INTERVIEW_GUIDE.md) 练习解释真实行为。

[Java 方法索引](METHOD_REFERENCE.md) · [Python 函数索引](FUNCTION_REFERENCE.md) · [接口契约](API_CONTRACT.md) · [数据库设计](DATABASE_DESIGN.md)

## 已实现的边界

Java 使用 Spring Boot、JWT、MyBatis 与 MySQL 管理账号、资产、工单、告警、任务和审计。Python 使用 FastAPI 与 SQLite WAL 管理异步诊断运行态。Redis Lua 负责可选告警窗口投影。当前技术难点包括丢响应后的稳定运行 ID、工单版本与任务租约、告警去重和数据库与缓存之间的故障边界。

本轮本地复跑 2 项 JUnit 和 4 项 pytest。上一轮端到端在真实 MySQL、Redis 与两个应用进程上通过。Flyway 有 MySQL 版本提醒，迁移已实测。Docker 默认地址池耗尽可使用 IPAM 覆盖文件。[测试报告](TEST_REPORT.md)

## 尚待实现或执行

- RocketMQ 与 Outbox/Inbox、真实遥测、LangGraph、MCP、RCA 排序、审批恢复和 Vue 为 NOT_IMPLEMENTED。
- 性能 Benchmark、RCA 精度、真实模型与 GitHub Actions 为 NOT_RUN。
- Phase 3 计划先引入 RocketMQ 与事务型 Outbox/Inbox，再测试发布与消费崩溃窗口。以 [开发计划](DEVELOPMENT_PLAN.md) 和实际提交为准。
