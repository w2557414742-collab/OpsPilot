# Linux 本机运行与验收

需要 JDK 21、Maven 3.9、Python 3.13、curl 和 Docker Compose。在仓库顶层执行下列命令。

```bash
python3 -m venv .local/venv
.local/venv/bin/python -m pip install -r agent-python/requirements.lock
mvn -B -ntp -f backend-java/pom.xml package
export OPSPILOT_DB_USER=opspilot
export OPSPILOT_DB_URL=jdbc:mysql://127.0.0.1:13316/opspilot
export OPSPILOT_DB_PASSWORD="$(openssl rand -hex 24)"
docker compose --profile alert-cache up -d --wait mysql redis
```

密码生成步骤用于首次创建本地数据库卷。复用卷时请沿用原密码。

```bash
.local/venv/bin/python -m pytest agent-python
.local/venv/bin/python scripts/verify_phase1.py --output .local/phase1-evidence
.local/venv/bin/python scripts/verify_phase2.py --output .local/phase2-evidence
```

为本地运行配置环境变量，再启动两个服务。

```bash
export OPSPILOT_JWT_KEY="$(openssl rand -hex 32)"
export OPSPILOT_AGENT_TOKEN="$(openssl rand -hex 32)"
export OPSPILOT_BOOTSTRAP_TENANT=lab
export OPSPILOT_BOOTSTRAP_USER=admin
export OPSPILOT_BOOTSTRAP_PASSWORD="$(openssl rand -hex 18)"
bash scripts/start-local.sh
```

Java 默认监听 127.0.0.1:8080，Python 默认监听 127.0.0.1:8000。在另一个终端可访问各自的 `/actuator/health` 和 `/health`。按 Ctrl+C 退出应用，再执行 `docker compose --profile alert-cache stop mysql redis`。MySQL 卷保留用于下次运行。
