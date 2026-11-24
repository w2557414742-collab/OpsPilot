package dev.opspilot.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.opspilot.alert.AlertModels.Input;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Component;

/** 用确定性的 JSON 编码计算跨重放稳定的分组、指纹及规范事件摘要。 */
@Component
public class AlertFingerprint {
    private final ObjectMapper json;

    /** 复用业务配置的 JSON 编码。输入标签已按字典序排序。 */
    public AlertFingerprint(ObjectMapper json) {
        this.json = json;
    }

    /** 同服务同告警名聚入同一组，租户由主键承载。 */
    public String group(Input event) {
        return digest(List.of(event.serviceId(), event.name()));
    }

    /** 指纹稳定地标识同一被观测实体，严重等级及发生时间不参与。 */
    public String fingerprint(Input event) {
        return digest(List.of(event.serviceId(), event.name(), event.labels()));
    }

    /** 相同幂等键必须对应相同的规范负载。 */
    public String payload(Input event) {
        return digest(event);
    }

    /** Alertmanager 的重复通知基于同一实体、状态、时间和等级折叠。 */
    public String webhookKey(Input event) {
        return digest(List.of(fingerprint(event), event.status(), event.occurredMs(), event.severity()));
    }

    /** 将长度自描述的 JSON 编码转换为 SHA-256 十六进制摘要。 */
    private String digest(Object value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value)));
        } catch (NoSuchAlgorithmException | JsonProcessingException ex) {
            throw new IllegalStateException("Cannot compute alert digest", ex);
        }
    }
}
