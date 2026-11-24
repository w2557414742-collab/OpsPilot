package dev.opspilot.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 对外暴露固定安全错误文本，隐藏底层数据库和 Agent 错误。 */
public final class ApiFailure {
    private ApiFailure() {}

    /** 租户内资源缺失与跨租户资源使用相同响应。 */
    public static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
    }

    /** 将业务并发冲突转换为 HTTP 409。 */
    public static ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
