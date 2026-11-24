# 告警接入、聚合与复现

支持单条标准事件和 Alertmanager v4 Webhook。部署侧设置 OPSPILOT_ALERT_TENANT、OPSPILOT_ALERT_SOURCE 和 OPSPILOT_ALERT_TOKEN。令牌至少 32 字符，源租户、来源 ID 和令牌配置共同授权写入。Alertmanager 可通过 http_config.authorization.credentials_file 发送 Bearer 令牌。[Alertmanager 配置](https://prometheus.io/docs/alerting/latest/configuration/#http_config)

| HTTP 入口 | 内容 |
| --- | --- |
| POST /ingest/v1/{tenant}/{source}/events | eventId、serviceId、name、occurredAt、severity、status、labels |
| POST /ingest/v1/{tenant}/{source}/alertmanager | v4 格式。告警标签包含 service 和 alertname。severity 缺省 WARNING |
| GET /api/v1/alerts/groups | 当前租户最多 100 个告警分组 |
| GET /api/v1/alerts/candidates | 当前租户最多 100 个分诊候选 |
| GET /api/v1/alerts/receipts/{id} | 每个原始通知对应的新事件或重复事件信息 |
| GET /api/v1/alerts/receipts/{id}/raw | 同租户管理员下载原始字节 |

标准事件等级为 INFO、WARNING、CRITICAL，状态为 FIRING 或 RESOLVED。接收体上限 128 KiB，Webhook 最多 100 项。发生时间使用 RFC3339。服务资产需预先存在。幂等冲突返回 409，整批回滚。Alertmanager FIRING 使用 startsAt，RESOLVED 使用 endsAt，重复通知中变化的 endsAt 不影响 FIRING 幂等。

## 业务不变量

- MySQL 在同一事务内提交原始回执、复用或新建的事件、逐项处理记录、分组及候选。重放的回执可单独查询，重复事件不重复计数。
- 指纹用于标识告警实体，使用服务、告警名和排序标签。分组以租户、服务和告警名确定。写入通过复合外键限于当前来源所属租户。
- 以最大已观察事件时间为 watermark，滚动统计窗口为 (watermark - 60s, watermark]。窗口推进时仅查询离开窗口的计数区间。超过水位窗口或距接收时刻超过 300 秒的事件只留证据。超过未来 30 秒的输入返回 400。RESOLVED 通知可以推进水位，本窗口计数表示收到的 FIRING 通知量。
- 窗口达到五条或新收到 CRITICAL 时创建候选，同组 120 秒事件时间冷却期间计入抑制指标。告警候选可供人工分诊，创建工单依旧由 Java 工单入口负责。
- Redis Lua 原子比较水位、将事件 UUID 插入有序集合、清理过期成员并返回计数。该投影在 MySQL 提交后执行，单次操作受连接与命令超时限制。缓存故障可从 Micrometer 计数观察。Redis 丢失或进程在提交后、投影前退出会使缓存低估计数。MySQL 持久化聚合决定候选生成。 [Redis EVAL](https://redis.io/docs/latest/commands/eval/)

## 本机测试

按 Windows 指南构建 Java 与准备 Python 环境。设置数据库变量后，执行以下命令。测试只访问专用回环 Redis，无生产共享实例。

```powershell
docker compose -p opspilot-phase2 --profile alert-cache up -d --wait mysql redis
& .local/venv/Scripts/python.exe scripts/verify_phase2.py --output .local/phase2-artifacts
docker compose -p opspilot-phase2 --profile alert-cache stop mysql redis
```

验收包括并发请求、原始证据字节比较、窗口驱逐、Redis 可用及不可用读取、跨租户访问与重启恢复。最终缓存故障场景将 Java 指向本机未监听端口。
