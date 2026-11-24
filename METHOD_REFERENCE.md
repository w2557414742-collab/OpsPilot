# Java 核心方法索引

| 方法 | 输入与输出 | 并发与错误 |
| --- | --- | --- |
| Auth.login | 登录负载，返回短效 Bearer token | BCrypt 校验，凭证错误返回 401 |
| IncidentService.transition | tenant、actor、工单 UUID、预期版本和目标，返回工单 | 工单行锁和 SQL 比较交换。过期版本返回 409 |
| IncidentService.requestDiagnosis | 上述租户、认证用户、工单和版本，以及幂等键，返回 Task | 同一工单串行化，事务内写工单、任务和事件 |
| IncidentService.cancel | tenant、actor 和 task UUID，返回 Task | 先锁工单再锁任务。取消使现任租约失效 |
| TaskWorker.tick | 无外部参数，推进一项数据库队列任务 | 到期租约可重领。HTTP 失败延迟复查 |
| TaskWorker.finish | 当前租约任务、状态及结果，更新事务 | 用租约和工单版本屏蔽已失租或旧运行的写入 |
