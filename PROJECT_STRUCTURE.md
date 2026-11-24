# 项目结构

完整 Git 文件树见 [PROJECT_TREE.txt](PROJECT_TREE.txt)。

| 目录 | 内容 |
| --- | --- |
| `.github/workflows/` | GitHub Actions 的构建、单测、端到端验收与工件配置 |
| `backend-java/ops-domain/src/main/` | Java 工单状态机和任务状态 |
| `backend-java/ops-domain/src/test/` | JUnit 状态矩阵测试 |
| `backend-java/ops-bootstrap/src/main/java/` | 装配、账号 JWT、工单、告警、持久化及跨语言任务 |
| `backend-java/ops-bootstrap/src/main/resources/` | Spring 配置及 Flyway SQL V1、V2 |
| `agent-python/src/opspilot_agent/` | FastAPI、Pydantic 契约、SQLite、provider 和运行循环 |
| `agent-python/tests/` | SQLite 恢复、并发、安全、取消和租约测试 |
| `infra/` | Docker 默认地址池耗尽时的可选 Compose IPAM |
| `scripts/` | Windows/Linux 启动脚本及真实 MySQL、Redis、Java、Python 集成验收 |
| `docs/adr/` | 拆分、存储、取消及事件窗口的设计决定 |
| `docs/learning/java/`、`docs/learning/agent/`、`docs/learning/integration/` | 中文源码练习 |
| `docs/research/` | 上游源码研究和兼容版本记录 |
| `artifacts/phase1/`、`artifacts/phase2/` | 本机环境、测试日志、接口快照及可复现说明 |
| `artifacts/publish-prep/` | 本轮发布候选的本地构建、单测及预检证据 |
| `.local/` | 忽略的本机工具、缓存和数据库，作为工作目录使用 |

根目录存放启动说明、接口与数据库契约、项目状态、发布审计、源码索引与求职资料。项目使用 Maven POM、Python 依赖锁以及 Compose 镜像摘要管理依赖。Vue 前端、RocketMQ、Benchmark 与 RCA 算法目录将在对应功能落地后加入。
