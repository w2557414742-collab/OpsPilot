package dev.opspilot.alert;

import dev.opspilot.alert.AlertModels.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

/** 告警接入数据库接口。所有对外可追踪对象均以租户为复合键。 */
@Mapper
public interface AlertStore {
    /** 验证部署配置的告警租户已初始化。 */
    @Select("SELECT EXISTS(SELECT 1 FROM tenant WHERE id=#{tenant})")
    boolean tenantExists(String tenant);

    /** 初始化已认证来源的元数据，凭证只存于部署环境。 */
    @Insert("INSERT IGNORE INTO alert_source(tenant_id,id) VALUES(#{tenant},#{source})")
    void ensureSource(String tenant, String source);

    /** 保留原始请求及接收时钟。 */
    @Insert("""
        INSERT INTO alert_receipt(tenant_id,id,source_id,format,raw_payload,received_ms)
        VALUES(#{tenant},#{id},#{source},#{format},#{raw},#{now})
        """)
    void receipt(String tenant, String id, String source, String format, byte[] raw, long now);

    /** 读取原始证据，鉴权由控制器负责。 */
    @Select("SELECT id,source_id,format,raw_payload,received_ms FROM alert_receipt WHERE tenant_id=#{tenant} AND id=#{id}")
    Receipt getReceipt(String tenant, String id);

    /** 创建或锁住稳定分组，随后用当前读获取最新信息。 */
    @Insert("""
        INSERT INTO alert_group(tenant_id,id,service_id,name) VALUES(#{tenant},#{id},#{service},#{name})
        ON DUPLICATE KEY UPDATE id=id
        """)
    void ensureGroup(String tenant, String id, String service, String name);

    String GROUP_COLUMNS = "id,service_id,name,watermark_ms,info_count,warning_count,critical_count,suppressed_count,next_candidate_ms";

    /** 在同一事务中以当前读取得被锁定的分组。 */
    @Select("SELECT " + GROUP_COLUMNS + " FROM alert_group WHERE tenant_id=#{tenant} AND id=#{id} FOR UPDATE")
    AlertGroup lockGroup(String tenant, String id);

    /** 读取当前租户最近推进的分组。 */
    @Select("SELECT " + GROUP_COLUMNS + " FROM alert_group WHERE tenant_id=#{tenant} ORDER BY watermark_ms DESC,id LIMIT 100")
    List<AlertGroup> groups(String tenant);

    /** 插入事件或保持旧事件，再读取旧摘要判断冲突。 */
    @Insert("""
        INSERT INTO alert_event(tenant_id,id,source_id,event_key,group_id,fingerprint,payload_digest,
                                occurred_ms,severity,status,counted,late)
        VALUES(#{tenant},#{id},#{source},#{event.eventKey},#{group},#{fingerprint},#{digest},
               #{event.occurredMs},#{event.severity},#{event.status},#{counted},#{late})
        ON DUPLICATE KEY UPDATE id=id
        """)
    void ensureEvent(String tenant, String id, String source, String group, Input event, String fingerprint,
                     String digest, boolean counted, boolean late);

    /** 用来源幂等复合键读取实际事件。 */
    @Select("""
        SELECT id,group_id,fingerprint,payload_digest,late FROM alert_event
        WHERE tenant_id=#{tenant} AND source_id=#{source} AND event_key=#{key} FOR UPDATE
        """)
    AlertEvent byKey(String tenant, String source, String key);

    /** 在窗口左边界推进时统计被淘汰的区间，使用分组索引。 */
    @Select("""
        SELECT COALESCE(SUM(severity='INFO'),0) AS info, COALESCE(SUM(severity='WARNING'),0) AS warning,
          COALESCE(SUM(severity='CRITICAL'),0) AS critical FROM alert_event
        WHERE tenant_id=#{tenant} AND group_id=#{group} AND counted=TRUE AND occurred_ms>#{old} AND occurred_ms<=#{cut}
        """)
    Counts evicted(String tenant, String group, long old, long cut);

    /** 提交窗口、严重等级计数与风暴抑制状态。调用方持有分组行锁。 */
    @Update("""
        UPDATE alert_group SET watermark_ms=#{group.watermarkMs}, info_count=#{group.infoCount},
          warning_count=#{group.warningCount}, critical_count=#{group.criticalCount},
          suppressed_count=#{group.suppressedCount}, next_candidate_ms=#{group.nextCandidateMs}
        WHERE tenant_id=#{tenant} AND id=#{group.id}
        """)
    void updateGroup(String tenant, AlertGroup group);

    /** 关联批次中每项请求与最终事件，包括重复提交。 */
    @Insert("""
        INSERT INTO alert_receipt_event(tenant_id,receipt_id,ordinal,event_id,duplicate_event)
        VALUES(#{tenant},#{receipt},#{ordinal},#{event},#{duplicate})
        """)
    void link(String tenant, String receipt, int ordinal, String event, boolean duplicate);

    /** 按原始顺序列出回执内全部处理记录。 */
    @Select("""
        SELECT re.ordinal,re.event_id,re.duplicate_event,e.late
        FROM alert_receipt_event re JOIN alert_event e ON e.tenant_id=re.tenant_id AND e.id=re.event_id
        WHERE re.tenant_id=#{tenant} AND re.receipt_id=#{receipt} ORDER BY re.ordinal
        """)
    List<DeduplicationRecord> records(String tenant, String receipt);

    /** 持久化触发的分诊候选。 */
    @Insert("""
        INSERT INTO incident_candidate(tenant_id,id,group_id,event_id,reason,watermark_ms)
        VALUES(#{tenant},#{candidate.id},#{candidate.groupId},#{candidate.eventId},
               #{candidate.reason},#{candidate.watermarkMs})
        """)
    void insertCandidate(String tenant, IncidentCandidate candidate);

    /** 列出租户内最近的候选。 */
    @Select("""
        SELECT id,group_id,event_id,reason,watermark_ms FROM incident_candidate
        WHERE tenant_id=#{tenant} ORDER BY watermark_ms DESC,id LIMIT 100
        """)
    List<IncidentCandidate> candidates(String tenant);
}
