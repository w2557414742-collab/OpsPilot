# Windows Phase 1 快速复现

使用 Windows PowerShell 7 或 PowerShell 5.1 发出下列命令。需要 JDK 21、Maven 3.9、Python 3.13 和运行中的 Docker Desktop Linux 引擎。用 mvn -version 确认 Maven 已读取 Java 21 的 JAVA_HOME。

本次验收在工作区使用 Microsoft OpenJDK 21.0.9+10。若沿用该本地安装，在当前终端执行 `$env:JAVA_HOME = (Resolve-Path '.local/jdk-21.0.9+10').Path` 和 `$env:Path = "$env:JAVA_HOME/bin;$env:Path"`。

## 构建与测试

```powershell
python -m venv .local/venv
& .local/venv/Scripts/python.exe -m pip install -r agent-python/requirements.lock
mvn -B -f backend-java/pom.xml clean package
& .local/venv/Scripts/python.exe -m pytest agent-python --basetemp=.local/pytest
```

## 启动本机数据库并验收

使用单独的 Compose 项目名创建验证库。为首次启动设置本地实验密码。已有数据库卷的密码由其初始化状态决定。

```powershell
$env:OPSPILOT_DB_PASSWORD = [guid]::NewGuid().ToString('N')
$env:OPSPILOT_DB_USER = 'opspilot'
$env:OPSPILOT_DB_PORT = '13316'
$env:OPSPILOT_DB_URL = "jdbc:mysql://127.0.0.1:$($env:OPSPILOT_DB_PORT)/opspilot"
docker compose -p opspilot-phase1 up -d --wait mysql
& .local/venv/Scripts/python.exe scripts/verify_phase1.py --output .local/phase1-artifacts
docker compose -p opspilot-phase1 stop mysql
```

脚本启动并回收自己的 Java 和 Python 进程，使用实际 MySQL 事务和 SQLite 文件。每次验证创建两个唯一租户。输出目录保留 Java 和 Uvicorn 原始日志及接口快照。复用同一项目的 MySQL 卷时使用先前的密码及原有端口。

若 Docker 报告默认地址池耗尽，可选用一个与本机路由及 Docker 网络均无重叠的私有网段。设置 OPSPILOT_COMPOSE_SUBNET，并在 Compose 子命令前传入配置参数 `-f compose.yaml -f infra/compose.ipam.yaml`。

## 手动启动服务

数据库可用后配置以下变量，在同一个 PowerShell 中启动 Java 和 Python。配置变量与默认值见 [.env.example](.env.example)。

```powershell
$env:OPSPILOT_JWT_KEY = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$env:OPSPILOT_AGENT_TOKEN = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$env:OPSPILOT_BOOTSTRAP_TENANT = 'lab'
$env:OPSPILOT_BOOTSTRAP_USER = 'admin'
$env:OPSPILOT_BOOTSTRAP_PASSWORD = [guid]::NewGuid().ToString('N')
$env:OPSPILOT_AGENT_DB = '.local/agent.sqlite3'
$env:OPSPILOT_PROVIDER = 'DETERMINISTIC_TEST'
& ./scripts/start-local.ps1
```

首次启动会插入管理员。重新启动时，已有账号保留原密码。公共 API 的路径及表单见 [HTTP 契约](API_CONTRACT.md)。

脚本让 Java 前台运行，Python 使用受脚本管理的后台作业。按 Ctrl+C 退出应用，再使用启动时的 Compose 参数停止数据库。Linux 用法见 [Linux 指南](docs/linux-quickstart.md)。
