# Phase 1 与 Phase 2 数据与事务

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
| MySQL | alert_source | tenant_id 与来源 ID |
| MySQL | alert_group | tenant_id 与稳定 group ID，外键约束资产归属 |
| MySQL | alert_receipt | tenant_id 与回执 UUID，保存原始字节 |
| MySQL | alert_event | tenant_id 与事件 UUID，来源及事件键唯一 |
| MySQL | alert_receipt_event | tenant_id、回执 UUID 和批内位置，关联回执和去重后的事件 |
| MySQL | incident_candidate | tenant_id 与候选 UUID，复合外键关联分组和触发事件 |

Flyway 执行 V1__core.sql。Java 工单写入、状态转换、诊断创建及终态更新与对应审计同事务提交。跨表事务先取得工单行锁再修改任务行。Python run 状态与事件使用同一 SQLite 写事务。

Java 的时钟判断使用数据库当前时间，Python 租约使用本机 Epoch 秒。

Flyway 的 V2 追加告警表。告警事务按分组固定顺序取锁，在当前已提交读隔离级别滚动计数。接收回执、证据、事件和候选一起提交。Redis 投影在事务提交后尝试写入。
