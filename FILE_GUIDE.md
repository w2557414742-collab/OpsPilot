# 源码阅读入口

| 文件 | 阅读重点 |
| --- | --- |
| [IncidentStatus.java](backend-java/ops-domain/src/main/java/dev/opspilot/domain/IncidentStatus.java) | 工单状态边与业务前置条件 |
| [Auth.java](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java) | 登录、JWT、账号角色与权限过滤链 |
| [OperationsApi.java](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java) | 租户公共接口及参数约束 |
| [OpsStore.java](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java) | 复合主键、行锁、工单版本与租约 SQL |
| [IncidentService.java](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java) | 业务事务与结果的版本约束 |
| [TaskWorker.java](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java) | 调度、HTTP 提交与结算 |
| [AgentClient.java](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/AgentClient.java) | Java 到 Python 的内部 HTTP 接口 |
| [AlertIngestion.java](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java) | 告警回执事务、事件水位、淘汰与候选冷却 |
| [AlertCache.java](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertCache.java) | Redis Lua 执行与缓存降级 |
| [api.py](agent-python/src/opspilot_agent/api.py) | FastAPI 入口、生命周期、取消与执行循环 |
| [store.py](agent-python/src/opspilot_agent/store.py) | 幂等 run、流水及 SQLite 运行租约 |
| [providers.py](agent-python/src/opspilot_agent/providers.py) | 确定性测试与外部模型适配 |
| [verify_phase1.py](scripts/verify_phase1.py) | 跨语言丢响应、租户隔离和重启验收 |
| [verify_phase2.py](scripts/verify_phase2.py) | 告警并发、乱序、降级和归档验收 |

按 [Java 方法](METHOD_REFERENCE.md) 和 [Python 函数](FUNCTION_REFERENCE.md) 跳转具体源码，辅以 [调用图](CALL_FLOW.md) 理解运行时边界。
