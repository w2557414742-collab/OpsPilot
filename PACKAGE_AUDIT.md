# OpsPilot 仓库体积审计

基线为 `main` 的 `44f5fb2ece09b45df8b0842ce94740675812055c`，开始检查时工作区干净。统计逻辑文件字节，包含 .git、忽略目录和本机运行产物。文件系统快照在发布整理前采集，构建或安装工具后本机体积会变化。

- 工作区合计 **1,246,975,160 B (1,189.21 MiB)**，共 5,403 个普通文件。根目录直属文件共 27,676 B (0.03 MiB)。
- 基线 Git 跟踪 **89 个文件，221,919 B (0.21 MiB)**。从 HEAD 的 blob 统计，换行符可能与 Windows 工作区不同。
- Git 历史含 **3 个提交**。本地对象库中审计了 **105 个 blob**，覆盖全部本地对象，并从本地 refs 和 reflog 解析路径。
- 对象库中大于等于 50 MiB 的 blob **0 个**，大于等于 100 MiB 的 blob **0 个**。
- 遍历或读取元数据出现 0 项错误。统计不跟随联接与符号链接。

## 一级目录

| Relative path | Logical size |
| --- | ---: |
| `.local` | 1,196,021,166 B (1,140.61 MiB) |
| `backend-java` | 50,554,482 B (48.21 MiB) |
| `.git` | 158,012 B (0.15 MiB) |
| `agent-python` | 82,387 B (0.08 MiB) |
| `artifacts` | 72,005 B (0.07 MiB) |
| `scripts` | 47,373 B (0.05 MiB) |
| `docs` | 11,912 B (0.01 MiB) |
| `infra` | 147 B (0.00 MiB) |

## 磁盘上最大的 30 个文件

| Relative path | Logical size |
| --- | ---: |
| `.local/mysql.zip` | 260,192,668 B (248.14 MiB) |
| `.local/jdk21.zip` | 200,938,696 B (191.63 MiB) |
| `.local/jdk-21.0.9+10/lib/modules` | 140,814,338 B (134.29 MiB) |
| `.local/mysql-8.4.8-winx64/bin/mysqld.exe` | 53,796,480 B (51.30 MiB) |
| `.local/jdk-21.0.9+10/lib/src.zip` | 52,964,633 B (50.51 MiB) |
| `.local/jdk21-oracle.zip` | 51,380,224 B (49.00 MiB) |
| `backend-java/ops-bootstrap/target/ops-bootstrap-0.1.0-SNAPSHOT.jar` | 50,175,480 B (47.85 MiB) |
| `.local/release-tools/gh.exe` | 41,905,976 B (39.96 MiB) |
| `.local/release-tools/gitleaks.exe` | 22,575,104 B (21.53 MiB) |
| `.local/jdk-21.0.9+10/jmods/java.base.jmod` | 22,084,042 B (21.06 MiB) |
| `.local/m2/org/testcontainers/testcontainers/1.21.4/testcontainers-1.21.4.jar` | 17,790,267 B (16.97 MiB) |
| `.local/jdk-21.0.9+10/bin/server/jvm.dll` | 13,966,368 B (13.32 MiB) |
| `.local/mysql-8.4.8-winx64/bin/libprotobuf-debug.dll` | 13,840,528 B (13.20 MiB) |
| `.local/jdk-21.0.9+10/bin/server/classes.jsa` | 13,434,880 B (12.81 MiB) |
| `.local/jdk-21.0.9+10/bin/server/classes_nocoops.jsa` | 13,434,880 B (12.81 MiB) |
| `.local/jdk-21.0.9+10/jmods/java.desktop.jmod` | 12,449,990 B (11.87 MiB) |
| `.local/jdk-21.0.9+10/jmods/jdk.localedata.jmod` | 11,949,326 B (11.40 MiB) |
| `.local/jdk-21.0.9+10/jmods/jdk.compiler.jmod` | 10,883,659 B (10.38 MiB) |
| `.local/jdk-21.0.9+10/lib/ct.sym` | 10,715,138 B (10.22 MiB) |
| `.local/m2/net/bytebuddy/byte-buddy/1.17.8/byte-buddy-1.17.8.jar` | 9,018,553 B (8.60 MiB) |
| `.local/mysql-8.4.8-winx64/bin/mysql.exe` | 7,177,336 B (6.84 MiB) |
| `.local/m2/com/github/luben/zstd-jni/1.5.5-11/zstd-jni-1.5.5-11.jar` | 6,776,400 B (6.46 MiB) |
| `.local/venv/Lib/site-packages/pydantic_core/_pydantic_core.cp313-win_amd64.pyd` | 5,437,440 B (5.19 MiB) |
| `.local/jdk-21.0.9+10/jmods/java.xml.jmod` | 5,228,449 B (4.99 MiB) |
| `.local/mysql-8.4.8-winx64/bin/libcrypto-3-x64.dll` | 5,187,216 B (4.95 MiB) |
| `.local/mysql-8.4.8-winx64/bin/abseil_dll-debug.dll` | 4,503,696 B (4.30 MiB) |
| `.local/mysql-8.4.8-winx64/bin/libprotobuf.dll` | 4,205,192 B (4.01 MiB) |
| `.local/m2/org/apache/tomcat/embed/tomcat-embed-core/10.1.55/tomcat-embed-core-10.1.55.jar` | 3,673,934 B (3.50 MiB) |
| `.local/m2/org/apache/maven/surefire/surefire-shared-utils/3.5.6/surefire-shared-utils-3.5.6.jar` | 2,996,047 B (2.86 MiB) |
| `.local/mysql-8.4.8-winx64/bin/libprotobuf-lite-debug.dll` | 2,778,272 B (2.65 MiB) |

## 磁盘上最大的 20 个子目录

| Relative path | Logical size |
| --- | ---: |
| `.local` | 1,196,021,166 B (1,140.61 MiB) |
| `.local/jdk-21.0.9+10` | 342,605,872 B (326.73 MiB) |
| `.local/jdk-21.0.9+10/lib` | 206,522,835 B (196.96 MiB) |
| `.local/m2` | 122,389,938 B (116.72 MiB) |
| `.local/mysql-8.4.8-winx64` | 110,792,464 B (105.66 MiB) |
| `.local/mysql-8.4.8-winx64/bin` | 99,570,032 B (94.96 MiB) |
| `.local/jdk-21.0.9+10/jmods` | 83,412,140 B (79.55 MiB) |
| `.local/m2/org` | 76,473,134 B (72.93 MiB) |
| `.local/release-tools` | 64,481,080 B (61.49 MiB) |
| `.local/jdk-21.0.9+10/bin` | 51,984,096 B (49.58 MiB) |
| `backend-java` | 50,554,482 B (48.21 MiB) |
| `backend-java/ops-bootstrap` | 50,529,815 B (48.19 MiB) |
| `backend-java/ops-bootstrap/target` | 50,446,500 B (48.11 MiB) |
| `.local/venv` | 42,981,894 B (40.99 MiB) |
| `.local/venv/Lib` | 41,380,864 B (39.46 MiB) |
| `.local/venv/Lib/site-packages` | 41,380,864 B (39.46 MiB) |
| `.local/jdk-21.0.9+10/bin/server` | 40,836,128 B (38.94 MiB) |
| `.local/m2/org/springframework` | 25,935,198 B (24.73 MiB) |
| `.local/m2/org/apache` | 18,722,194 B (17.85 MiB) |
| `.local/m2/org/testcontainers` | 17,892,681 B (17.06 MiB) |

## Git 对象库中最大的 10 个 blob

| Relative path | Logical size |
| --- | ---: |
| `scripts/verify_phase1.py` | 14,769 B (0.01 MiB) |
| `backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java` | 9,868 B (0.01 MiB) |
| `scripts/verify_phase2.py` | 9,282 B (0.01 MiB) |
| `agent-python/src/opspilot_agent/store.py` | 8,072 B (0.01 MiB) |
| `backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java` | 7,195 B (0.01 MiB) |
| `backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java` | 7,049 B (0.01 MiB) |
| `backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java` | 6,657 B (0.01 MiB) |
| `backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java` | 6,525 B (0.01 MiB) |
| `backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java` | 6,412 B (0.01 MiB) |
| `backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java` | 6,057 B (0.01 MiB) |

## 内含的依赖和产物

工作区的主要体积来自 .local 下的 JDK、MySQL 下载、Maven 缓存和 Python 虚拟环境。Maven target、Python 缓存和临时 SQLite 保留在本机。跟踪的 artifacts 只包含两轮测试的日志、接口快照和复现说明。

Git 已跟踪内容与历史中未发现需要移出索引的大型依赖。发布阶段保留既有提交和本机文件。

## 复核命令

- 磁盘文件与目录使用普通文件的逻辑字节数，自底向上汇总。最大子目录可包含其他上榜子目录。
- git ls-tree -r -l HEAD 核对当前提交。git rev-list --objects --all --reflog 获取历史路径。
- git cat-file --batch-all-objects --batch-check 核对对象库中的全部对象大小。
