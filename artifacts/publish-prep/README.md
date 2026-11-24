# GitHub 发布准备复测

日期为 2026-11-24。本地基线为 `44f5fb2ece09b45df8b0842ce94740675812055c`。本轮仅增补发布、维护和运行入口，Java 与 Python 业务源文件沿用该基线。

- Java 21 和 Maven 3.9.4 执行 `mvn -o -B -ntp -f backend-java/pom.xml verify`，使用工作区已有依赖缓存。
- Python 3.13.5 执行 `python -m pytest agent-python --basetemp=.local/release-pytest`。
- 上一轮功能验收的日志与代码范围见 [Phase 1](../phase1/README.md) 和 [Phase 2](../phase2/README.md)。
- `java-tests.log` 与 `python-tests.log` 为原始输出。`preflight.log` 记录 Git、发布密钥与文档检查。Windows 启动在回环端口使用真实 MySQL 验证，原生 Linux 运行尚未执行。
- 本轮新增 GitHub Actions，远程运行状态为 NOT_RUN。
