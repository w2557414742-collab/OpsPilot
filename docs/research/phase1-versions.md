# Phase 1 版本冻结

| 组件 | 冻结版本 | 依据 |
| --- | --- | --- |
| Java | Microsoft OpenJDK 21.0.9+10，Maven release 21 | Windows ZIP SHA-256 为 96cda27c4d2171b94125e2c7dedad3d22c8e02f99babfe3369ac52af06b4397a。[Microsoft 下载与校验](https://learn.microsoft.com/en-us/java/openjdk/older-releases) · [Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html) |
| Maven | 3.9.4 | 本机构建工具 |
| Spring Boot | 3.5.16 | 父 POM 固定版本并管理 Spring Security、Flyway、Connector/J、Micrometer、JUnit |
| MyBatis Starter | 3.0.5 | 该版本切换到 Spring Boot 3.5 基线。 [上游发布说明](https://github.com/mybatis/spring-boot-starter/releases/tag/mybatis-spring-boot-3.0.5) |
| MySQL | 8.4.8，Compose 固定镜像摘要 | 本机 Docker Desktop 的 Linux 容器 |
| Python | 3.13.5 | 项目声明兼容 3.11 至 3.13。本机以 3.13.5 执行 |
| FastAPI / Pydantic | 0.121.3 / 2.12.4 | [FastAPI 版本](https://pypi.org/project/fastapi/0.121.3/) · [Pydantic 版本](https://pypi.org/project/pydantic/2.12.4/) |
| httpx / Uvicorn / pytest | 0.28.1 / 0.38.0 / 8.4.2 | 直接和传递依赖见 agent-python/requirements.lock |
| Git / Node / npm | 2.51.0.windows.1 / 24.15.0 / 11.12.1 | 本机检查。Phase 1 未使用 Node 构建 |
| Docker / Compose | 28.3.2 / 2.38.2-desktop.1 | 本机 Docker Desktop Linux 引擎 |

Redis、RocketMQ、LangGraph、前端与可观测性依赖随进入相应阶段时冻结，记录实际验证的兼容组合。
