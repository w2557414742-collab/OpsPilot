# Phase 1 与 Phase 2 源码阅读入口

| 文件 | 阅读重点 |
| --- | --- |
| backend-java/ops-domain/src/main/java/dev/opspilot/domain/IncidentStatus.java | 状态边、业务前置条件 |
| backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java | 登录、JWT 与权限过滤链 |
| backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java | 复合主键、行锁、版本与租约 |
| backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java | 业务事务与审计 |
| backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java | 调度与 HTTP 失败恢复 |
| agent-python/src/opspilot_agent/api.py | 请求入口、生命周期和执行循环 |
| agent-python/src/opspilot_agent/store.py | 幂等 run、事件检索、取消和运行租约 |
| agent-python/src/opspilot_agent/providers.py | 确定性和外部 provider 的边界 |
| scripts/verify_phase1.py | 手动重复请求、注入丢响应、验证业务不变量 |
| backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java | 回执事务、事件时间窗口及候选冷却 |
| backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertCache.java | Lua 执行及提交后投影 |
