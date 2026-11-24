# 交付状态

更新于 2026-11-24。

| 阶段或能力 | 状态 | 证据或待验收项 |
| --- | --- | --- |
| Phase 0 环境、依赖冻结、上游源码与 ADR | VERIFIED | docs/research 与 docs/adr，Java 21 已独立安装并验证官方摘要 |
| Phase 1 Python 异步接口及持久化 | VERIFIED | 4 项 pytest，日志在 artifacts/phase1 |
| Phase 1 Java 账号、租户、资产、工单、任务与审计 | VERIFIED | Java 21、真实 MySQL 8.4.8 与 Python 回环验收。见 TEST_REPORT.md |
| Phase 1 外部模型兼容适配器 | NOT_RUN | 默认确定性适配器，未授权真实模型 |
| Phase 2 告警入库、窗口、风暴抑制与 Redis 投影 | VERIFIED | 真实 MySQL 8.4.8、Redis 7.4.8、并发与降级验收。见 TEST_REPORT.md |
| Phase 3 RocketMQ、Outbox/Inbox、预算管理 | NOT_IMPLEMENTED | 待启动 |
| Phase 4 遥测实验 | NOT_IMPLEMENTED | 待启动 |
| Phase 5 RCA 算法 | NOT_IMPLEMENTED | 待启动 |
| Phase 6 LangGraph/MCP 主动诊断 | NOT_IMPLEMENTED | 待启动 |
| Phase 7 RAG、审批、处置和恢复验证 | NOT_IMPLEMENTED | 待启动 |
| Phase 8 Benchmark、性能与故障注入 | NOT_RUN | 当前实验只验证 Phase 1 功能 |
| Phase 9 前端、完整 Compose profiles、SSE | NOT_IMPLEMENTED | 当前 Compose 启动 MySQL 与可选 Redis，均通过健康检查 |
| Phase 10 完整安全审计及求职交付 | NOT_IMPLEMENTED | 已归档 Phase 1 至 Phase 2 源码阅读、面试及阶段性安全核验 |

已验证的告警并发用例用于检查业务不变量。QPS、延迟分位数与长期容量均为 NOT_RUN。

## GitHub 发布

Phase 0–2 源码、历史和验收证据已发布至私有仓库 [w2557414742-collab/OpsPilot](https://github.com/w2557414742-collab/OpsPilot)。远程仓库的可见性、分支、文件与 CI 见 [发布报告](GITHUB_PUBLISH_REPORT.md)。
