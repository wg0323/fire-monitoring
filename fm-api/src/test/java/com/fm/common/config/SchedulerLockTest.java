package com.fm.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 分布式锁测试：抢锁成功返回令牌，被占用或 Redis 异常时跳过本轮。
 */
class SchedulerLockTest {

    private static final String KEY = "scheduler:lock:test";
    private static final long TTL = 60L;

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final SchedulerLock lock = new SchedulerLock(redisTemplate);

    @Test
    void acquiredLockReturnsToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(KEY), anyString(), eq(TTL), eq(TimeUnit.SECONDS)))
                .thenReturn(Boolean.TRUE);

        Optional<String> token = lock.tryLock(KEY, TTL);

        assertTrue(token.isPresent());
        assertFalse(token.get().isEmpty());
    }

    @Test
    void lockHeldByOtherInstanceReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(KEY), anyString(), eq(TTL), eq(TimeUnit.SECONDS)))
                .thenReturn(Boolean.FALSE);

        assertFalse(lock.tryLock(KEY, TTL).isPresent());
    }

    @Test
    void redisFailureSkipsThisRound() {
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        assertFalse(lock.tryLock(KEY, TTL).isPresent());
    }

    @Test
    void blankTokenSkipsRelease() {
        lock.release(KEY, " ");

        verifyNoInteractions(redisTemplate);
    }
}
