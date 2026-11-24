# Phase 1 数据与事务

| 库 | 表 | 主键与约束 |
| --- | --- | --- |
| MySQL | tenant | 租户 ID |
| MySQL | account | tenant_id 与 username，BCrypt 摘要 |
| MySQL | service_asset | tenant_id 与 id |
| MySQL | incident | tenant_id 与 id，复合外键引用同租户资产，version 比较交换 |
| MySQL | diagnosis_task | tenant_id 与 id，tenant_id、incident_id、idempotency_key 唯一 |
| MySQL | audit_event | 递增 ID，按租户、资源类型、资源 ID 与游标读取 |
| SQLite | run | tenant 与 id，保存请求 SHA-256、提供者和租约 |
| SQLite | run_event | 递增 ID，复合外键引用 run |

Flyway 执行 V1__core.sql。Java 工单写入、状态转换、诊断创建及终态更新与对应审计同事务提交。跨表事务先取得工单行锁再修改任务行。Python run 状态与事件使用同一 SQLite 写事务。

Java 的时钟判断使用数据库当前时间，Python 租约使用本机 Epoch 秒。
