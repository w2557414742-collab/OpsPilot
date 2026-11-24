package dev.opspilot.domain;

/** Java 持有的诊断任务状态。UNKNOWN_OUTCOME 表示需要人工对账。 */
public enum TaskStatus {
    QUEUED, RUNNING, CANCEL_REQUESTED, SUCCEEDED, FAILED, CANCELLED, UNKNOWN_OUTCOME;

    /** 判断该状态是否允许调度器继续推进。 */
    public boolean active() {
        return this == QUEUED || this == RUNNING || this == CANCEL_REQUESTED;
    }
}
