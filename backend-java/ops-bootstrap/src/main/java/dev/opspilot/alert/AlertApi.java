package dev.opspilot.alert;

import dev.opspilot.alert.AlertModels.*;
import dev.opspilot.api.ApiFailure;
import dev.opspilot.auth.Auth;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** 来源接入与租户观察接口。来源凭证和用户 JWT 分属两条过滤链。 */
@RestController
public class AlertApi {
    private static final int PAYLOAD_LIMIT = 128 * 1024;
    private final AlertIngestion ingestion;
    private final AlertStore store;
    private final AlertNormalization normalize;
    private final AlertCache cache;
    private final String sourceTenant;
    private final String sourceId;
    private final String sourceToken;

    /** 加载部署侧专用来源凭证，缺省关闭接入。 */
    public AlertApi(AlertIngestion ingestion, AlertStore store, AlertNormalization normalize, AlertCache cache,
                    @Value("${opspilot.alert.tenant}") String sourceTenant,
                    @Value("${opspilot.alert.source}") String sourceId,
                    @Value("${opspilot.alert.token}") String sourceToken) {
        this.ingestion = ingestion;
        this.store = store;
        this.normalize = normalize;
        this.cache = cache;
        this.sourceTenant = sourceTenant;
        this.sourceId = sourceId;
        this.sourceToken = sourceToken;
        if (!sourceToken.isEmpty() && (!sourceTenant.matches(Auth.IDENTIFIER)
                || !sourceId.matches(Auth.IDENTIFIER) || sourceToken.length() < 32)) {
            throw new IllegalStateException("Configured alert source requires valid IDs and a 32-character token");
        }
    }

    /** 读取有界原始字节，验证来源与格式，成功持久化后返回 202 和回执。 */
    @PostMapping(path = "/ingest/v1/{tenant}/{source}/{format}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Acceptance accept(@PathVariable String tenant, @PathVariable String source, @PathVariable String format,
                             @RequestHeader(name = HttpHeaders.AUTHORIZATION, defaultValue = "") String authorization,
                             HttpServletRequest request) throws IOException {
        if (sourceToken.isEmpty() || !sourceTenant.equals(tenant) || !sourceId.equals(source)
                || !MessageDigest.isEqual(("Bearer " + sourceToken).getBytes(StandardCharsets.UTF_8),
                        authorization.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid alert source");
        }
        byte[] raw = request.getInputStream().readNBytes(PAYLOAD_LIMIT + 1);
        if (raw.length > PAYLOAD_LIMIT) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Alert body exceeds 128 KiB");
        }
        List<Input> events = switch (format) {
            case "events" -> normalize.standard(raw);
            case "alertmanager" -> normalize.alertmanager(raw);
            default -> throw ApiFailure.missing();
        };
        AlertIngestion.Committed result = ingestion.accept(tenant, source, format, raw, events);
        cache.project(tenant, result.projections());
        return result.receipt();
    }

    /** 获取当前租户的告警聚合视图。 */
    @GetMapping("/api/v1/alerts/groups")
    public List<AlertGroup> groups(@AuthenticationPrincipal Jwt actor) {
        return store.groups(actor.getClaimAsString("tenant"));
    }

    /** 获取当前租户的故障分诊候选。 */
    @GetMapping("/api/v1/alerts/candidates")
    public List<IncidentCandidate> candidates(@AuthenticationPrincipal Jwt actor) {
        return store.candidates(actor.getClaimAsString("tenant"));
    }

    /** 按接收顺序读取批次的处理记录。 */
    @GetMapping("/api/v1/alerts/receipts/{id}")
    public Acceptance receipt(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id) {
        List<DeduplicationRecord> events = store.records(actor.getClaimAsString("tenant"), id.toString());
        if (events.isEmpty()) {
            throw ApiFailure.missing();
        }
        return new Acceptance(id.toString(), events);
    }

    /** 仅允许同租户管理员下载原始证据，按不透明附件返回。 */
    @GetMapping("/api/v1/alerts/receipts/{id}/raw")
    public ResponseEntity<byte[]> raw(@AuthenticationPrincipal Jwt actor, @PathVariable UUID id) {
        if (!"ADMIN".equals(actor.getClaimAsString("role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Raw alerts require admin role");
        }
        Receipt receipt = store.getReceipt(actor.getClaimAsString("tenant"), id.toString());
        if (receipt == null) {
            throw ApiFailure.missing();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alert-" + id + ".json\"")
                .header("X-Content-Type-Options", "nosniff").cacheControl(CacheControl.noStore())
                .body(receipt.rawPayload());
    }

    /** 来源入口不走用户 JWT 解码，控制器验证专用 Bearer 凭证。 */
    @Configuration(proxyBeanMethods = false)
    static class IngressSecurity {
        /** 只允许 POST 到声明的接入路径，其他方法由入口链拒绝。 */
        @Bean
        @Order(1)
        SecurityFilterChain alertsFilter(HttpSecurity http) throws Exception {
            return http.securityMatcher("/ingest/v1/**").csrf(csrf -> csrf.disable())
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(rules -> rules
                            .requestMatchers(HttpMethod.POST, "/ingest/v1/*/*/*").permitAll()
                            .anyRequest().denyAll()).build();
        }
    }
}
