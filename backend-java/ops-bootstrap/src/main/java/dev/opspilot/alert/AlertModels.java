package dev.opspilot.alert;

import java.util.List;
import java.util.Map;

/** 告警输入、持久化快照与公开回执。 */
public final class AlertModels {
    private AlertModels() {}

    /** 标准严重等级。 */
    public enum Severity { INFO, WARNING, CRITICAL }

    /** 报警通知状态，用于识别是否需要聚合。 */
    public enum Status { FIRING, RESOLVED }

    /** 统一验证后的单条输入，标签由接入层按键排序。 */
    public record Input(String eventKey, String serviceId, String name, long occurredMs, Severity severity,
                        Status status, Map<String, String> labels) {}

    /** 事件已持久化后的分类。 */
    public record AlertEvent(String id, String groupId, String fingerprint, String payloadDigest, boolean late) {}

    /** MySQL 权威聚合视图，以事件时间 watermark 为右端点。 */
    public record AlertGroup(String id, String serviceId, String name, long watermarkMs, long infoCount,
                             long warningCount, long criticalCount, long suppressedCount, long nextCandidateMs) {
        /** 返回当前事件时间窗口中计入的告警数量。 */
        public long firingCount() {
            return infoCount + warningCount + criticalCount;
        }
    }

    /** 从滑动窗口淘汰的一段计数。 */
    public record Counts(long info, long warning, long critical) {}

    /** 被持久化的分诊候选，包含实际触发告警。 */
    public record IncidentCandidate(String id, String groupId, String eventId, String reason, long watermarkMs) {}

    /** 接入审计数据，保存原始请求字节。 */
    public record Receipt(String id, String sourceId, String format, byte[] rawPayload, long receivedMs) {}

    /** 原始请求中的第几项被接收或去重。 */
    public record DeduplicationRecord(int ordinal, String eventId, boolean duplicateEvent, boolean late) {}

    /** 成功提交后响应的批次及处理结果。 */
    public record Acceptance(String receiptId, List<DeduplicationRecord> events) {}

    /** Redis 投影只携带已经提交的事件和单调水位。 */
    public record Projection(String groupId, String eventId, long eventMs, long watermarkMs, boolean counted) {}
}
