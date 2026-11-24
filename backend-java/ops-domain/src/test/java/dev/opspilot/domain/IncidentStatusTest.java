package dev.opspilot.domain;

import static dev.opspilot.domain.IncidentStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** 验证状态矩阵的收尾约束和处置必经阶段。 */
class IncidentStatusTest {
    /** 关闭工单拒绝后续状态转换。 */
    @Test
    void closedIsTerminal() {
        for (IncidentStatus candidate : values()) {
            assertFalse(CLOSED.permits(candidate));
        }
    }

    /** 顺序必须经过审批和恢复验证。 */
    @Test
    void followsMitigationSequence() {
        assertTrue(MITIGATION_PROPOSED.permits(APPROVAL_PENDING));
        assertTrue(APPROVAL_PENDING.permits(MITIGATING));
        assertTrue(MITIGATING.permits(RECOVERY_VERIFYING));
        assertTrue(RECOVERY_VERIFYING.permits(RESOLVED));
        assertTrue(RECOVERY_VERIFYING.permits(MANUAL_REVIEW));
        assertFalse(MITIGATION_PROPOSED.permits(MITIGATING));
        assertFalse(MITIGATING.permits(RESOLVED));
        assertFalse(OPEN.permits(DIAGNOSING));
    }
}
