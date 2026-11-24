# 第三方来源与许可

OpsPilot 源码仓库通过 Maven 和 Python 清单声明依赖，通过 Docker Compose 引用上游镜像。依赖二进制、JDK 下载、镜像、模型与数据集保留在各自来源或本机缓存。

| 实际依赖或工具 | 许可证与核对来源 |
| --- | --- |
| Spring Boot | Apache-2.0。由 Maven 父 POM 管理 Spring Security、Spring Data 等版本。 [上游许可](https://github.com/spring-projects/spring-boot/blob/v3.5.16/LICENSE.txt) |
| MyBatis Spring Boot Starter | Apache-2.0。[上游发行版](https://github.com/mybatis/spring-boot-starter/releases/tag/mybatis-spring-boot-3.0.5) |
| MySQL Server、Connector/J | Docker 镜像及 Maven 发行件各依上游许可。Connector/J 的 FOSS 例外及版本条款由发行件提供。 [Server 许可](https://dev.mysql.com/doc/refman/8.4/en/license-information.html) · [Connector/J 9.7.0 许可](https://github.com/mysql/mysql-connector-j/blob/9.7.0/LICENSE) |
| Redis 7.4.8 | RSALv2 或 SSPLv1，以该版本的许可条款为准。 [固定版本许可证](https://github.com/redis/redis/blob/7.4.8/LICENSE.txt) |
| FastAPI、Pydantic | MIT。 [FastAPI 发行包](https://pypi.org/project/fastapi/0.121.3/) · [Pydantic 发行包](https://pypi.org/project/pydantic/2.12.4/) |
| httpx、Uvicorn | BSD-3-Clause。 [httpx 发行包](https://pypi.org/project/httpx/0.28.1/) · [Uvicorn 发行包](https://pypi.org/project/uvicorn/0.38.0/) |
| pytest | MIT。 [pytest 发行包](https://pypi.org/project/pytest/8.4.2/) |

完整版本范围以 [Maven POM](backend-java/pom.xml)、[Python 锁文件](agent-python/requirements.lock) 和 [Compose](compose.yaml) 为准。参考项目的版本、SHA 和许可证见 [相关工作](docs/research/related_work.md)。发布包含依赖的二进制或定制镜像前，应按实际发行件复核各依赖的许可与随附声明。
