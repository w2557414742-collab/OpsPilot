# 当前仓库结构

| 路径 | 真实职责 |
| --- | --- |
| backend-java/ops-domain | 工单状态矩阵、任务状态、JUnit 状态矩阵测试 |
| backend-java/ops-bootstrap | Spring MVC、Spring Security、MyBatis、Flyway、工单事务及 Java worker |
| agent-python/src/opspilot_agent | Pydantic 契约、FastAPI、SQLite 事务、provider 及后台 worker |
| agent-python/tests | 并发提交、存储重启、取消乱序、租约回收测试 |
| scripts/verify_phase1.py | Java、Python 与真实 MySQL 的跨进程功能验收 |
| docs/adr | 模块、存储与取消决策 |
| docs/research | 上游研究与阶段依赖冻结 |
| artifacts/phase1 | Phase 1 测试日志和真实接口快照 |
