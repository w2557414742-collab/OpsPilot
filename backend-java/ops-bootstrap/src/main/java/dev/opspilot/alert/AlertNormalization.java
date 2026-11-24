package dev.opspilot.alert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import dev.opspilot.alert.AlertModels.*;
import dev.opspilot.auth.Auth;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** 验证两种接入契约，生成事务使用的不可变输入。 */
@Component
public class AlertNormalization {
    private static final Set<String> STANDARD_FIELDS =
            Set.of("eventId", "serviceId", "name", "occurredAt", "severity", "status", "labels");
    private static final String NAME = "[a-zA-Z0-9][a-zA-Z0-9_.:-]{0,95}";
    private final ObjectReader reader;
    private final AlertFingerprint ids;

    /** 启用重复键和尾随内容检查。 */
    public AlertNormalization(ObjectMapper json, AlertFingerprint ids) {
        this.reader = json.reader().with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.ids = ids;
    }

    /** 解析单条标准事件。客户端提供稳定的 eventId。 */
    public List<Input> standard(byte[] bytes) {
        JsonNode root = parse(bytes);
        if (root.size() != STANDARD_FIELDS.size() || !STANDARD_FIELDS.stream().allMatch(root::has)) {
            throw invalid();
        }
        return List.of(input(text(root, "eventId"), text(root, "serviceId"), text(root, "name"),
                text(root, "occurredAt"), text(root, "severity"), status(root), labels(root.path("labels"))));
    }

    /** 解析 Alertmanager webhook v4，每个批次最多一百条。保留原文供管理员访问。 */
    public List<Input> alertmanager(byte[] bytes) {
        JsonNode root = parse(bytes);
        JsonNode alerts = root.path("alerts");
        if (!"4".equals(text(root, "version")) || !alerts.isArray() || alerts.isEmpty() || alerts.size() > 100) {
            throw invalid();
        }
        List<Input> events = new ArrayList<>(alerts.size());
        for (JsonNode alert : alerts) {
            SortedMap<String, String> labels = labels(alert.path("labels"));
            Status status = status(alert);
            String start = text(alert, "startsAt");
            String instant = status == Status.FIRING ? start : text(alert, "endsAt");
            if (epoch(instant) < epoch(start)) {
                throw invalid();
            }
            Input current = input("derived", labels.get("service"), labels.get("alertname"), instant,
                    labels.getOrDefault("severity", "warning"), status, labels);
            events.add(new Input(ids.webhookKey(current), current.serviceId(), current.name(), current.occurredMs(),
                    current.severity(), current.status(), current.labels()));
        }
        return List.copyOf(events);
    }

    /** 验证标识、时间和严重级别，并剔除已归一为专有字段的标签。 */
    private Input input(String key, String service, String name, String time, String severity, Status status,
                        SortedMap<String, String> labels) {
        if (!key.matches(NAME) || service == null || !service.matches(Auth.IDENTIFIER)
                || name == null || !name.matches(NAME)) {
            throw invalid();
        }
        labels.keySet().removeAll(Set.of("service", "alertname", "severity"));
        try {
            return new Input(key, service, name, epoch(time), Severity.valueOf(severity.toUpperCase(Locale.ROOT)),
                    status, Collections.unmodifiableSortedMap(labels));
        } catch (IllegalArgumentException ex) {
            throw invalid();
        }
    }

    /** 解析 JSON 对象，统一隐藏底层解析错误。 */
    private JsonNode parse(byte[] bytes) {
        try {
            JsonNode root = reader.readTree(bytes);
            if (root == null || !root.isObject()) {
                throw invalid();
            }
            return root;
        } catch (IOException ex) {
            throw invalid();
        }
    }

    /** 提取文本字段。空值或非文本触发 400。 */
    private String text(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw invalid();
        }
        return value.textValue();
    }

    /** 解析通知状态枚举。 */
    private Status status(JsonNode node) {
        try {
            return Status.valueOf(text(node, "status").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw invalid();
        }
    }

    /** 解析有界标签数、键和文本值。 */
    private SortedMap<String, String> labels(JsonNode node) {
        if (!node.isObject() || node.size() > 24) {
            throw invalid();
        }
        SortedMap<String, String> labels = new TreeMap<>();
        for (var value : node.properties()) {
            if (!value.getKey().matches(NAME) || !value.getValue().isTextual()
                    || value.getValue().textValue().length() > 160) {
                throw invalid();
            }
            labels.put(value.getKey(), value.getValue().textValue());
        }
        return labels;
    }

    /** 统一转成 Unix 毫秒。边界日期避免数值溢出。 */
    private long epoch(String text) {
        try {
            long epoch = Instant.parse(text).toEpochMilli();
            if (epoch < 0) {
                throw invalid();
            }
            return epoch;
        } catch (java.time.DateTimeException | ArithmeticException ex) {
            throw invalid();
        }
    }

    /** 按安全错误契约报告输入无效。 */
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid alert payload");
    }
}
