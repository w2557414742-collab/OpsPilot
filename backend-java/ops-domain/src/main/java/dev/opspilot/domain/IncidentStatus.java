package dev.opspilot.domain;

import java.util.Map;
import java.util.Set;

/** 工单生命周期与允许的边。调用端仍须验证权限及转换所需的业务证据。 */
public enum IncidentStatus {
    OPEN, TRIAGING, DIAGNOSING, MITIGATION_PROPOSED, APPROVAL_PENDING, MITIGATING,
    RECOVERY_VERIFYING, RESOLVED, CLOSED, MANUAL_REVIEW;

    private static final Map<IncidentStatus, Set<IncidentStatus>> TRANSITIONS = Map.of(
        OPEN, Set.of(TRIAGING, MANUAL_REVIEW),
        TRIAGING, Set.of(DIAGNOSING, MANUAL_REVIEW),
        DIAGNOSING, Set.of(MITIGATION_PROPOSED, MANUAL_REVIEW),
        MITIGATION_PROPOSED, Set.of(APPROVAL_PENDING, MANUAL_REVIEW),
        APPROVAL_PENDING, Set.of(MITIGATING, MANUAL_REVIEW),
        MITIGATING, Set.of(RECOVERY_VERIFYING, MANUAL_REVIEW),
        RECOVERY_VERIFYING, Set.of(RESOLVED, MANUAL_REVIEW),
        RESOLVED, Set.of(CLOSED, MANUAL_REVIEW),
        CLOSED, Set.of(),
        MANUAL_REVIEW, Set.of(TRIAGING)
    );

    /**
     * 判断目标状态是否为当前状态的一条合法边。
     * @param target 目标状态
     * @return 存在合法边时返回 true
     */
    public boolean permits(IncidentStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }
}
