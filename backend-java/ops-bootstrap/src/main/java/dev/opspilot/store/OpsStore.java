package dev.opspilot.store;

import dev.opspilot.domain.IncidentStatus;
import dev.opspilot.domain.TaskStatus;
import dev.opspilot.store.Rows.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

/** 参数绑定 SQL。业务入口按租户访问，调度器只读取全局队列头。 */
@Mapper
public interface OpsStore {
    /** 初始化租户，重复运行保留已有租户。 */
    @Insert("INSERT INTO tenant(id) VALUES(#{tenant}) ON DUPLICATE KEY UPDATE id=id")
    void ensureTenant(String tenant);

    /** 读取一个租户内的用户。 */
    @Select("SELECT tenant_id, username, password_hash, role FROM account WHERE tenant_id=#{tenant} AND username=#{username}")
    Account account(String tenant, String username);

    /** 写入账号。主键冲突由业务层转译为 409。 */
    @Insert("INSERT INTO account VALUES(#{tenant},#{username},#{hash},#{role})")
    void insertAccount(String tenant, String username, String hash, String role);

    /** 启动时插入管理员，已有账号的密码和角色保持原值。 */
    @Insert("INSERT INTO account VALUES(#{tenant},#{username},#{hash},'ADMIN') ON DUPLICATE KEY UPDATE username=username")
    void seedAccount(String tenant, String username, String hash);

    /** 写入服务资产。 */
    @Insert("INSERT INTO service_asset VALUES(#{tenant},#{id},#{name})")
    void insertAsset(String tenant, String id, String name);

    /** 按复合主键查找服务资产。 */
    @Select("SELECT id,name FROM service_asset WHERE tenant_id=#{tenant} AND id=#{id}")
    Asset asset(String tenant, String id);

    /** 有界读取服务资产目录。 */
    @Select("SELECT id,name FROM service_asset WHERE tenant_id=#{tenant} ORDER BY id LIMIT 100")
    List<Asset> assets(String tenant);

    String INCIDENT_COLUMNS = "id,service_id,title,description,status,version,created_at";

    /** 创建 OPEN 工单。 */
    @Insert("""
        INSERT INTO incident(tenant_id,id,service_id,title,description,status)
        VALUES(#{tenant},#{id},#{serviceId},#{title},#{description},'OPEN')
        """)
    void insertIncident(String tenant, String id, String serviceId, String title, String description);

    /** 获取租户内的工单。 */
    @Select("SELECT " + INCIDENT_COLUMNS + " FROM incident WHERE tenant_id=#{tenant} AND id=#{id}")
    Incident incident(String tenant, String id);

    /** 获取工单行锁。跨表写入统一先锁工单，再锁任务。 */
    @Select("SELECT " + INCIDENT_COLUMNS + " FROM incident WHERE tenant_id=#{tenant} AND id=#{id} FOR UPDATE")
    Incident lockIncident(String tenant, String id);

    /** 有界读取最近的工单。 */
    @Select("SELECT " + INCIDENT_COLUMNS + " FROM incident WHERE tenant_id=#{tenant} ORDER BY created_at DESC,id LIMIT 100")
    List<Incident> incidents(String tenant);

    /** 对指定版本与状态执行比较交换。 */
    @Update("""
        UPDATE incident SET status=#{next},version=version+1
        WHERE tenant_id=#{tenant} AND id=#{id} AND version=#{version} AND status=#{previous}
        """)
    int transition(String tenant, String id, long version, IncidentStatus previous, IncidentStatus next);

    /** 持久化待分发任务，截止时间使用数据库时钟。 */
    @Insert("""
        INSERT INTO diagnosis_task(tenant_id,id,incident_id,incident_version,idempotency_key,trace_id,status,deadline_at)
        VALUES(#{tenant},#{id},#{incident},#{version},#{key},#{traceId},'QUEUED',
               DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 90 SECOND))
        """)
    void insertTask(String tenant, String id, String incident, long version, String key, String traceId);

    String TASK_COLUMNS = """
        tenant_id,id,incident_id,incident_version,trace_id,status,run_id,attempt,lease_token,result_json,error_code,
        deadline_at <= CURRENT_TIMESTAMP(6) AS expired
        """;

    /** 同一工单内，同一键的重复请求返回原任务。调用方持有工单行锁。 */
    @Select("SELECT " + TASK_COLUMNS + """
        FROM diagnosis_task WHERE tenant_id=#{tenant} AND incident_id=#{incident} AND idempotency_key=#{key}
        FOR UPDATE
        """)
    Task byKey(String tenant, String incident, String key);

    /** 租户内读取单任务。 */
    @Select("SELECT " + TASK_COLUMNS + " FROM diagnosis_task WHERE tenant_id=#{tenant} AND id=#{id}")
    Task task(String tenant, String id);

    /** 在取消与结算事务内读取最新任务快照。 */
    @Select("SELECT " + TASK_COLUMNS + " FROM diagnosis_task WHERE tenant_id=#{tenant} AND id=#{id} FOR UPDATE")
    Task lockTask(String tenant, String id);

    String READY = """
        status IN ('QUEUED','RUNNING','CANCEL_REQUESTED') AND next_at<=CURRENT_TIMESTAMP(6)
          AND (lease_until IS NULL OR lease_until<=CURRENT_TIMESTAMP(6))
        """;

    /** 读取队列中最早到期的候选项。领取时再次检查调度条件。 */
    @Select("SELECT " + TASK_COLUMNS + " FROM diagnosis_task WHERE " + READY + " ORDER BY next_at,id LIMIT 1")
    Task readyTask();

    /** 原子领取任务。令牌隔离超时并重新领取后的旧 worker。 */
    @Update("""
        UPDATE diagnosis_task SET lease_token=#{token},lease_until=DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 12 SECOND),
            attempt=attempt+1,status=CASE WHEN status='QUEUED' THEN 'RUNNING' ELSE status END
        WHERE tenant_id=#{tenant} AND id=#{id} AND
        """ + READY)
    int claim(String tenant, String id, String token);

    /** 取消请求使现任 worker 的提交失效。正在飞行的 HTTP 请求由对账兜底。 */
    @Update("""
        UPDATE diagnosis_task SET status=#{status},lease_token=NULL,lease_until=NULL,next_at=CURRENT_TIMESTAMP(6)
        WHERE tenant_id=#{tenant} AND id=#{id}
        """)
    void cancel(String tenant, String id, TaskStatus status);

    /** 带令牌归还租约，安排复查或落库终态。 */
    @Update("""
        UPDATE diagnosis_task SET status=#{status},run_id=COALESCE(#{runId},run_id),
               result_json=#{result},error_code=#{error},next_at=TIMESTAMPADD(MICROSECOND,#{delay},CURRENT_TIMESTAMP(6)),
               lease_token=NULL,lease_until=NULL
        WHERE tenant_id=#{tenant} AND id=#{id} AND lease_token=#{token} AND lease_until>CURRENT_TIMESTAMP(6)
        """)
    int settle(String tenant, String id, String token, TaskStatus status, String runId,
               String result, String error, long delay);

    /** 按资源写入审计事件，随调用方业务事务提交。 */
    @Insert("""
        INSERT INTO audit_event(tenant_id,resource_type,resource_id,actor,event_type,detail)
        VALUES(#{tenant},#{type},#{id},#{actor},#{event},#{detail})
        """)
    void audit(String tenant, String type, String id, String actor, String event, String detail);

    /** 有界增量读取同工单的事件流水。 */
    @Select("""
        SELECT id,actor,event_type,detail,created_at FROM audit_event
        WHERE tenant_id=#{tenant} AND resource_type='INCIDENT' AND resource_id=#{id} AND id>#{after}
        ORDER BY id LIMIT 100
        """)
    List<Audit> events(String tenant, String id, long after);
}
