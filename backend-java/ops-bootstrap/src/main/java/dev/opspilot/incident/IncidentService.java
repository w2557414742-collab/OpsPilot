package dev.opspilot.incident;

import dev.opspilot.api.ApiFailure;
import dev.opspilot.domain.IncidentStatus;
import dev.opspilot.domain.TaskStatus;
import dev.opspilot.store.OpsStore;
import dev.opspilot.store.Rows.*;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在业务事务内维护资产、工单、诊断任务与审计流水。 */
@Service
public class IncidentService {
    private static final Set<IncidentStatus> MANUAL_TARGETS =
            Set.of(IncidentStatus.TRIAGING, IncidentStatus.MANUAL_REVIEW, IncidentStatus.CLOSED);
    private final OpsStore store;

    /** 注入租户隔离的数据访问接口。 */
    public IncidentService(OpsStore store) {
        this.store = store;
    }

    /** 创建服务资产并记录操作者。 */
    @Transactional
    public Asset createAsset(String tenant, String actor, String id, String name) {
        try {
            store.insertAsset(tenant, id, name);
        } catch (DuplicateKeyException ex) {
            throw ApiFailure.conflict("Service already exists");
        }
        store.audit(tenant, "SERVICE", id, actor, "SERVICE_CREATED", name);
        return new Asset(id, name);
    }

    /** 验证资产归属并创建工单。状态流水与业务记录在同一事务内写入。 */
    @Transactional
    public Incident create(String tenant, String actor, String serviceId, String title, String description) {
        if (store.asset(tenant, serviceId) == null) {
            throw ApiFailure.missing();
        }
        String id = UUID.randomUUID().toString();
        store.insertIncident(tenant, id, serviceId, title, description);
        store.audit(tenant, "INCIDENT", id, actor, "INCIDENT_CREATED", "OPEN@0");
        return store.incident(tenant, id);
    }

    /** 读取归属当前租户的工单。缺失资源返回 404。 */
    public Incident incident(String tenant, String id) {
        Incident incident = store.incident(tenant, id);
        if (incident == null) {
            throw ApiFailure.missing();
        }
        return incident;
    }

    /** 人工触发分诊、人工复核或关闭。旧版本与非法边均返回 409。 */
    @Transactional
    public Incident transition(String tenant, String actor, String id, long version, IncidentStatus next) {
        Incident current = lockedIncident(tenant, id);
        if (!MANUAL_TARGETS.contains(next)) {
            throw ApiFailure.conflict("Transition requires a workflow action");
        }
        applyTransition(tenant, actor, current, version, next);
        return store.incident(tenant, id);
    }

    /**
     * 同步创建持久化任务与 DIAGNOSING 状态。工单锁串行化同一工单的请求，
     * 重放在版本检查之前返回原任务。后台调度在该事务提交后可见。
     */
    @Transactional
    public Task requestDiagnosis(String tenant, String actor, String incidentId, long version, String key) {
        Incident current = lockedIncident(tenant, incidentId);
        Task previous = store.byKey(tenant, incidentId, key);
        if (previous != null) {
            if (previous.incidentVersion() != version + 1) {
                throw ApiFailure.conflict("Idempotency key already used with another version");
            }
            return previous;
        }
        applyTransition(tenant, actor, current, version, IncidentStatus.DIAGNOSING);
        String id = UUID.randomUUID().toString();
        String trace = UUID.randomUUID().toString().replace("-", "");
        store.insertTask(tenant, id, incidentId, version + 1, key, trace);
        store.audit(tenant, "INCIDENT", incidentId, actor, "TASK_QUEUED", id);
        return store.task(tenant, id);
    }

    /** 读取租户内的任务，供任务详情和取消入口复用。 */
    public Task task(String tenant, String id) {
        Task task = store.task(tenant, id);
        if (task == null) {
            throw ApiFailure.missing();
        }
        return task;
    }

    /** 尝试取消任务。未领取任务可本地确认，已领取任务需由 Agent 对账。 */
    @Transactional
    public Task cancel(String tenant, String actor, String id) {
        Task existing = task(tenant, id);
        Incident current = lockedIncident(tenant, existing.incidentId());
        Task task = store.lockTask(tenant, id);
        if (!task.status().active() || task.status() == TaskStatus.CANCEL_REQUESTED) {
            return task;
        }
        TaskStatus status = task.status() == TaskStatus.QUEUED ? TaskStatus.CANCELLED : TaskStatus.CANCEL_REQUESTED;
        store.cancel(tenant, id, status);
        if (current.status() == IncidentStatus.DIAGNOSING && current.version() == task.incidentVersion()) {
            applyTransition(tenant, actor, current, current.version(), IncidentStatus.MANUAL_REVIEW);
        }
        store.audit(tenant, "INCIDENT", current.id(), actor, status.name(), id);
        return store.task(tenant, id);
    }

    /** 锁定租户内工单。调用端持有业务事务。 */
    private Incident lockedIncident(String tenant, String id) {
        Incident incident = store.lockIncident(tenant, id);
        if (incident == null) {
            throw ApiFailure.missing();
        }
        return incident;
    }

    /** 校验状态矩阵和版本，在当前事务记录状态转换。 */
    private void applyTransition(String tenant, String actor, Incident current, long version, IncidentStatus next) {
        if (current.version() != version || !current.status().permits(next)
                || store.transition(tenant, current.id(), version, current.status(), next) != 1) {
            throw ApiFailure.conflict("Stale incident version or invalid transition");
        }
        store.audit(tenant, "INCIDENT", current.id(), actor, "INCIDENT_TRANSITION",
                current.status() + " -> " + next + "@" + (version + 1));
    }
}
