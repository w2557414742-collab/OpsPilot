# Phase 1 与 Phase 2 演示

按 [Windows 指南](WINDOWS_QUICKSTART.md) 构建和启动真实 MySQL 后，运行 scripts/verify_phase1.py。演示从登录、创建服务与工单到异步诊断结果落库。审计和结果在 snapshot.json 中可查，丢失 HTTP 响应时可对照 Java task ID 与 Python run ID，以及 Python 事件流中的 RUNNING 次数。

按 [告警复现](docs/alerts.md) 启用 Redis 后，运行 scripts/verify_phase2.py。观察同一告警的 8 次提交如何映射为一条业务事件，并读取单次回执的原始 JSON。脚本推进事件时间水位，核对 MySQL 与 Redis 窗口计数，再重启 Java 模拟 Redis 无法连接。

这些演示使用测试工单与固定输入。完整测试日志见 [测试报告](TEST_REPORT.md)。
