package dev.opspilot.alert;

import dev.opspilot.alert.AlertModels.*;
import dev.opspilot.api.ApiFailure;
import dev.opspilot.store.OpsStore;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 用 MySQL 事务完成批次回执、去重、窗口计数及候选触发。 */
@Service
public class AlertIngestion {
    static final long WINDOW_MS = 60_000;
    private static final long MAX_AGE_MS = 300_000;
    private static final long FUTURE_TOLERANCE_MS = 30_000;
    private static final long COOLDOWN_MS = 120_000;
    private static final long STORM_THRESHOLD = 5;
    private final AlertStore alerts;
    private final OpsStore ops;
    private final AlertFingerprint ids;

    /** 注入告警、资产与哈希计算。 */
    public AlertIngestion(AlertStore alerts, OpsStore ops, AlertFingerprint ids) {
        this.alerts = alerts;
        this.ops = ops;
        this.ids = ids;
    }

    /** 提供提交后的回执与可选缓存投影数据。 */
    public record Committed(Acceptance receipt, List<Projection> projections) {}

    /** 保留通知原序与稳定加锁顺序所需的键。 */
    private record OrderedInput(int ordinal, Input event, String group) {}

    /**
     * 原子接受全批次或回滚。使用 READ COMMITTED 确保等待分组锁后淘汰查询可见前序写入。
     * HTTP 客户端在返回前失联时，可用相同来源和事件键重放。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Committed accept(String tenant, String source, String format, byte[] raw, List<Input> inputs) {
        long now = Instant.now().toEpochMilli();
        if (!alerts.tenantExists(tenant) || inputs.stream().map(Input::serviceId).distinct()
                .anyMatch(id -> ops.asset(tenant, id) == null)) {
            throw ApiFailure.missing();
        }
        if (inputs.stream().anyMatch(input -> input.occurredMs() > now + FUTURE_TOLERANCE_MS)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alert time is too far in the future");
        }
        alerts.ensureSource(tenant, source);
        String receiptId = uuid();
        alerts.receipt(tenant, receiptId, source, format, raw, now);

        List<OrderedInput> ordered = IntStream.range(0, inputs.size())
                .mapToObj(i -> new OrderedInput(i, inputs.get(i), ids.group(inputs.get(i))))
                .sorted(Comparator.comparing(OrderedInput::group).thenComparing(item -> item.event().eventKey()))
                .toList();
        List<Projection> projections = new ArrayList<>();
        for (OrderedInput input : ordered) {
            apply(tenant, source, receiptId, now, input, projections);
        }
        return new Committed(new Acceptance(receiptId, alerts.records(tenant, receiptId)), List.copyOf(projections));
    }

    /** 注册事件、记录接收决策并推进新鲜事件的聚合状态。 */
    private void apply(String tenant, String source, String receipt, long now, OrderedInput input,
                       List<Projection> projections) {
        Input event = input.event();
        alerts.ensureGroup(tenant, input.group(), event.serviceId(), event.name());
        AlertGroup group = alerts.lockGroup(tenant, input.group());
        long watermark = Math.max(group.watermarkMs(), event.occurredMs());
        boolean late = event.occurredMs() <= Math.max(now - MAX_AGE_MS, group.watermarkMs() - WINDOW_MS);
        boolean counted = !late && event.status() == Status.FIRING;
        String generated = uuid();
        String digest = ids.payload(event);
        alerts.ensureEvent(tenant, generated, source, group.id(), event, ids.fingerprint(event), digest, counted, late);
        AlertEvent existing = alerts.byKey(tenant, source, event.eventKey());
        if (!digest.equals(existing.payloadDigest())) {
            throw ApiFailure.conflict("Alert event key already used with a different payload");
        }
        boolean duplicate = !generated.equals(existing.id());
        alerts.link(tenant, receipt, input.ordinal(), existing.id(), duplicate);
        if (!duplicate && !late) {
            advance(tenant, group, event, existing.id(), watermark, counted);
            projections.add(new Projection(group.id(), existing.id(), event.occurredMs(), watermark, counted));
        }
    }

    /** 按推进区间扣除过期告警并加入新值，冷却期间抑制重复候选。 */
    private void advance(String tenant, AlertGroup group, Input event, String eventId, long watermark, boolean counted) {
        Counts expired = watermark > group.watermarkMs()
                ? alerts.evicted(tenant, group.id(), group.watermarkMs() - WINDOW_MS, watermark - WINDOW_MS)
                : new Counts(0, 0, 0);
        long info = group.infoCount() - expired.info() + increment(event, counted, Severity.INFO);
        long warning = group.warningCount() - expired.warning() + increment(event, counted, Severity.WARNING);
        long critical = group.criticalCount() - expired.critical() + increment(event, counted, Severity.CRITICAL);
        long next = group.nextCandidateMs();
        long suppressed = group.suppressedCount();
        if (counted && (event.severity() == Severity.CRITICAL || info + warning + critical >= STORM_THRESHOLD)) {
            if (watermark >= next) {
                String reason = event.severity() == Severity.CRITICAL ? "CRITICAL_ALERT" : "ALERT_STORM";
                alerts.insertCandidate(tenant, new IncidentCandidate(uuid(), group.id(), eventId, reason, watermark));
                next = watermark + COOLDOWN_MS;
            } else {
                suppressed++;
            }
        }
        alerts.updateGroup(tenant, new AlertGroup(group.id(), group.serviceId(), group.name(), watermark,
                info, warning, critical, suppressed, next));
    }

    /** 当前事件为窗口中的指定严重级别时增加一。 */
    private static int increment(Input event, boolean counted, Severity severity) {
        return counted && event.severity() == severity ? 1 : 0;
    }

    /** 生成与跨租户资源是非关联的内部 ID。 */
    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
