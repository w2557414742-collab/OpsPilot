package dev.opspilot.task;

import com.fasterxml.jackson.databind.JsonNode;
import dev.opspilot.store.Rows.Incident;
import dev.opspilot.store.Rows.Task;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 通过幂等资源标识提交、查询和取消 Python run。 */
@Component
public class AgentClient {
    private final RestClient client;

    /** 构造有连接及请求超时的内部客户端。认证令牌仅发往固定地址。 */
    public AgentClient(@Value("${opspilot.agent.url}") String url,
                       @Value("${opspilot.agent.token}") String token) {
        if (token.length() < 32) {
            throw new IllegalStateException("Agent token requires at least 32 characters");
        }
        var http = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(4));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory)
                .defaultHeaders(headers -> headers.setBearerAuth(token)).build();
    }

    /** 请求体中的应用标识与工单快照在任务重放时保持稳定。 */
    public record Request(String incidentId, String serviceId, String title, String description, String traceId) {}

    /** Agent 内部接口的状态快照。 */
    public record Run(String runId, String status, JsonNode result, String errorCode) {}

    /** 创建或读取同一 run。PUT 重放由 Python 的持久化主键及请求摘要去重。 */
    public Run submit(Task task, Incident incident) {
        return client.put().uri("/internal/v1/tenants/{tenant}/runs/{run}", task.tenantId(), task.id())
                .body(new Request(incident.id(), incident.serviceId(), incident.title(), incident.description(),
                        task.traceId()))
                .retrieve().body(Run.class);
    }

    /** 查询已经确认存在的 run。 */
    public Run poll(Task task) {
        return client.get().uri("/internal/v1/tenants/{tenant}/runs/{run}", task.tenantId(), task.id())
                .retrieve().body(Run.class);
    }

    /** 请求取消，也可先于提交建立取消标记。 */
    public Run cancel(Task task) {
        return client.put().uri("/internal/v1/tenants/{tenant}/runs/{run}/cancel", task.tenantId(), task.id())
                .retrieve().body(Run.class);
    }
}
