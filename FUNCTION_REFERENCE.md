# Python 函数参考

索引由 Python AST 生成，覆盖自有 Agent、测试与集成验收脚本。类方法使用限定名称。后续增加的 LangGraph、MCP 与 RCA 算法将在实际实现后纳入。

行号由声明与函数体的源码范围生成。调用关系来自静态表达式。FastAPI 通过路由装饰器和 lifespan 间接触发的方法见 [调用流程](CALL_FLOW.md)。

## __init__.py

[`agent-python/src/opspilot_agent/__init__.py`](agent-python/src/opspilot_agent/__init__.py)

此模块定义数据契约或包说明，无显式函数。

## api.py

[`agent-python/src/opspilot_agent/api.py`](agent-python/src/opspilot_agent/api.py)

| 类与函数 | 签名 | 行号 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- |
| `work_once` | `def work_once(store: Store, provider: Provider) -> bool` | [23–41](agent-python/src/opspilot_agent/api.py#L23-L41) | 领取并执行一个 run。校验固定的 provider，区分失败与付费调用结果未知。 | 上游 test_cancel_before_delayed_submit, test_expired_lease_reclaims_only_deterministic, test_idempotent_http_and_restart · 下游 store.claim, store.finish, provider.diagnose, DiagnosisRequest.model_validate_json, LOG.warning, type | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app` | `def create_app(path: Path, token: str, provider: Provider, start_worker: bool=True) -> FastAPI` | [44–118](agent-python/src/opspilot_agent/api.py#L44-L118) | 构建独立 Agent 服务。测试可手动推进 worker 以断言状态边界。 | 上游 from_env, test_cancel_before_delayed_submit, test_idempotent_http_and_restart · 下游 Store, FastAPI, app.get, app.put, len, ValueError, asyncio.Event, Depends | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app.dispatch` | `async def dispatch(stop: asyncio.Event)` | [50–60](agent-python/src/opspilot_agent/api.py#L50-L60) | 在后台轮询数据库，SQLite 锁冲突时下轮继续。 | 上游 create_app, create_app.lifespan · 下游 stop.is_set, asyncio.to_thread, LOG.warning, asyncio.wait_for, stop.wait, type | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app.lifespan` | `async def lifespan(_app)` | [63–72](agent-python/src/opspilot_agent/api.py#L63-L72) | 启停调度循环，关闭服务前等待正在执行的步骤结束。 | 下游 asyncio.Event, asyncio.create_task, stop.set, dispatch | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app.authorize` | `def authorize(authorization: Annotated[str \| None, Header()]=None) -> None` | [74–77](agent-python/src/opspilot_agent/api.py#L74-L77) | 用常量时间比较校验 Java 专用服务令牌。 | 下游 hmac.compare_digest, HTTPException, (authorization or '').encode, f'Bearer {token}'.encode, Header | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app.health` | `def health()` | [85–87](agent-python/src/opspilot_agent/api.py#L85-L87) | 暴露进程健康与当前 provider 模式。 | 下游 app.get | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app.submit` | `def submit(tenant: Tenant, run_id: UUID, request: DiagnosisRequest)` | [90–95](agent-python/src/opspilot_agent/api.py#L90-L95) | 异步接受运行任务。重复请求返回已持久化状态。 | 下游 app.put, store.submit, str, HTTPException | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app.get` | `def get(tenant: Tenant, run_id: UUID)` | [98–103](agent-python/src/opspilot_agent/api.py#L98-L103) | 获取运行状态与结果。 | 下游 app.get, store.get, str, HTTPException | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app.cancel` | `def cancel(tenant: Tenant, run_id: UUID)` | [106–108](agent-python/src/opspilot_agent/api.py#L106-L108) | 提交幂等取消请求或读取终态。 | 下游 app.put, store.cancel, str | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `create_app.events` | `def events(tenant: Tenant, run_id: UUID, after: Annotated[int, Query(ge=0)]=0)` | [111–116](agent-python/src/opspilot_agent/api.py#L111-L116) | 获取增量运行状态流水。 | 下游 app.get, store.events, str, HTTPException, Query | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `from_env` | `def from_env() -> FastAPI` | [121–124](agent-python/src/opspilot_agent/api.py#L121-L124) | Uvicorn 工厂，从环境加载存储路径、内部凭证和 provider。 | 下游 create_app, Path, configured_provider, os.environ.get | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |

## models.py

[`agent-python/src/opspilot_agent/models.py`](agent-python/src/opspilot_agent/models.py)

此模块定义数据契约或包说明，无显式函数。

## providers.py

[`agent-python/src/opspilot_agent/providers.py`](agent-python/src/opspilot_agent/providers.py)

| 类与函数 | 签名 | 行号 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- |
| `Provider.diagnose` | `def diagnose(self, request: DiagnosisRequest) -> Report` | [18–19](agent-python/src/opspilot_agent/providers.py#L18-L19) | 从指定工单生成受契约约束的下一步调查建议。 | 见声明与实现。 | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `DeterministicProvider.diagnose` | `def diagnose(self, request: DiagnosisRequest) -> Report` | [27–32](agent-python/src/opspilot_agent/providers.py#L27-L32) | 给出指标、日志与 Trace 的待调查项，证据保持为空。 | 下游 Report | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `CompatibleProvider.__init__` | `def __init__(self, url: str, api_key: str, model: str)` | [47–52](agent-python/src/opspilot_agent/providers.py#L47-L52) | 仅允许部署端配置 HTTPS 端点和凭证。 | 下游 httpx.URL, ValueError | 真实模型 NOT_RUN |
| `CompatibleProvider.diagnose` | `def diagnose(self, request: DiagnosisRequest) -> Report` | [54–72](agent-python/src/opspilot_agent/providers.py#L54-L72) | 提交有界的工单上下文。调用失败交由任务层记录，禁止盲重放。 | 下游 httpx.post, response.raise_for_status, SuggestedChecks.model_validate_json, Report, response.json, json.dumps | 真实模型 NOT_RUN |
| `configured_provider` | `def configured_provider() -> Provider` | [75–83](agent-python/src/opspilot_agent/providers.py#L75-L83) | 默认使用本地模式。真实模型需要部署端显式选择与配置完整凭证。 | 上游 from_env · 下游 os.environ.get, ValueError, DeterministicProvider, CompatibleProvider | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |

## store.py

[`agent-python/src/opspilot_agent/store.py`](agent-python/src/opspilot_agent/store.py)

| 类与函数 | 签名 | 行号 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- |
| `Store.__init__` | `def __init__(self, path: Path)` | [20–39](agent-python/src/opspilot_agent/store.py#L20-L39) | 设置持久化路径，并初始化 WAL 和数据表。 | 下游 path.parent.mkdir, self._connection, conn.execute, conn.executescript | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store._connection` | `def _connection(self, write=False)` | [42–54](agent-python/src/opspilot_agent/store.py#L42-L54) | 写事务先取得保留锁，使读改写和事件提交不可分割。 | 上游 Store.__init__, Store.cancel, Store.claim, Store.events · 下游 sqlite3.connect, conn.execute, conn.close, conn.commit | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store._row` | `def _row(conn, tenant, run_id)` | [57–59](agent-python/src/opspilot_agent/store.py#L57-L59) | 用复合主键查询运行快照。 | 上游 Store.cancel, Store.events, Store.finish, Store.get · 下游 conn.execute | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store._event` | `def _event(conn, tenant, run_id, status)` | [62–65](agent-python/src/opspilot_agent/store.py#L62-L65) | 在调用方事务内记录状态变更。 | 上游 Store.cancel, Store.claim, Store.finish, Store.submit · 下游 conn.execute, time.time | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store._view` | `def _view(row)` | [68–71](agent-python/src/opspilot_agent/store.py#L68-L71) | 将持久化 JSON 验证为对外契约。 | 上游 Store.cancel, Store.get, Store.submit · 下游 Run, Report.model_validate_json | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store.submit` | `def submit(self, tenant: str, run_id: str, request: DiagnosisRequest, provider: ProviderName) -> Run` | [73–88](agent-python/src/opspilot_agent/store.py#L73-L88) | 以租户和稳定 run ID 去重，支持提交响应丢失后的相同请求重放。 | 下游 request.model_dump_json, hashlib.sha256(payload.encode()).hexdigest, self._connection, self._row, conn.execute, self._event, self._view, hashlib.sha256 | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store.get` | `def get(self, tenant: str, run_id: str) -> Run \| None` | [90–94](agent-python/src/opspilot_agent/store.py#L90-L94) | 读取租户内已知运行状态。 | 下游 self._connection, self._row, self._view | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store.cancel` | `def cancel(self, tenant: str, run_id: str) -> Run` | [96–108](agent-python/src/opspilot_agent/store.py#L96-L108) | 取消未启动任务或登记取消请求，支持取消先于延迟提交到达。 | 下游 self._connection, self._row, self._view, conn.execute, self._event, time.time | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store.events` | `def events(self, tenant: str, run_id: str, after: int) -> list[RunEvent] \| None` | [110–119](agent-python/src/opspilot_agent/store.py#L110-L119) | 以序号增量读取最多 100 条状态事件。 | 下游 self._connection, conn.execute, self._row, RunEvent, dict | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store.claim` | `def claim(self, lease_seconds: float=30)` | [121–146](agent-python/src/opspilot_agent/store.py#L121-L146) | 回收过期执行并领取下一项运行，租约内执行发生在 SQLite 事务外。 | 上游 test_expired_lease_reclaims_only_deterministic, test_running_cancel_requires_worker_ack, work_once · 下游 self._connection, time.time, str, conn.execute, self._event, uuid4 | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |
| `Store.finish` | `def finish(self, tenant, run_id, lease, report=None, error=None, uncertain=False) -> bool` | [148–165](agent-python/src/opspilot_agent/store.py#L148-L165) | 只有当前有效令牌可结算，远程执行结果不明时进入 UNKNOWN_OUTCOME。 | 上游 test_expired_lease_reclaims_only_deterministic, test_running_cancel_requires_worker_ack, work_once · 下游 self._connection, self._row, conn.execute, self._event, report.model_dump_json, time.time | [pytest](agent-python/tests/test_runs.py) · [诊断集成](scripts/verify_phase1.py) |

## test_runs.py

[`agent-python/tests/test_runs.py`](agent-python/tests/test_runs.py)

| 类与函数 | 签名 | 行号 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- |
| `test_idempotent_http_and_restart` | `def test_idempotent_http_and_restart(tmp_path)` | [24–46](agent-python/tests/test_runs.py#L24-L46) | 重复提交只执行一个 run，重建应用后保留完整流水。 | 下游 DeterministicProvider, work_once, TestClient, all, Store, client.get(url, headers=HEADERS).json, client.get(url + '/events', headers=HEADERS).json, uuid4 | 验收脚本或直接用例 |
| `test_cancel_before_delayed_submit` | `def test_cancel_before_delayed_submit(tmp_path)` | [49–57](agent-python/tests/test_runs.py#L49-L57) | 取消标记屏蔽迟到 PUT，不会创建第二次执行。 | 下游 DeterministicProvider, TestClient, uuid4, create_app, work_once, client.put(url + '/cancel', headers=HEADERS).json, client.put(url, json=SAMPLE, headers=HEADERS).json, Store | 验收脚本或直接用例 |
| `test_expired_lease_reclaims_only_deterministic` | `def test_expired_lease_reclaims_only_deterministic(tmp_path)` | [60–74](agent-python/tests/test_runs.py#L60-L74) | 重启可以恢复确定性 run。未知的模型执行进入对账状态。 | 下游 Store, DiagnosisRequest, DeterministicProvider, store.submit, store.claim, work_once, str, store.finish | 验收脚本或直接用例 |
| `test_running_cancel_requires_worker_ack` | `def test_running_cancel_requires_worker_ack(tmp_path)` | [77–87](agent-python/tests/test_runs.py#L77-L87) | 执行中取消先记录请求，工作完成时丢弃报告并确认。 | 下游 Store, DeterministicProvider, store.submit, store.claim, store.finish, DiagnosisRequest, str, uuid4 | 验收脚本或直接用例 |

## verify_phase1.py

[`scripts/verify_phase1.py`](scripts/verify_phase1.py)

| 类与函数 | 签名 | 行号 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- |
| `port` | `def port()` | [26–30](scripts/verify_phase1.py#L26-L30) | 分配一个空闲的回环测试端口。 | 上游 verify · 下游 socket.socket, sock.bind, sock.getsockname | 验收脚本或直接用例 |
| `wait_for` | `def wait_for(check, timeout=45)` | [33–44](scripts/verify_phase1.py#L33-L44) | 在有界等待内轮询真实进程状态。 | 上游 create_app, create_app.dispatch, login, verify · 下游 AssertionError, time.monotonic, time.sleep, check | 验收脚本或直接用例 |
| `stop_process` | `def stop_process(proc)` | [47–55](scripts/verify_phase1.py#L47-L55) | 仅停止本脚本启动的子进程。 | 上游 verify · 下游 proc.poll, proc.terminate, proc.wait, proc.kill | 验收脚本或直接用例 |
| `start` | `def start(stack, cmd, env, log)` | [58–63](scripts/verify_phase1.py#L58-L63) | 启动服务并保留进程输出，退出测试时回收该进程。 | 上游 verify · 下游 stack.enter_context, subprocess.Popen, stack.callback, log.open | 验收脚本或直接用例 |
| `AgentProxy.do_PUT` | `def do_PUT(self)` | [69–71](scripts/verify_phase1.py#L69-L71) | 转发或丢弃一次提交响应，以验证稳定 run ID 重放。 | 下游 self.forward | 验收脚本或直接用例 |
| `AgentProxy.do_GET` | `def do_GET(self)` | [73–75](scripts/verify_phase1.py#L73-L75) | 转发轮询请求。 | 下游 self.forward | 验收脚本或直接用例 |
| `AgentProxy.forward` | `def forward(self)` | [77–102](scripts/verify_phase1.py#L77-L102) | 固定上游，注入受控丢包与迟到结果。 | 上游 AgentProxy.do_GET, AgentProxy.do_PUT · 下游 self.reply, self.headers.get('Transfer-Encoding', '').lower, self.read_chunks, self.rfile.read, httpx.request, int, self.path.endswith, self.server.late_arrived.set | 验收脚本或直接用例 |
| `AgentProxy.reply` | `def reply(self, status, content)` | [104–112](scripts/verify_phase1.py#L104-L112) | 返回本机上游响应。被停止的测试客户端可能已关闭连接。 | 上游 AgentProxy.forward · 下游 self.send_response, self.send_header, self.end_headers, self.wfile.write | 验收脚本或直接用例 |
| `AgentProxy.read_chunks` | `def read_chunks(self)` | [114–122](scripts/verify_phase1.py#L114-L122) | 从 Java HTTP 客户端读取由分块传输发送的 JSON。 | 上游 AgentProxy.forward · 下游 b''.join, int, chunks.append, self.rfile.read, self.rfile.readline, self.rfile.readline(64).partition | 验收脚本或直接用例 |
| `AgentProxy.log_message` | `def log_message(self, *_args)` | [124–125](scripts/verify_phase1.py#L124-L125) | 避免将请求凭证记入测试日志。 | 见声明与实现。 | 验收脚本或直接用例 |
| `api` | `def api(client, method, route, expected=200, **kwargs)` | [128–132](scripts/verify_phase1.py#L128-L132) | 断言 HTTP 状态并返回 JSON 负载。 | 上游 create_incident, submit, terminal_task, verify · 下游 client.request, response.json | 验收脚本或直接用例 |
| `login` | `def login(base, tenant, username='admin', password=ADMIN_PASSWORD)` | [135–140](scripts/verify_phase1.py#L135-L140) | 向实际认证服务申请一枚租户访问令牌。 | 上游 verify · 下游 httpx.Client, wait_for, login_ready, response.json | 验收脚本或直接用例 |
| `login_ready` | `def login_ready(client, tenant, username, password)` | [142–145](scripts/verify_phase1.py#L142-L145) | 等待启动初始化写入管理员账号。 | 上游 login · 下游 client.post | 验收脚本或直接用例 |
| `create_incident` | `def create_incident(client, asset, title)` | [148–153](scripts/verify_phase1.py#L148-L153) | 创建关联服务的工单并进入 TRIAGING。 | 上游 verify · 下游 api | 验收脚本或直接用例 |
| `submit` | `def submit(client, incident, key)` | [156–159](scripts/verify_phase1.py#L156-L159) | 以当前版本与幂等键请求诊断。 | 下游 api | 验收脚本或直接用例 |
| `terminal_task` | `def terminal_task(client, task_id)` | [162–165](scripts/verify_phase1.py#L162-L165) | 读取真实任务终态。 | 上游 verify · 下游 api | 验收脚本或直接用例 |
| `verify` | `def verify(args, output)` | [168–281](scripts/verify_phase1.py#L168-L281) | 运行竞争写、跨租户、丢响应、迟到、取消与双进程重启场景。 | 下游 secrets.token_urlsafe, port, str, ExitStack, ThreadingHTTPServer, threading.Lock, threading.Thread, thread.start | 验收脚本或直接用例 |
| `main` | `def main()` | [284–295](scripts/verify_phase1.py#L284-L295) | 校验依赖并运行可重复的回环验收。 | 下游 argparse.ArgumentParser, parser.add_argument, parser.parse_args, args.output.resolve, output.mkdir, verify, parser.error, JAR.is_file | 验收脚本或直接用例 |

## verify_phase2.py

[`scripts/verify_phase2.py`](scripts/verify_phase2.py)

| 类与函数 | 签名 | 行号 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- |
| `iso` | `def iso(instant)` | [19–21](scripts/verify_phase2.py#L19-L21) | 让验收时间使用同一个 UTC 时间基准。 | 上游 verify, verify.event · 下游 instant.isoformat().replace, instant.isoformat | 验收脚本或直接用例 |
| `zcard` | `def zcard(redis_port, key)` | [24–30](scripts/verify_phase2.py#L24-L30) | 通过本机 Redis 的原生 RESP 协议读取投影计数。 | 上游 verify · 下游 socket.create_connection, f'*2\r\n$5\r\nZCARD\r\n${len(key)}\r\n{key}\r\n'.encode, sock.sendall, sock.makefile, int, response.readline().removeprefix, len, response.readline | 验收脚本或直接用例 |
| `verify` | `def verify(args, output)` | [33–148](scripts/verify_phase2.py#L33-L148) | 覆盖去重、乱序、水位驱逐、缓存降级、审计和重启。 | 下游 secrets.token_urlsafe, port, str, datetime.now, timedelta, ExitStack, start, stack.enter_context | 验收脚本或直接用例 |
| `verify.event` | `def event(key, offset=0, severity='WARNING')` | [50–54](scripts/verify_phase2.py#L50-L54) | 构造明确标注的测试数据，均指向受控服务。 | 上游 verify · 下游 iso, timedelta | 验收脚本或直接用例 |
| `main` | `def main()` | [151–163](scripts/verify_phase2.py#L151-L163) | 解析复现参数并在指定输出路径保留证据。 | 下游 argparse.ArgumentParser, parser.add_argument, parser.parse_args, args.output.resolve, output.mkdir, verify, parser.error, JAR.is_file | 验收脚本或直接用例 |
