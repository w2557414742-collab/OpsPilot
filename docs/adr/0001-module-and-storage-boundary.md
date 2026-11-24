# ADR 0001 模块化单体与独立诊断运行态

日期为 2026-11-24。状态为已采纳。

Java 由两个 Maven 模块组成。ops-domain 定义独立状态规则，ops-bootstrap 提供 Spring MVC、Spring Security、MyBatis 与调度入口。按业务包组织账号、工单、任务和存储，等模块拥有独立生命周期或明确依赖边界再拆 Maven 模块。

MySQL 保存用户、资产、工单、审计和任务结果。Python 单独使用 SQLite WAL 保存 run、幂等摘要、租约和事件。HTTP 路径使用 Java 任务 UUID 作为稳定 run ID。Java 的 202 确认任务入库，Python 的 202 确认 run 入库。

Phase 1 使用数据库队列，任务入库与工单状态切换共用事务。RocketMQ 阶段会以 Outbox/Inbox 验证可重放分发。SQLite 单文件适用于单节点运行，跨节点共用存储前需重新设计运行态存储和租约。
