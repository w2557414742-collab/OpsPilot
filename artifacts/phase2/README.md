# Phase 2 运行证据

- 源码提交 86c401f7a8ab0a2dadc7483a94d99e05b544c208。
- 环境同 Phase 1。Spring Boot 3.5.16 BOM 管理 Flyway 11.7.2、Connector/J 9.7.0、Spring Data 2025.0.13 与 Lettuce 6.6.0.RELEASE。
- MySQL 使用 8.4.8，Redis 使用 7.4.8 Alpine。摘要与端口见 compose.yaml 和 Compose 运行日志。
- 验收命令为 python scripts/verify_phase2.py --java <本机 JDK 21 的 java.exe> --redis-port 16379 --output artifacts/phase2。使用本机 13316 的 MySQL 与 16379 的 Redis。
- Phase 1 回归命令为 python scripts/verify_phase1.py --java <本机 JDK 21 的 java.exe> --output .local/phase1-regression。
- Compose 记录使用 opspilot-compose-check 项目、alert-cache Profile、IPAM 覆盖文件、本机 MySQL 23316 与 Redis 26379。Docker 默认地址池耗尽的首次启动结果一并留存。
- 从空库验证使用同一代码与该 Compose 项目。该次调用脚本带 --redis-port 26379 --output .local/phase2-fresh-schema。
- 报告、用途与复现入口见 [功能测试报告](../../TEST_REPORT.md)。
