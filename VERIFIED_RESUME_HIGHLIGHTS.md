# 可验证的项目经历

- 使用 Java 21、Spring Boot、MyBatis 与 MySQL 构建多租户 AIOps 工单后端。以复合资源键、版本条件更新和事务流水实现租户隔离及并发状态控制。[Phase 1 验收](artifacts/phase1/end-to-end.log)
- 实现 Java 与 Python 分离存储的异步诊断。通过持久化幂等键、租约令牌和结果版本检查，验证了 HTTP 响应丢失、服务重启、迟到结果与取消确认。[接口快照](artifacts/phase1/snapshot.json)
- 实现 Alertmanager 告警归一化、原始证据归档、事件时间窗口与风暴抑制。使用 Redis Lua 投影窗口并验证 Redis 不可用时 MySQL 写入及聚合继续工作。[Phase 2 验收](artifacts/phase2/end-to-end.log)

面试展示使用确定性测试模式和实际验收日志。吞吐量、RCA 准确率和真实模型成本尚未测量。
