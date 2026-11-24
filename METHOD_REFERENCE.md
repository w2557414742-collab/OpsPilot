# Java 方法参考

索引来自 JDK 21 语法树，收录项目源码中显式声明的方法和构造器。记录类型的隐式访问器由编译器生成。签名栏省略注解，链接行号包含对应源码。

调用栏列举方法体中的静态调用表达式。对源码中仅有一处同名声明的方法，列出项目内静态上游引用。Spring、HTTP 和 MyBatis 的实际运行入口见 [调用流程](CALL_FLOW.md)。
测试链接按所属功能链路给出。入口方法及其辅助方法的验证覆盖以测试断言为准。

## OpsPilot

Package `dev.opspilot` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/OpsPilot.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/OpsPilot.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `OpsPilot.main` [12–14](backend-java/ops-bootstrap/src/main/java/dev/opspilot/OpsPilot.java#L12-L14) | `public static void main(String[] args)` | `String[] args` | `void` | 从命令行参数和环境变量启动应用。 | 下游 SpringApplication.run | [诊断集成](scripts/verify_phase1.py) · [告警集成](scripts/verify_phase2.py) |

## AlertApi

Package `dev.opspilot.alert` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `AlertApi.AlertApi` [37–52](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java#L37-L52) | `public AlertApi(AlertIngestion ingestion, AlertStore store, AlertNormalization normalize, AlertCache cache, String sourceTenant, String sourceId, String sourceToken)` | `AlertIngestion ingestion, AlertStore store, AlertNormalization normalize, AlertCache cache, String sourceTenant, String sourceId, String sourceToken` | `AlertApi 实例` | 加载部署侧专用来源凭证，缺省关闭接入。 | 下游 sourceToken.isEmpty, sourceTenant.matches, sourceId.matches, sourceToken.length | [告警集成](scripts/verify_phase2.py) |
| `AlertApi.accept` [55–77](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java#L55-L77) | `public Acceptance accept(String tenant, String source, String format, String authorization, HttpServletRequest request) throws IOException` | `String tenant, String source, String format, String authorization, HttpServletRequest request` | `Acceptance` | 读取有界原始字节，验证来源与格式，成功持久化后返回 202 和回执。 | 下游 sourceToken.isEmpty, sourceTenant.equals, sourceId.equals, MessageDigest.isEqual, ("Bearer " + sourceToken).getBytes, authorization.getBytes, request.getInputStream().readNBytes, request.getInputStream | [告警集成](scripts/verify_phase2.py) |
| `AlertApi.groups` [80–83](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java#L80-L83) | `public List<AlertGroup> groups(Jwt actor)` | `Jwt actor` | `List<AlertGroup>` | 获取当前租户的告警聚合视图。 | 下游 store.groups, actor.getClaimAsString | [告警集成](scripts/verify_phase2.py) |
| `AlertApi.candidates` [86–89](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java#L86-L89) | `public List<IncidentCandidate> candidates(Jwt actor)` | `Jwt actor` | `List<IncidentCandidate>` | 获取当前租户的故障分诊候选。 | 下游 store.candidates, actor.getClaimAsString | [告警集成](scripts/verify_phase2.py) |
| `AlertApi.receipt` [92–99](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java#L92-L99) | `public Acceptance receipt(Jwt actor, UUID id)` | `Jwt actor, UUID id` | `Acceptance` | 按接收顺序读取批次的处理记录。 | 下游 store.records, actor.getClaimAsString, id.toString, events.isEmpty, ApiFailure.missing | [告警集成](scripts/verify_phase2.py) |
| `AlertApi.raw` [102–115](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java#L102-L115) | `public ResponseEntity<byte[]> raw(Jwt actor, UUID id)` | `Jwt actor, UUID id` | `ResponseEntity<byte[]>` | 仅允许同租户管理员下载原始证据，按不透明附件返回。 | 下游 "ADMIN".equals, actor.getClaimAsString, store.getReceipt, id.toString, ApiFailure.missing, ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header, ResponseEntity.ok().contentType, ResponseEntity.ok | [告警集成](scripts/verify_phase2.py) |
| `AlertApi.IngressSecurity.alertsFilter` [121–129](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertApi.java#L121-L129) | `SecurityFilterChain alertsFilter(HttpSecurity http) throws Exception` | `HttpSecurity http` | `SecurityFilterChain` | 只允许 POST 到声明的接入路径，其他方法由入口链拒绝。 | 下游 http.securityMatcher("/ingest/v1/**").csrf, http.securityMatcher, csrf.disable, s.sessionCreationPolicy, rules.requestMatchers(HttpMethod.POST, "/ingest/v1/*/*/*").permitAll, rules.requestMatchers | [告警集成](scripts/verify_phase2.py) |

## AlertCache

Package `dev.opspilot.alert` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertCache.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertCache.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `AlertCache.AlertCache` [34–40](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertCache.java#L34-L40) | `public AlertCache(StringRedisTemplate redis, MeterRegistry registry, boolean enabled)` | `StringRedisTemplate redis, MeterRegistry registry, boolean enabled` | `AlertCache 实例` | 配置 Redis 客户端和固定基数的度量。 | 下游 registry.counter | [告警集成](scripts/verify_phase2.py) |
| `AlertCache.project` [43–59](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertCache.java#L43-L59) | `public void project(String tenant, List<Projection> projections)` | `String tenant, List<Projection> projections` | `void` | 投影已提交事件。原子裁剪和计数处理回调乱序，故障时停止本批缓存写入。 | 上游 AlertApi.accept · 下游 item.groupId, redis.execute, List.of, item.eventId, Long.toString, item.eventMs, item.watermarkMs, item.counted | [告警集成](scripts/verify_phase2.py) |

## AlertFingerprint

Package `dev.opspilot.alert` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertFingerprint.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertFingerprint.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `AlertFingerprint.AlertFingerprint` [18–20](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertFingerprint.java#L18-L20) | `public AlertFingerprint(ObjectMapper json)` | `ObjectMapper json` | `AlertFingerprint 实例` | 复用业务配置的 JSON 编码。输入标签已按字典序排序。 | 见声明与实现。 | [告警集成](scripts/verify_phase2.py) |
| `AlertFingerprint.group` [23–25](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertFingerprint.java#L23-L25) | `public String group(Input event)` | `Input event` | `String` | 同服务同告警名聚入同一组，租户由主键承载。 | 上游 AlertIngestion.accept, AlertIngestion.apply · 下游 digest, List.of, event.serviceId, event.name | [告警集成](scripts/verify_phase2.py) |
| `AlertFingerprint.fingerprint` [28–30](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertFingerprint.java#L28-L30) | `public String fingerprint(Input event)` | `Input event` | `String` | 指纹稳定地标识同一被观测实体，严重等级及发生时间不参与。 | 上游 AlertFingerprint.webhookKey, AlertIngestion.apply · 下游 digest, List.of, event.serviceId, event.name, event.labels | [告警集成](scripts/verify_phase2.py) |
| `AlertFingerprint.payload` [33–35](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertFingerprint.java#L33-L35) | `public String payload(Input event)` | `Input event` | `String` | 相同幂等键必须对应相同的规范负载。 | 上游 AlertIngestion.apply · 下游 digest | [告警集成](scripts/verify_phase2.py) |
| `AlertFingerprint.webhookKey` [38–40](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertFingerprint.java#L38-L40) | `public String webhookKey(Input event)` | `Input event` | `String` | Alertmanager 的重复通知基于同一实体、状态、时间和等级折叠。 | 上游 AlertNormalization.alertmanager · 下游 digest, List.of, fingerprint, event.status, event.occurredMs, event.severity | [告警集成](scripts/verify_phase2.py) |
| `AlertFingerprint.digest` [43–49](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertFingerprint.java#L43-L49) | `private String digest(Object value)` | `Object value` | `String` | 将长度自描述的 JSON 编码转换为 SHA-256 十六进制摘要。 | 上游 AlertFingerprint.fingerprint, AlertFingerprint.group, AlertFingerprint.payload, AlertFingerprint.webhookKey · 下游 HexFormat.of().formatHex, HexFormat.of, MessageDigest.getInstance("SHA-256").digest, MessageDigest.getInstance, json.writeValueAsBytes | [告警集成](scripts/verify_phase2.py) |

## AlertIngestion

Package `dev.opspilot.alert` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `AlertIngestion.AlertIngestion` [28–32](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java#L28-L32) | `public AlertIngestion(AlertStore alerts, OpsStore ops, AlertFingerprint ids)` | `AlertStore alerts, OpsStore ops, AlertFingerprint ids` | `AlertIngestion 实例` | 注入告警、资产与哈希计算。 | 见声明与实现。 | [告警集成](scripts/verify_phase2.py) |
| `AlertIngestion.accept` [44–67](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java#L44-L67) | `public Committed accept(String tenant, String source, String format, byte[] raw, List<Input> inputs)` | `String tenant, String source, String format, byte[] raw, List<Input> inputs` | `Committed` | 原子接受全批次或回滚。使用 READ COMMITTED 确保等待分组锁后淘汰查询可见前序写入。 HTTP 客户端在返回前失联时，可用相同来源和事件键重放。 | 下游 Instant.now().toEpochMilli, Instant.now, alerts.tenantExists, inputs.stream().map(Input::serviceId).distinct().anyMatch, inputs.stream().map(Input::serviceId).distinct, inputs.stream().map, inputs.stream, ops.asset | [告警集成](scripts/verify_phase2.py) |
| `AlertIngestion.apply` [70–91](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java#L70-L91) | `private void apply(String tenant, String source, String receipt, long now, OrderedInput input, List<Projection> projections)` | `String tenant, String source, String receipt, long now, OrderedInput input, List<Projection> projections` | `void` | 注册事件、记录接收决策并推进新鲜事件的聚合状态。 | 上游 AlertIngestion.accept · 下游 input.event, alerts.ensureGroup, input.group, event.serviceId, event.name, alerts.lockGroup, Math.max, group.watermarkMs | [告警集成](scripts/verify_phase2.py) |
| `AlertIngestion.advance` [94–114](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java#L94-L114) | `private void advance(String tenant, AlertGroup group, Input event, String eventId, long watermark, boolean counted)` | `String tenant, AlertGroup group, Input event, String eventId, long watermark, boolean counted` | `void` | 按推进区间扣除过期告警并加入新值，冷却期间抑制重复候选。 | 上游 AlertIngestion.apply · 下游 group.watermarkMs, alerts.evicted, group.id, group.infoCount, expired.info, increment, group.warningCount, expired.warning | [告警集成](scripts/verify_phase2.py) |
| `AlertIngestion.increment` [117–119](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java#L117-L119) | `private static int increment(Input event, boolean counted, Severity severity)` | `Input event, boolean counted, Severity severity` | `int` | 当前事件为窗口中的指定严重级别时增加一。 | 上游 AlertCache.project, AlertIngestion.advance · 下游 event.severity | [告警集成](scripts/verify_phase2.py) |
| `AlertIngestion.uuid` [122–124](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertIngestion.java#L122-L124) | `private static String uuid()` | 无 | `String` | 生成与跨租户资源是非关联的内部 ID。 | 上游 AlertIngestion.accept, AlertIngestion.advance, AlertIngestion.apply · 下游 UUID.randomUUID().toString, UUID.randomUUID | [告警集成](scripts/verify_phase2.py) |

## AlertModels

Package `dev.opspilot.alert` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertModels.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertModels.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `AlertModels.AlertModels` [8](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertModels.java#L8) | `private AlertModels()` | 无 | `AlertModels 实例` | 源类的显式构造方法。 | 见声明与实现。 | [告警集成](scripts/verify_phase2.py) |
| `AlertModels.AlertGroup.firingCount` [27–29](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertModels.java#L27-L29) | `public long firingCount()` | 无 | `long` | 返回当前事件时间窗口中计入的告警数量。 | 见声明与实现。 | [告警集成](scripts/verify_phase2.py) |

## AlertNormalization

Package `dev.opspilot.alert` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `AlertNormalization.AlertNormalization` [24–28](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L24-L28) | `public AlertNormalization(ObjectMapper json, AlertFingerprint ids)` | `ObjectMapper json, AlertFingerprint ids` | `AlertNormalization 实例` | 启用重复键和尾随内容检查。 | 下游 json.reader().with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).with, json.reader().with, json.reader | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.standard` [31–38](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L31-L38) | `public List<Input> standard(byte[] bytes)` | `byte[] bytes` | `List<Input>` | 解析单条标准事件。客户端提供稳定的 eventId。 | 上游 AlertApi.accept · 下游 parse, root.size, STANDARD_FIELDS.size, STANDARD_FIELDS.stream().allMatch, STANDARD_FIELDS.stream, invalid, List.of, input | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.alertmanager` [41–62](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L41-L62) | `public List<Input> alertmanager(byte[] bytes)` | `byte[] bytes` | `List<Input>` | 解析 Alertmanager webhook v4，每个批次最多一百条。保留原文供管理员访问。 | 上游 AlertApi.accept · 下游 parse, root.path, "4".equals, text, alerts.isArray, alerts.isEmpty, alerts.size, invalid | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.input` [65–78](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L65-L78) | `private Input input(String key, String service, String name, String time, String severity, Status status, SortedMap<String, String> labels)` | `String key, String service, String name, String time, String severity, Status status, SortedMap<String, String> labels` | `Input` | 验证标识、时间和严重级别，并剔除已归一为专有字段的标签。 | 上游 AlertNormalization.alertmanager, AlertNormalization.standard · 下游 key.matches, service.matches, name.matches, invalid, labels.keySet().removeAll, labels.keySet, Set.of, epoch | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.parse` [81–91](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L81-L91) | `private JsonNode parse(byte[] bytes)` | `byte[] bytes` | `JsonNode` | 解析 JSON 对象，统一隐藏底层解析错误。 | 上游 AlertNormalization.alertmanager, AlertNormalization.epoch, AlertNormalization.standard · 下游 reader.readTree, root.isObject, invalid | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.text` [94–100](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L94-L100) | `private String text(JsonNode node, String name)` | `JsonNode node, String name` | `String` | 提取文本字段。空值或非文本触发 400。 | 上游 AlertNormalization.alertmanager, AlertNormalization.standard, AlertNormalization.status · 下游 node.path, value.isTextual, value.textValue().isBlank, value.textValue, invalid | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.status` [103–109](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L103-L109) | `private Status status(JsonNode node)` | `JsonNode node` | `Status` | 解析通知状态枚举。 | 上游 AlertFingerprint.webhookKey, AlertIngestion.apply, AlertNormalization.alertmanager, AlertNormalization.standard · 下游 Status.valueOf, text(node, "status").toUpperCase, text, invalid | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.labels` [112–125](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L112-L125) | `private SortedMap<String, String> labels(JsonNode node)` | `JsonNode node` | `SortedMap<String, String>` | 解析有界标签数、键和文本值。 | 上游 AlertFingerprint.fingerprint, AlertNormalization.alertmanager, AlertNormalization.standard · 下游 node.isObject, node.size, invalid, node.properties, value.getKey().matches, value.getKey, value.getValue().isTextual, value.getValue | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.epoch` [128–138](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L128-L138) | `private long epoch(String text)` | `String text` | `long` | 统一转成 Unix 毫秒。边界日期避免数值溢出。 | 上游 AlertNormalization.alertmanager, AlertNormalization.input · 下游 Instant.parse(text).toEpochMilli, Instant.parse, invalid | [告警集成](scripts/verify_phase2.py) |
| `AlertNormalization.invalid` [141–143](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertNormalization.java#L141-L143) | `private static ResponseStatusException invalid()` | 无 | `ResponseStatusException` | 按安全错误契约报告输入无效。 | 上游 AlertNormalization.alertmanager, AlertNormalization.epoch, AlertNormalization.input, AlertNormalization.labels | [告警集成](scripts/verify_phase2.py) |

## AlertStore

Package `dev.opspilot.alert` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `AlertStore.tenantExists` [11–12](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L11-L12) | `boolean tenantExists(String tenant)` | `String tenant` | `boolean` | 验证部署配置的告警租户已初始化。 | 上游 AlertIngestion.accept | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.ensureSource` [15–16](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L15-L16) | `void ensureSource(String tenant, String source)` | `String tenant, String source` | `void` | 初始化已认证来源的元数据，凭证只存于部署环境。 | 上游 AlertIngestion.accept | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.receipt` [19–23](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L19-L23) | `void receipt(String tenant, String id, String source, String format, byte[] raw, long now)` | `String tenant, String id, String source, String format, byte[] raw, long now` | `void` | 保留原始请求及接收时钟。 | 见声明与实现。 | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.getReceipt` [26–27](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L26-L27) | `Receipt getReceipt(String tenant, String id)` | `String tenant, String id` | `Receipt` | 读取原始证据，鉴权由控制器负责。 | 上游 AlertApi.raw | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.ensureGroup` [30–34](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L30-L34) | `void ensureGroup(String tenant, String id, String service, String name)` | `String tenant, String id, String service, String name` | `void` | 创建或锁住稳定分组，随后用当前读获取最新信息。 | 上游 AlertIngestion.apply | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.lockGroup` [39–40](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L39-L40) | `AlertGroup lockGroup(String tenant, String id)` | `String tenant, String id` | `AlertGroup` | 在同一事务中以当前读取得被锁定的分组。 | 上游 AlertIngestion.apply | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.groups` [43–44](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L43-L44) | `List<AlertGroup> groups(String tenant)` | `String tenant` | `List<AlertGroup>` | 读取当前租户最近推进的分组。 | 见声明与实现。 | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.ensureEvent` [47–55](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L47-L55) | `void ensureEvent(String tenant, String id, String source, String group, Input event, String fingerprint, String digest, boolean counted, boolean late)` | `String tenant, String id, String source, String group, Input event, String fingerprint, String digest, boolean counted, boolean late` | `void` | 插入事件或保持旧事件，再读取旧摘要判断冲突。 | 上游 AlertIngestion.apply | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.byKey` [58–62](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L58-L62) | `AlertEvent byKey(String tenant, String source, String key)` | `String tenant, String source, String key` | `AlertEvent` | 用来源幂等复合键读取实际事件。 | 见声明与实现。 | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.evicted` [65–70](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L65-L70) | `Counts evicted(String tenant, String group, long old, long cut)` | `String tenant, String group, long old, long cut` | `Counts` | 在窗口左边界推进时统计被淘汰的区间，使用分组索引。 | 上游 AlertIngestion.advance | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.updateGroup` [73–79](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L73-L79) | `void updateGroup(String tenant, AlertGroup group)` | `String tenant, AlertGroup group` | `void` | 提交窗口、严重等级计数与风暴抑制状态。调用方持有分组行锁。 | 上游 AlertIngestion.advance | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.link` [82–86](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L82-L86) | `void link(String tenant, String receipt, int ordinal, String event, boolean duplicate)` | `String tenant, String receipt, int ordinal, String event, boolean duplicate` | `void` | 关联批次中每项请求与最终事件，包括重复提交。 | 上游 AlertIngestion.apply | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.records` [89–94](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L89-L94) | `List<DeduplicationRecord> records(String tenant, String receipt)` | `String tenant, String receipt` | `List<DeduplicationRecord>` | 按原始顺序列出回执内全部处理记录。 | 上游 AlertApi.receipt, AlertIngestion.accept | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.insertCandidate` [97–102](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L97-L102) | `void insertCandidate(String tenant, IncidentCandidate candidate)` | `String tenant, IncidentCandidate candidate` | `void` | 持久化触发的分诊候选。 | 上游 AlertIngestion.advance | [告警集成](scripts/verify_phase2.py) |
| `AlertStore.candidates` [105–109](backend-java/ops-bootstrap/src/main/java/dev/opspilot/alert/AlertStore.java#L105-L109) | `List<IncidentCandidate> candidates(String tenant)` | `String tenant` | `List<IncidentCandidate>` | 列出租户内最近的候选。 | 见声明与实现。 | [告警集成](scripts/verify_phase2.py) |

## ApiFailure

Package `dev.opspilot.api` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/ApiFailure.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/ApiFailure.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `ApiFailure.ApiFailure` [8](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/ApiFailure.java#L8) | `private ApiFailure()` | 无 | `ApiFailure 实例` | 源类的显式构造方法。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `ApiFailure.missing` [11–13](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/ApiFailure.java#L11-L13) | `public static ResponseStatusException missing()` | 无 | `ResponseStatusException` | 租户内资源缺失与跨租户资源使用相同响应。 | 上游 AlertApi.accept, AlertApi.raw, AlertApi.receipt, AlertIngestion.accept | [诊断集成](scripts/verify_phase1.py) |
| `ApiFailure.conflict` [16–18](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/ApiFailure.java#L16-L18) | `public static ResponseStatusException conflict(String reason)` | `String reason` | `ResponseStatusException` | 将业务并发冲突转换为 HTTP 409。 | 上游 AlertIngestion.apply, Auth.create, IncidentService.applyTransition, IncidentService.createAsset | [诊断集成](scripts/verify_phase1.py) |

## OperationsApi

Package `dev.opspilot.api` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `OperationsApi.OperationsApi` [30–34](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L30-L34) | `public OperationsApi(OpsStore store, IncidentService incidents, ObjectMapper json)` | `OpsStore store, IncidentService incidents, ObjectMapper json` | `OperationsApi 实例` | 注入查询、事务业务服务及结果转换器。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.createAsset` [56–60](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L56-L60) | `public Asset createAsset(Jwt jwt, NewAsset request)` | `Jwt jwt, NewAsset request` | `Asset` | 创建同租户的服务。 | 下游 incidents.createAsset, tenant, jwt.getSubject, request.id, request.name | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.assets` [63–66](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L63-L66) | `public List<Asset> assets(Jwt jwt)` | `Jwt jwt` | `List<Asset>` | 列出当前租户的资产。 | 下游 store.assets, tenant | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.create` [69–74](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L69-L74) | `public Incident create(Jwt jwt, NewIncident request)` | `Jwt jwt, NewIncident request` | `Incident` | 基于租户内的服务资产创建工单。 | 下游 incidents.create, tenant, jwt.getSubject, request.serviceId, request.title, request.description | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.list` [77–80](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L77-L80) | `public List<Incident> list(Jwt jwt)` | `Jwt jwt` | `List<Incident>` | 列出当前租户近期的工单。 | 下游 store.incidents, tenant | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.get` [83–86](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L83-L86) | `public Incident get(Jwt jwt, UUID id)` | `Jwt jwt, UUID id` | `Incident` | 读取当前租户的单条工单。 | 上游 AgentClient.poll, AlertIngestion.accept, AlertNormalization.alertmanager, IncidentStatus.permits · 下游 incidents.incident, tenant, id.toString | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.transition` [89–94](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L89-L94) | `public Incident transition(Jwt jwt, UUID id, Transition request)` | `Jwt jwt, UUID id, Transition request` | `Incident` | 按状态矩阵和客户端版本执行人工转换。 | 下游 incidents.transition, tenant, jwt.getSubject, id.toString, request.expectedVersion, request.target | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.diagnose` [97–104](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L97-L104) | `public TaskView diagnose(Jwt jwt, UUID id, String key, Diagnosis request)` | `Jwt jwt, UUID id, String key, Diagnosis request` | `TaskView` | 创建任务或重放幂等请求，HTTP 202 不表示诊断已经完成。 | 下游 view, incidents.requestDiagnosis, tenant, jwt.getSubject, id.toString, request.expectedVersion | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.task` [107–110](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L107-L110) | `public TaskView task(Jwt jwt, UUID id)` | `Jwt jwt, UUID id` | `TaskView` | 读取本租户的任务及已结算结果。 | 下游 view, incidents.task, tenant, id.toString | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.cancel` [113–117](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L113-L117) | `public TaskView cancel(Jwt jwt, UUID id)` | `Jwt jwt, UUID id` | `TaskView` | 提交取消，返回取消进度或已存在的终态。 | 下游 view, incidents.cancel, tenant, jwt.getSubject, id.toString | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.events` [120–126](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L120-L126) | `public List<Audit> events(Jwt jwt, UUID id, long after)` | `Jwt jwt, UUID id, long after` | `List<Audit>` | 以事务流水 ID 为游标读取最近事件，调用端可持续轮询。 | 下游 tenant, incidents.incident, id.toString, store.events | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.tenant` [129–131](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L129-L131) | `private static String tenant(Jwt jwt)` | `Jwt jwt` | `String` | 获取服务端签发的租户声明。 | 上游 Auth.login, OperationsApi.assets, OperationsApi.cancel, OperationsApi.create · 下游 jwt.getClaimAsString | [诊断集成](scripts/verify_phase1.py) |
| `OperationsApi.view` [134–142](backend-java/ops-bootstrap/src/main/java/dev/opspilot/api/OperationsApi.java#L134-L142) | `private TaskView view(Task task)` | `Task task` | `TaskView` | 将 MySQL JSON 原样转成 JSON 节点，保留结构化响应。 | 上游 OperationsApi.cancel, OperationsApi.diagnose, OperationsApi.task · 下游 task.id, task.incidentId, task.traceId, task.status, task.runId, task.attempt, task.resultJson, json.readTree | [诊断集成](scripts/verify_phase1.py) |

## Auth

Package `dev.opspilot.auth` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `Auth.Auth` [47–54](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L47-L54) | `public Auth(OpsStore store, BCryptPasswordEncoder passwords, JwtEncoder encoder, TransactionTemplate transaction)` | `OpsStore store, BCryptPasswordEncoder passwords, JwtEncoder encoder, TransactionTemplate transaction` | `Auth 实例` | 注入密码编码器、签发器及账号写入事务。 | 下游 passwords.encode | [诊断集成](scripts/verify_phase1.py) |
| `Auth.login` [73–88](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L73-L88) | `public Token login(Login request)` | `Login request` | `Token` | 检查登录凭据，签发有效期 30 分钟的 HS256 JWT。 | 下游 store.account, request.tenant, request.username, account.passwordHash, request.password().getBytes, request.password, passwords.matches, Instant.now | [诊断集成](scripts/verify_phase1.py) |
| `Auth.create` [91–107](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L91-L107) | `public void create(Jwt actor, NewAccount request)` | `Jwt actor, NewAccount request` | `void` | 在管理员所属租户创建账号，并与审计记录一同提交。 | 下游 checkPasswordLength, request.password, passwords.encode, actor.getClaimAsString, transaction.executeWithoutResult, store.insertAccount, request.username, request.role().name | [诊断集成](scripts/verify_phase1.py) |
| `Auth.checkPasswordLength` [110–114](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L110-L114) | `private static void checkPasswordLength(String password)` | `String password` | `void` | BCrypt 按 UTF-8 字节计数，避免提交会被截断的密码。 | 上游 Auth.SecuritySetup.bootstrap, Auth.create · 下游 password.length, password.getBytes | [诊断集成](scripts/verify_phase1.py) |
| `Auth.SecuritySetup.passwords` [120–123](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L120-L123) | `BCryptPasswordEncoder passwords()` | 无 | `BCryptPasswordEncoder` | 创建 BCrypt 编码器。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `Auth.SecuritySetup.jwtSecret` [126–133](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L126-L133) | `SecretKey jwtSecret(String value)` | `String value` | `SecretKey` | 从环境加载至少 32 字节的签名密钥。 | 下游 value.getBytes | [诊断集成](scripts/verify_phase1.py) |
| `Auth.SecuritySetup.jwtEncoder` [136–139](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L136-L139) | `JwtEncoder jwtEncoder(SecretKey secret)` | `SecretKey secret` | `JwtEncoder` | 配置 HS256 JWT 签发器。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `Auth.SecuritySetup.jwtDecoder` [142–147](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L142-L147) | `JwtDecoder jwtDecoder(SecretKey secret)` | `SecretKey secret` | `JwtDecoder` | 验证算法、发行方和凭据时效。 | 下游 NimbusJwtDecoder.withSecretKey(secret).macAlgorithm, NimbusJwtDecoder.withSecretKey, decoder.setJwtValidator, JwtValidators.createDefaultWithIssuer | [诊断集成](scripts/verify_phase1.py) |
| `Auth.SecuritySetup.security` [150–171](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L150-L171) | `SecurityFilterChain security(HttpSecurity http) throws Exception` | `HttpSecurity http` | `SecurityFilterChain` | 对 Bearer API 实施角色限制，默认拒绝未注册路由。 | 下游 roles.setAuthoritiesClaimName, roles.setAuthorityPrefix, converter.setJwtGrantedAuthoritiesConverter, http.csrf((csrf)->csrf.disable()).sessionManagement, http.csrf, csrf.disable, s.sessionCreationPolicy, rules.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll | [诊断集成](scripts/verify_phase1.py) |
| `Auth.SecuritySetup.bootstrap` [174–193](backend-java/ops-bootstrap/src/main/java/dev/opspilot/auth/Auth.java#L174-L193) | `ApplicationRunner bootstrap(OpsStore store, BCryptPasswordEncoder passwords, TransactionTemplate tx, String tenant, String user, String password)` | `OpsStore store, BCryptPasswordEncoder passwords, TransactionTemplate tx, String tenant, String user, String password` | `ApplicationRunner` | 使用启动配置初始化一个租户管理员，重复执行保留现有账号。 | 下游 tenant.isEmpty, user.isEmpty, password.isEmpty, tenant.matches, user.matches, checkPasswordLength, passwords.encode, tx.executeWithoutResult | [诊断集成](scripts/verify_phase1.py) |

## IncidentService

Package `dev.opspilot.incident` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `IncidentService.IncidentService` [22–24](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L22-L24) | `public IncidentService(OpsStore store)` | `OpsStore store` | `IncidentService 实例` | 注入租户隔离的数据访问接口。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.createAsset` [27–36](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L27-L36) | `public Asset createAsset(String tenant, String actor, String id, String name)` | `String tenant, String actor, String id, String name` | `Asset` | 创建服务资产并记录操作者。 | 下游 store.insertAsset, ApiFailure.conflict, store.audit | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.create` [39–48](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L39-L48) | `public Incident create(String tenant, String actor, String serviceId, String title, String description)` | `String tenant, String actor, String serviceId, String title, String description` | `Incident` | 验证资产归属并创建工单。状态流水与业务记录在同一事务内写入。 | 下游 store.asset, ApiFailure.missing, UUID.randomUUID().toString, UUID.randomUUID, store.insertIncident, store.audit, store.incident | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.incident` [51–57](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L51-L57) | `public Incident incident(String tenant, String id)` | `String tenant, String id` | `Incident` | 读取归属当前租户的工单。缺失资源返回 404。 | 下游 store.incident, ApiFailure.missing | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.transition` [60–68](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L60-L68) | `public Incident transition(String tenant, String actor, String id, long version, IncidentStatus next)` | `String tenant, String actor, String id, long version, IncidentStatus next` | `Incident` | 人工触发分诊、人工复核或关闭。旧版本与非法边均返回 409。 | 下游 lockedIncident, MANUAL_TARGETS.contains, ApiFailure.conflict, applyTransition, store.incident | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.requestDiagnosis` [74–90](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L74-L90) | `public Task requestDiagnosis(String tenant, String actor, String incidentId, long version, String key)` | `String tenant, String actor, String incidentId, long version, String key` | `Task` | 同步创建持久化任务与 DIAGNOSING 状态。工单锁串行化同一工单的请求， 重放在版本检查之前返回原任务。后台调度在该事务提交后可见。 | 上游 OperationsApi.diagnose · 下游 lockedIncident, store.byKey, previous.incidentVersion, ApiFailure.conflict, applyTransition, UUID.randomUUID().toString, UUID.randomUUID, UUID.randomUUID().toString().replace | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.task` [93–99](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L93-L99) | `public Task task(String tenant, String id)` | `String tenant, String id` | `Task` | 读取租户内的任务，供任务详情和取消入口复用。 | 下游 store.task, ApiFailure.missing | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.cancel` [102–117](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L102-L117) | `public Task cancel(String tenant, String actor, String id)` | `String tenant, String actor, String id` | `Task` | 尝试取消任务。未领取任务可本地确认，已领取任务需由 Agent 对账。 | 下游 task, lockedIncident, existing.incidentId, store.lockTask, task.status().active, task.status, store.cancel, current.status | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.lockedIncident` [120–126](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L120-L126) | `private Incident lockedIncident(String tenant, String id)` | `String tenant, String id` | `Incident` | 锁定租户内工单。调用端持有业务事务。 | 上游 IncidentService.cancel, IncidentService.requestDiagnosis, IncidentService.transition · 下游 store.lockIncident, ApiFailure.missing | [诊断集成](scripts/verify_phase1.py) |
| `IncidentService.applyTransition` [129–136](backend-java/ops-bootstrap/src/main/java/dev/opspilot/incident/IncidentService.java#L129-L136) | `private void applyTransition(String tenant, String actor, Incident current, long version, IncidentStatus next)` | `String tenant, String actor, Incident current, long version, IncidentStatus next` | `void` | 校验状态矩阵和版本，在当前事务记录状态转换。 | 上游 IncidentService.cancel, IncidentService.requestDiagnosis, IncidentService.transition · 下游 current.version, current.status().permits, current.status, store.transition, current.id, ApiFailure.conflict, store.audit | [诊断集成](scripts/verify_phase1.py) |

## OpsStore

Package `dev.opspilot.store` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `OpsStore.ensureTenant` [13–14](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L13-L14) | `void ensureTenant(String tenant)` | `String tenant` | `void` | 初始化租户，重复运行保留已有租户。 | 上游 Auth.SecuritySetup.bootstrap | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.account` [17–18](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L17-L18) | `Account account(String tenant, String username)` | `String tenant, String username` | `Account` | 读取一个租户内的用户。 | 上游 Auth.login | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.insertAccount` [21–22](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L21-L22) | `void insertAccount(String tenant, String username, String hash, String role)` | `String tenant, String username, String hash, String role` | `void` | 写入账号。主键冲突由业务层转译为 409。 | 上游 Auth.create | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.seedAccount` [25–26](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L25-L26) | `void seedAccount(String tenant, String username, String hash)` | `String tenant, String username, String hash` | `void` | 启动时插入管理员，已有账号的密码和角色保持原值。 | 上游 Auth.SecuritySetup.bootstrap | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.insertAsset` [29–30](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L29-L30) | `void insertAsset(String tenant, String id, String name)` | `String tenant, String id, String name` | `void` | 写入服务资产。 | 上游 IncidentService.createAsset | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.asset` [33–34](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L33-L34) | `Asset asset(String tenant, String id)` | `String tenant, String id` | `Asset` | 按复合主键查找服务资产。 | 上游 AlertIngestion.accept, IncidentService.create | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.assets` [37–38](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L37-L38) | `List<Asset> assets(String tenant)` | `String tenant` | `List<Asset>` | 有界读取服务资产目录。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.insertIncident` [43–47](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L43-L47) | `void insertIncident(String tenant, String id, String serviceId, String title, String description)` | `String tenant, String id, String serviceId, String title, String description` | `void` | 创建 OPEN 工单。 | 上游 IncidentService.create | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.incident` [50–51](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L50-L51) | `Incident incident(String tenant, String id)` | `String tenant, String id` | `Incident` | 获取租户内的工单。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.lockIncident` [54–55](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L54-L55) | `Incident lockIncident(String tenant, String id)` | `String tenant, String id` | `Incident` | 获取工单行锁。跨表写入统一先锁工单，再锁任务。 | 上游 IncidentService.lockedIncident, TaskWorker.finish | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.incidents` [58–59](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L58-L59) | `List<Incident> incidents(String tenant)` | `String tenant` | `List<Incident>` | 有界读取最近的工单。 | 上游 OperationsApi.list | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.transition` [62–66](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L62-L66) | `int transition(String tenant, String id, long version, IncidentStatus previous, IncidentStatus next)` | `String tenant, String id, long version, IncidentStatus previous, IncidentStatus next` | `int` | 对指定版本与状态执行比较交换。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.insertTask` [69–74](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L69-L74) | `void insertTask(String tenant, String id, String incident, long version, String key, String traceId)` | `String tenant, String id, String incident, long version, String key, String traceId` | `void` | 持久化待分发任务，截止时间使用数据库时钟。 | 上游 IncidentService.requestDiagnosis | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.byKey` [82–86](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L82-L86) | `Task byKey(String tenant, String incident, String key)` | `String tenant, String incident, String key` | `Task` | 同一工单内，同一键的重复请求返回原任务。调用方持有工单行锁。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.task` [89–90](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L89-L90) | `Task task(String tenant, String id)` | `String tenant, String id` | `Task` | 租户内读取单任务。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.lockTask` [93–94](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L93-L94) | `Task lockTask(String tenant, String id)` | `String tenant, String id` | `Task` | 在取消与结算事务内读取最新任务快照。 | 上游 IncidentService.cancel | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.readyTask` [102–103](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L102-L103) | `Task readyTask()` | 无 | `Task` | 读取队列中最早到期的候选项。领取时再次检查调度条件。 | 上游 TaskWorker.tick | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.claim` [106–111](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L106-L111) | `int claim(String tenant, String id, String token)` | `String tenant, String id, String token` | `int` | 原子领取任务。令牌隔离超时并重新领取后的旧 worker。 | 上游 TaskWorker.tick | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.cancel` [114–118](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L114-L118) | `void cancel(String tenant, String id, TaskStatus status)` | `String tenant, String id, TaskStatus status` | `void` | 取消请求使现任 worker 的提交失效。正在飞行的 HTTP 请求由对账兜底。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.settle` [121–128](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L121-L128) | `int settle(String tenant, String id, String token, TaskStatus status, String runId, String result, String error, long delay)` | `String tenant, String id, String token, TaskStatus status, String runId, String result, String error, long delay` | `int` | 带令牌归还租约，安排复查或落库终态。 | 上游 TaskWorker.finish, TaskWorker.reconcile, TaskWorker.tick | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.audit` [131–135](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L131-L135) | `void audit(String tenant, String type, String id, String actor, String event, String detail)` | `String tenant, String type, String id, String actor, String event, String detail` | `void` | 按资源写入审计事件，随调用方业务事务提交。 | 上游 Auth.create, IncidentService.applyTransition, IncidentService.cancel, IncidentService.create | [诊断集成](scripts/verify_phase1.py) |
| `OpsStore.events` [138–143](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/OpsStore.java#L138-L143) | `List<Audit> events(String tenant, String id, long after)` | `String tenant, String id, long after` | `List<Audit>` | 有界增量读取同工单的事件流水。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |

## Rows

Package `dev.opspilot.store` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/Rows.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/Rows.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `Rows.Rows` [9](backend-java/ops-bootstrap/src/main/java/dev/opspilot/store/Rows.java#L9) | `private Rows()` | 无 | `Rows 实例` | 源类的显式构造方法。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |

## AgentClient

Package `dev.opspilot.task` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/AgentClient.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/AgentClient.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `AgentClient.AgentClient` [18–28](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/AgentClient.java#L18-L28) | `public AgentClient(String url, String token)` | `String url, String token` | `AgentClient 实例` | 构造有连接及请求超时的内部客户端。认证令牌仅发往固定地址。 | 下游 token.length, java.net.http.HttpClient.newBuilder().connectTimeout, java.net.http.HttpClient.newBuilder, Duration.ofSeconds, factory.setReadTimeout, RestClient.builder().baseUrl(url).requestFactory(factory).defaultHeaders, RestClient.builder().baseUrl(url).requestFactory, RestClient.builder().baseUrl | [诊断集成](scripts/verify_phase1.py) |
| `AgentClient.submit` [37–42](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/AgentClient.java#L37-L42) | `public Run submit(Task task, Incident incident)` | `Task task, Incident incident` | `Run` | 创建或读取同一 run。PUT 重放由 Python 的持久化主键及请求摘要去重。 | 上游 TaskWorker.reconcile · 下游 client.put().uri, client.put, task.tenantId, task.id, incident.id, incident.serviceId, incident.title, incident.description | [诊断集成](scripts/verify_phase1.py) |
| `AgentClient.poll` [45–48](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/AgentClient.java#L45-L48) | `public Run poll(Task task)` | `Task task` | `Run` | 查询已经确认存在的 run。 | 上游 TaskWorker.reconcile · 下游 client.get().uri, client.get, task.tenantId, task.id | [诊断集成](scripts/verify_phase1.py) |
| `AgentClient.cancel` [51–54](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/AgentClient.java#L51-L54) | `public Run cancel(Task task)` | `Task task` | `Run` | 请求取消，也可先于提交建立取消标记。 | 下游 client.put().uri, client.put, task.tenantId, task.id | [诊断集成](scripts/verify_phase1.py) |

## TaskWorker

Package `dev.opspilot.task` · [`backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java`](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `TaskWorker.TaskWorker` [29–33](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java#L29-L33) | `public TaskWorker(OpsStore store, AgentClient agent, TransactionTemplate transaction)` | `OpsStore store, AgentClient agent, TransactionTemplate transaction` | `TaskWorker 实例` | 绑定持久层、独立 HTTP 边界和结算事务。 | 见声明与实现。 | [诊断集成](scripts/verify_phase1.py) |
| `TaskWorker.tick` [36–69](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java#L36-L69) | `public void tick()` | 无 | `void` | 每轮领取一项到期任务。数据库租约在进程退出后自然到期。 | 下游 store.readyTask, UUID.randomUUID().toString, UUID.randomUUID, store.claim, ready.tenantId, ready.id, store.task, token.equals | [诊断集成](scripts/verify_phase1.py) |
| `TaskWorker.reconcile` [72–102](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java#L72-L102) | `private void reconcile(Task task)` | `Task task` | `void` | 将 Python 状态转成 Java 队列延迟或终态。提交丢失时重放相同 PUT。 | 上游 TaskWorker.tick · 下游 task.status, agent.cancel, task.runId, agent.poll, agent.submit, store.incident, task.tenantId, task.incidentId | [诊断集成](scripts/verify_phase1.py) |
| `TaskWorker.validReport` [105–116](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java#L105-L116) | `private boolean validReport(JsonNode report)` | `JsonNode report` | `boolean` | 校验 Phase 1 报告最小契约，结论只能是待补证据。 | 上游 TaskWorker.reconcile · 下游 report.isObject, report.toString().length, report.toString, report.path, "INSUFFICIENT_EVIDENCE".equals, report.path("conclusion").asText, PROVIDERS.contains, report.path("provider").asText | [诊断集成](scripts/verify_phase1.py) |
| `TaskWorker.finish` [122–141](backend-java/ops-bootstrap/src/main/java/dev/opspilot/task/TaskWorker.java#L122-L141) | `private void finish(Task task, TaskStatus status, String runId, String result, String error)` | `Task task, TaskStatus status, String runId, String result, String error` | `void` | 原子保存 Java 终态并审计结果。仅当前工单版本适用结果， 旧 run 仍可追溯。Phase 1 缺少遥测证据，因此有效结果也进入人工复核。 | 上游 TaskWorker.reconcile, TaskWorker.tick · 下游 transaction.executeWithoutResult, store.lockIncident, task.tenantId, task.incidentId, store.settle, task.id, task.leaseToken, status.name | [诊断集成](scripts/verify_phase1.py) |

## IncidentStatus

Package `dev.opspilot.domain` · [`backend-java/ops-domain/src/main/java/dev/opspilot/domain/IncidentStatus.java`](backend-java/ops-domain/src/main/java/dev/opspilot/domain/IncidentStatus.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `IncidentStatus.permits` [29–31](backend-java/ops-domain/src/main/java/dev/opspilot/domain/IncidentStatus.java#L29-L31) | `public boolean permits(IncidentStatus target)` | `IncidentStatus target` | `boolean` | 判断目标状态是否为当前状态的一条合法边。 @param target 目标状态 @return 存在合法边时返回 true | 上游 IncidentService.applyTransition, IncidentStatusTest.closedIsTerminal, IncidentStatusTest.followsMitigationSequence · 下游 TRANSITIONS.get(this).contains, TRANSITIONS.get | [JUnit 矩阵](backend-java/ops-domain/src/test/java/dev/opspilot/domain/IncidentStatusTest.java) · [集成](scripts/verify_phase1.py) |

## TaskStatus

Package `dev.opspilot.domain` · [`backend-java/ops-domain/src/main/java/dev/opspilot/domain/TaskStatus.java`](backend-java/ops-domain/src/main/java/dev/opspilot/domain/TaskStatus.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `TaskStatus.active` [8–10](backend-java/ops-domain/src/main/java/dev/opspilot/domain/TaskStatus.java#L8-L10) | `public boolean active()` | 无 | `boolean` | 判断该状态是否允许调度器继续推进。 | 上游 IncidentService.cancel | [诊断集成](scripts/verify_phase1.py) |

## IncidentStatusTest

Package `dev.opspilot.domain` · [`backend-java/ops-domain/src/test/java/dev/opspilot/domain/IncidentStatusTest.java`](backend-java/ops-domain/src/test/java/dev/opspilot/domain/IncidentStatusTest.java)

| 类与方法 | 方法签名 | 参数 | 返回 | 职责 | 调用关系 | 测试 |
| --- | --- | --- | --- | --- | --- | --- |
| `IncidentStatusTest.closedIsTerminal` [10–15](backend-java/ops-domain/src/test/java/dev/opspilot/domain/IncidentStatusTest.java#L10-L15) | `void closedIsTerminal()` | 无 | `void` | 关闭工单拒绝后续状态转换。 | 下游 values, assertFalse, CLOSED.permits | JUnit 用例 |
| `IncidentStatusTest.followsMitigationSequence` [18–28](backend-java/ops-domain/src/test/java/dev/opspilot/domain/IncidentStatusTest.java#L18-L28) | `void followsMitigationSequence()` | 无 | `void` | 顺序必须经过审批和恢复验证。 | 下游 assertTrue, MITIGATION_PROPOSED.permits, APPROVAL_PENDING.permits, MITIGATING.permits, RECOVERY_VERIFYING.permits, assertFalse, OPEN.permits | JUnit 用例 |
