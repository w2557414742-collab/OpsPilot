# Phase 1 运行证据

- 源码提交 b82dd5605b7899b9003c118c4e4f54c7e8614677。
- 环境 Windows amd64，Microsoft OpenJDK 21.0.9+10，Maven 3.9.4，Python 3.13.5。
- MySQL 使用官方 8.4.8 镜像摘要 sha256:2952e3be7807f06fc18de50b3ea1a632d5c70d63482ff7d7376fe3aa8999babf，数据库通过 127.0.0.1:13316 访问。使用数据库队列。
- Python 运行 DETERMINISTIC_TEST 模式，凭证和测试租户由脚本创建。
- 构建命令为 mvn -B -ntp -f backend-java/pom.xml clean package。执行环境使用工作区 Maven 缓存与仅 Maven Central 的 settings。
- Python 命令为 python -m pytest agent-python --basetemp=.local/pytest。
- 端到端命令为 python scripts/verify_phase1.py --java <本机 JDK 21 的 java.exe> --output artifacts/phase1。
- java.log、java-restart.log、python.log 和 python-restart.log 记录子进程。snapshot.json 保存验收时返回的工单、任务和事件数据。
