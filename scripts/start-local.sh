#!/usr/bin/env bash
set -euo pipefail

repo=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd -- "$repo"
python=${OPSPILOT_PYTHON:-"$repo/.local/venv/bin/python"}
java=java
if [[ -n ${JAVA_HOME:-} ]]; then
    java="$JAVA_HOME/bin/java"
fi
jar="$repo/backend-java/ops-bootstrap/target/ops-bootstrap-0.1.0-SNAPSHOT.jar"
port=${OPSPILOT_AGENT_PORT:-8000}
: "${OPSPILOT_DB_URL:?Set OPSPILOT_DB_URL}"
: "${OPSPILOT_DB_USER:?Set OPSPILOT_DB_USER}"
: "${OPSPILOT_DB_PASSWORD:?Set OPSPILOT_DB_PASSWORD}"
: "${OPSPILOT_JWT_KEY:?Set OPSPILOT_JWT_KEY}"
: "${OPSPILOT_AGENT_TOKEN:?Set OPSPILOT_AGENT_TOKEN}"
[[ -f $jar ]] || { printf '%s\n' 'Build the Java jar first.' >&2; exit 1; }
command -v "$python" "$java" curl >/dev/null
"$python" -c 'import socket, sys; socket.create_server(("127.0.0.1", int(sys.argv[1]))).close()' "$port"

export SERVER_ADDRESS=127.0.0.1
export OPSPILOT_AGENT_URL="http://127.0.0.1:$port"
export OPSPILOT_PROVIDER=DETERMINISTIC_TEST
export OPSPILOT_AGENT_DB=${OPSPILOT_AGENT_DB:-"$repo/.local/agent.sqlite3"}
export PYTHONPATH="$repo/agent-python/src"
"$python" -m uvicorn opspilot_agent.api:from_env --factory --host 127.0.0.1 --port "$port" &
agent_pid=$!
cleanup() {
    kill "$agent_pid" 2>/dev/null || true
    wait "$agent_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

for ((attempt=0; attempt<40; attempt++)); do
    kill -0 "$agent_pid" 2>/dev/null || break
    if curl --noproxy '*' --fail --silent --max-time 1 "$OPSPILOT_AGENT_URL/health" >/dev/null; then
        "$java" -jar "$jar"
        exit $?
    fi
    sleep 0.25
done
printf '%s\n' 'The local Python Agent did not become healthy.' >&2
exit 1
