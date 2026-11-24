# Phase 1 HTTP 契约

Java 公共接口使用 /api/v1。除登录外都使用 Bearer JWT，列表最多 100 行。

| 方法与路径 | 负载或参数 | 成功状态 |
| --- | --- | --- |
| POST /auth/token | tenant、username、password | 200 |
| POST /auth/users | username、password、role | 201 |
| GET /services | 当前租户 | 200 |
| POST /services | id、name | 201 |
| GET /incidents | 当前租户 | 200 |
| POST /incidents | serviceId、title、description | 201 |
| GET /incidents/{id} | 工单 UUID | 200 |
| POST /incidents/{id}/transitions | expectedVersion、target | 200 |
| POST /incidents/{id}/diagnoses | expectedVersion，Idempotency-Key 请求头 | 202 |
| GET /incidents/{id}/events | after 默认 0 | 200 |
| GET /tasks/{id} | task UUID | 200 |
| POST /tasks/{id}/cancel | task UUID | 202 |

管理员可创建用户，管理员和运维可写业务，观察者可读。无权限返回 403。租户内缺失和跨租户 ID 返回 404。非法状态转换、旧版本和幂等冲突返回 409。

Python 内部接口前缀为 /internal/v1/tenants/{tenant}/runs/{run_id}，使用部署配置的服务 Bearer 令牌。

| 方法 | 负载或参数 | 响应 |
| --- | --- | --- |
| PUT 根路径 | incidentId、serviceId、title、description、traceId | 202，runId、status、result、errorCode |
| GET 根路径 | run UUID | 200，同上 |
| PUT /cancel | run UUID | 202，当前状态 |
| GET /events | after 默认 0 | 200，id、status、at 列表 |

result 在 SUCCEEDED 时包含 provider、conclusion、evidence 和 nextChecks。Phase 1 的证据数组为空，结论为 INSUFFICIENT_EVIDENCE。
