package com.fm.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务轻量 Redis 分布式锁，仅用于多实例防重，不引入 ShedLock 等新依赖。
 * 局限：无续约（watchdog），调用方传入的 ttl 必须大于单轮任务最大耗时；
 * Redis 不可用时按「跳过本轮」处理，避免多实例同时执行造成数据错误。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerLock {

    /**
     * 释放必须先校验持有者再删除，且两步必须原子：
     * 若用「先 get 比对、再 delete」的非原子写法，锁已过期并被其他实例持有时会误删他人锁。
     */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end";
    private static final RedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(UNLOCK_LUA, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * @return 持锁令牌；未抢到锁或 Redis 异常时返回 empty，调用方应直接跳过本轮
     */
    public Optional<String> tryLock(String lockKey, long ttlSeconds) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("scheduler lock acquire failed, lockKey={}", lockKey, ex);
            return Optional.empty();
        }
    }

    /** 值校验 + delete 的原子释放；释放失败只告警，锁由 ttl 兜底自动过期 */
    public void release(String lockKey, String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
        } catch (RuntimeException ex) {
            log.warn("scheduler lock release failed, lockKey={}", lockKey, ex);
        }
    }
}
