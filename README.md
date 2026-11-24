# OpsPilot — Evidence-Guided AIOps & Incident Response Platform

基于多源可观测性数据与 AI Agent 的智能故障根因分析及应急响应平台。本里程碑交付 Java 业务状态与 Python 诊断运行态之间的最小闭环。

创建服务资产、创建工单、分诊、提交诊断、查看任务和增量流水。Java 把用户、工单、任务、审计与结果保存在 MySQL。Python 使用独立的 SQLite，通过稳定 run ID 提供幂等的异步 HTTP 接口。默认设计模式 DETERMINISTIC_TEST 生成调查建议，报告明确指出当前证据不足。

- [Windows 启动与复现](WINDOWS_QUICKSTART.md)
- [当前交付状态](PROJECT_STATUS.md)
- [HTTP 契约](API_CONTRACT.md)
- [架构](ARCHITECTURE.md)
- [状态机](INCIDENT_STATE_MACHINE.md)
- [任务引擎](AGENT_TASK_ENGINE.md)
- [参考项目与版本](docs/research/related_work.md)

当前 Compose 启动固定版本的 MySQL。Java 和 Python 使用独立的本地进程启动，诊断任务采用数据库队列。
