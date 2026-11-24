package dev.opspilot.task;

import com.fasterxml.jackson.databind.JsonNode;
import dev.opspilot.domain.IncidentStatus;
import dev.opspilot.domain.TaskStatus;
import dev.opspilot.store.OpsStore;
import dev.opspilot.store.Rows.Incident;
import dev.opspilot.store.Rows.Task;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Phase 1 数据库队列 worker。网络请求在事务外执行，结算需持有仍有效的租约。 */
@Component
public class TaskWorker {
    private static final Logger LOG = LoggerFactory.getLogger(TaskWorker.class);
    private static final Set<String> PROVIDERS = Set.of("DETERMINISTIC_TEST", "OPENAI_COMPATIBLE");
    private final OpsStore store;
    private final AgentClient agent;
    private final TransactionTemplate transaction;

    /** 绑定持久层、独立 HTTP 边界和结算事务。 */
    public TaskWorker(OpsStore store, AgentClient agent, TransactionTemplate transaction) {
        this.store = store;
        this.agent = agent;
        this.transaction = transaction;
    }

    /** 每轮领取一项到期任务。数据库租约在进程退出后自然到期。 */
    @Scheduled(fixedDelayString = "${opspilot.agent.tick-ms}")
    public void tick() {
        Task ready = store.readyTask();
        if (ready == null) {
            return;
        }
        String token = UUID.randomUUID().toString();
        if (store.claim(ready.tenantId(), ready.id(), token) != 1) {
            return;
        }
        Task task = store.task(ready.tenantId(), ready.id());
        if (!token.equals(task.leaseToken())) {
            return;
        }
        if (task.expired()) {
            finish(task, TaskStatus.UNKNOWN_OUTCOME, task.runId(), null, "RECONCILIATION_DEADLINE");
            return;
        }
        try {
            reconcile(task);
        } catch (RestClientException ex) {
            if (ex instanceof RestClientResponseException response && response.getStatusCode().is4xxClientError()
                    && !Set.of(408, 429).contains(response.getStatusCode().value())) {
                finish(task, TaskStatus.UNKNOWN_OUTCOME, task.runId(), null,
                        "AGENT_HTTP_" + response.getStatusCode().value());
                return;
            }
            long delayMs = Math.min(4000L, 250L << Math.min(task.attempt(), 4));
            LOG.info("Agent HTTP unavailable, task={} attempt={} class={}", task.id(), task.attempt(),
                    ex.getClass().getSimpleName());
            store.settle(task.tenantId(), task.id(), token, task.status(), task.runId(),
                    null, "AGENT_HTTP_UNAVAILABLE", delayMs * 1000);
        }
    }

    /** 将 Python 状态转成 Java 队列延迟或终态。提交丢失时重放相同 PUT。 */
    private void reconcile(Task task) {
        AgentClient.Run run;
        if (task.status() == TaskStatus.CANCEL_REQUESTED) {
            run = agent.cancel(task);
        } else if (task.runId() != null) {
            run = agent.poll(task);
        } else {
            run = agent.submit(task, store.incident(task.tenantId(), task.incidentId()));
        }
        if (run == null || !task.id().equals(run.runId()) || run.status() == null) {
            finish(task, TaskStatus.UNKNOWN_OUTCOME, task.runId(), null, "INVALID_AGENT_RESPONSE");
            return;
        }
        switch (run.status()) {
            case "QUEUED", "RUNNING", "CANCEL_REQUESTED" ->
                store.settle(task.tenantId(), task.id(), task.leaseToken(), task.status(), run.runId(),
                        null, null, 500_000);
            case "SUCCEEDED" -> {
                if (validReport(run.result())) {
                    finish(task, TaskStatus.SUCCEEDED, run.runId(), run.result().toString(), null);
                } else {
                    finish(task, TaskStatus.UNKNOWN_OUTCOME, run.runId(), null, "INVALID_AGENT_REPORT");
                }
            }
            case "FAILED" -> finish(task, TaskStatus.FAILED, run.runId(), null, "AGENT_RUN_FAILED");
            case "CANCELLED" -> finish(task, TaskStatus.CANCELLED, run.runId(), null, null);
            case "UNKNOWN_OUTCOME" ->
                finish(task, TaskStatus.UNKNOWN_OUTCOME, run.runId(), null, "AGENT_UNKNOWN_OUTCOME");
            default -> finish(task, TaskStatus.UNKNOWN_OUTCOME, run.runId(), null, "INVALID_AGENT_STATUS");
        }
    }

    /** 校验 Phase 1 报告最小契约，结论只能是待补证据。 */
    private boolean validReport(JsonNode report) {
        if (report == null || !report.isObject() || report.toString().length() > 8192) {
            return false;
        }
        JsonNode advice = report.path("nextChecks");
        return "INSUFFICIENT_EVIDENCE".equals(report.path("conclusion").asText())
                && PROVIDERS.contains(report.path("provider").asText())
                && report.path("evidence").isArray() && report.path("evidence").isEmpty()
                && advice.isArray() && !advice.isEmpty() && advice.size() <= 5
                && java.util.stream.StreamSupport.stream(advice.spliterator(), false)
                    .allMatch(item -> item.isTextual() && !item.asText().isBlank() && item.asText().length() <= 300);
    }

    /**
     * 原子保存 Java 终态并审计结果。仅当前工单版本适用结果，
     * 旧 run 仍可追溯。Phase 1 缺少遥测证据，因此有效结果也进入人工复核。
     */
    private void finish(Task task, TaskStatus status, String runId, String result, String error) {
        transaction.executeWithoutResult(ignored -> {
            Incident incident = store.lockIncident(task.tenantId(), task.incidentId());
            if (store.settle(task.tenantId(), task.id(), task.leaseToken(), status, runId, result, error, 0) != 1) {
                return;
            }
            String type = "TASK_" + status.name();
            if (incident.status() == IncidentStatus.DIAGNOSING && incident.version() == task.incidentVersion()) {
                if (store.transition(task.tenantId(), incident.id(), incident.version(), incident.status(),
                        IncidentStatus.MANUAL_REVIEW) != 1) {
                    throw new IllegalStateException("Locked incident version unexpectedly changed");
                }
                store.audit(task.tenantId(), "INCIDENT", incident.id(), "worker", "INCIDENT_TRANSITION",
                        "DIAGNOSING -> MANUAL_REVIEW@" + (incident.version() + 1));
            } else if (status == TaskStatus.SUCCEEDED) {
                type = "TASK_RESULT_STALE";
            }
            store.audit(task.tenantId(), "INCIDENT", incident.id(), "worker", type, task.id());
        });
    }
}
