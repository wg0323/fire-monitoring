package com.fm.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 告警通知异步投递线程池。
 * device.up 消费端 prefetch=1，短信/微信外部 HTTP 调用若在消费线程内同步执行，
 * 一次挂起就会堵住整条设备上报链路，故把 AFTER_COMMIT 之后的通知交给有界线程池。
 */
@Configuration
public class NotifyExecutorConfig {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 8;
    private static final int QUEUE_CAPACITY = 200;
    private static final long KEEP_ALIVE_SECONDS = 60L;

    @Primary
    @Bean(name = "notifyExecutor", destroyMethod = "shutdown")
    public Executor notifyExecutor() {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger index = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "fm-notify-" + index.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };
        // 队列打满时退化为调用线程执行，宁可短暂变慢也不丢火警通知
        return new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY), factory, new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
