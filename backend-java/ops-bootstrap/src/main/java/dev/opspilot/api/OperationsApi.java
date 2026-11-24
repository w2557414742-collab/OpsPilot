package dev.opspilot.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.opspilot.auth.Auth;
import dev.opspilot.domain.IncidentStatus;
import dev.opspilot.domain.TaskStatus;
import dev.opspilot.incident.IncidentService;
import dev.opspilot.store.OpsStore;
import dev.opspilot.store.Rows.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Web 入口从 JWT 推导租户和操作者，客户端只能指定租户内资源标识。 */
@RestController
@RequestMapping("/api/v1")
public class OperationsApi {
    private final OpsStore store;
    private final IncidentService incidents;
    private final ObjectMapper json;

    /** 注入查询、事务业务服务及结果转换器。 */
    public OperationsApi(OpsStore store, IncidentService incidents, ObjectMapper json) {
        this.store = store;
        this.incidents = incidents;
        this.json = json;
    }

    /** 创建资产的表单。 */
    public record NewAsset(@NotBlank @Pattern(regexp = Auth.IDENTIFIER) String id,
                           @NotBlank @Size(max = 160) String name) {}

    /** 创建工单的表单。 */
    public record NewIncident(@NotBlank @Pattern(regexp = Auth.IDENTIFIER) String serviceId,
                              @NotBlank @Size(max = 200) String title,
                              @NotBlank @Size(max = 2000) String description) {}

    /** 以客户端所读版本为前置条件的人工转换表单。 */
    public record Transition(@NotNull @PositiveOrZero Long expectedVersion, @NotNull IncidentStatus target) {}

    /** 诊断请求使用客户端读取的工单版本。 */
    public record Diagnosis(@NotNull @PositiveOrZero Long expectedVersion) {}

    /** 对外任务状态，内部租约令牌不参与序列化。 */
    public record TaskView(String id, String incidentId, String traceId, TaskStatus status, String runId,
                           int attempt, JsonNode result, String errorCode) {}

    /** 创建同租户的服务。 */
    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public Asset createAsset(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NewAsset request) {
        return incidents.createAsset(tenant(jwt), jwt.getSubject(), request.id(), request.name());
    }

    /** 列出当前租户的资产。 */
    @GetMapping("/services")
    public List<Asset> assets(@AuthenticationPrincipal Jwt jwt) {
        return store.assets(tenant(jwt));
    }

    /** 基于租户内的服务资产创建工单。 */
    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    public Incident create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NewIncident request) {
        return incidents.create(tenant(jwt), jwt.getSubject(), request.serviceId(), request.title(),
                request.description());
    }

    /** 列出当前租户近期的工单。 */
    @GetMapping("/incidents")
    public List<Incident> list(@AuthenticationPrincipal Jwt jwt) {
        return store.incidents(tenant(jwt));
    }

    /** 读取当前租户的单条工单。 */
    @GetMapping("/incidents/{id}")
    public Incident get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return incidents.incident(tenant(jwt), id.toString());
    }

    /** 按状态矩阵和客户端版本执行人工转换。 */
    @PostMapping("/incidents/{id}/transitions")
    public Incident transition(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                               @Valid @RequestBody Transition request) {
        return incidents.transition(tenant(jwt), jwt.getSubject(), id.toString(), request.expectedVersion(),
                request.target());
    }

    /** 创建任务或重放幂等请求，HTTP 202 不表示诊断已经完成。 */
    @PostMapping("/incidents/{id}/diagnoses")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TaskView diagnose(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                             @RequestHeader("Idempotency-Key") @Pattern(regexp = "[a-zA-Z0-9:_-]{1,80}") String key,
                             @Valid @RequestBody Diagnosis request) {
        return view(incidents.requestDiagnosis(tenant(jwt), jwt.getSubject(), id.toString(),
                request.expectedVersion(), key));
    }

    /** 读取本租户的任务及已结算结果。 */
    @GetMapping("/tasks/{id}")
    public TaskView task(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return view(incidents.task(tenant(jwt), id.toString()));
    }

    /** 提交取消，返回取消进度或已存在的终态。 */
    @PostMapping("/tasks/{id}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TaskView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return view(incidents.cancel(tenant(jwt), jwt.getSubject(), id.toString()));
    }

    /** 以事务流水 ID 为游标读取最近事件，调用端可持续轮询。 */
    @GetMapping("/incidents/{id}/events")
    public List<Audit> events(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @RequestParam(defaultValue = "0") @PositiveOrZero long after) {
        String tenant = tenant(jwt);
        incidents.incident(tenant, id.toString());
        return store.events(tenant, id.toString(), after);
    }

    /** 获取服务端签发的租户声明。 */
    private static String tenant(Jwt jwt) {
        return jwt.getClaimAsString("tenant");
    }

    /** 将 MySQL JSON 原样转成 JSON 节点，保留结构化响应。 */
    private TaskView view(Task task) {
        try {
            return new TaskView(task.id(), task.incidentId(), task.traceId(), task.status(), task.runId(),
                    task.attempt(), task.resultJson() == null ? null : json.readTree(task.resultJson()),
                    task.errorCode());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored result is not valid JSON", ex);
        }
    }
}
