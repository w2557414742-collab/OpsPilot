package dev.opspilot.store;

import dev.opspilot.domain.IncidentStatus;
import dev.opspilot.domain.TaskStatus;
import java.time.LocalDateTime;

/** MyBatis 按列名构造的持久化记录。 */
public final class Rows {
    private Rows() {}

    /** 用户角色与 BCrypt 密码摘要，仅认证层读取。 */
    public record Account(String tenantId, String username, String passwordHash, String role) {}

    /** 租户内的服务资产。 */
    public record Asset(String id, String name) {}

    /** 带乐观版本号的工单快照。 */
    public record Incident(String id, String serviceId, String title, String description,
                           IncidentStatus status, long version, LocalDateTime createdAt) {}

    /** Java 诊断任务的调度、租约与结果快照。 */
    public record Task(String tenantId, String id, String incidentId, long incidentVersion, String traceId,
                       TaskStatus status, String runId, int attempt, String leaseToken,
                       String resultJson, String errorCode, boolean expired) {}

    /** 工单事件流水，可按单调 ID 增量读取。 */
    public record Audit(long id, String actor, String eventType, String detail, LocalDateTime createdAt) {}
}
