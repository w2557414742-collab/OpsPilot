package dev.opspilot.alert;

import dev.opspilot.alert.AlertModels.Projection;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** 写入有界过期时间的 Redis 聚合投影。MySQL 独立决定接收和聚合结果。 */
@Component
public class AlertCache {
    private static final DefaultRedisScript<Long> COUNT = new DefaultRedisScript<>("""
        local watermark = math.max(tonumber(redis.call('GET', KEYS[1]) or '0'), tonumber(ARGV[3]))
        local cut = watermark - tonumber(ARGV[4])
        redis.call('SET', KEYS[1], string.format('%.0f', watermark), 'PX', 120000)
        if ARGV[5] == '1' and tonumber(ARGV[2]) > cut then
            redis.call('ZADD', KEYS[2], 'NX', ARGV[2], ARGV[1])
        end
        redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', string.format('%.0f', cut))
        redis.call('PEXPIRE', KEYS[2], 120000)
        return redis.call('ZCARD', KEYS[2])
        """, Long.class);

    private final StringRedisTemplate redis;
    private final boolean enabled;
    private final Counter completed;
    private final Counter failed;

    /** 配置 Redis 客户端和固定基数的度量。 */
    public AlertCache(StringRedisTemplate redis, MeterRegistry registry,
                      @Value("${opspilot.alert.redis-enabled}") boolean enabled) {
        this.redis = redis;
        this.enabled = enabled;
        this.completed = registry.counter("opspilot.alert.cache.projection", "outcome", "success");
        this.failed = registry.counter("opspilot.alert.cache.projection", "outcome", "unavailable");
    }

    /** 投影已提交事件。原子裁剪和计数处理回调乱序，故障时停止本批缓存写入。 */
    public void project(String tenant, List<Projection> projections) {
        if (!enabled) {
            return;
        }
        for (Projection item : projections) {
            String key = "opspilot:alert:{" + tenant + ":" + item.groupId() + "}";
            try {
                redis.execute(COUNT, List.of(key + ":watermark", key + ":events"), item.eventId(),
                        Long.toString(item.eventMs()), Long.toString(item.watermarkMs()),
                        Long.toString(AlertIngestion.WINDOW_MS), item.counted() ? "1" : "0");
                completed.increment();
            } catch (DataAccessException ex) {
                failed.increment();
                return;
            }
        }
    }
}
