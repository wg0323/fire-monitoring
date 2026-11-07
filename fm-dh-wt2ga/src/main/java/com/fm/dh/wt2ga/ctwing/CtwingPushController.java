package com.fm.dh.wt2ga.ctwing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.RejectedExecutionException;

/**
 * CTWing 推送入口：只做三件事——按需打印原始报文、提交线程池、立即返回 200。
 * CTWing 不提供签名或 token，应用层没有可校验的凭据，来源控制放在部署侧。
 */
@Slf4j
@RestController
public class CtwingPushController {

    private final PushDispatcher dispatcher;
    private final ThreadPoolTaskExecutor pushExecutor;
    private final boolean rawLogEnabled;

    public CtwingPushController(PushDispatcher dispatcher,
                                ThreadPoolTaskExecutor pushExecutor,
                                @Value("${fm.raw-log-enabled:true}") boolean rawLogEnabled) {
        this.dispatcher = dispatcher;
        this.pushExecutor = pushExecutor;
        this.rawLogEnabled = rawLogEnabled;
    }

    @PostMapping("${ctwing.push.callback-path:/callback}")
    public void receive(@RequestBody String rawBody) {
        if (rawLogEnabled) {
            log.info("received ctwing push: {}", rawBody);
        }
        try {
            pushExecutor.execute(() -> dispatcher.dispatch(rawBody));
        } catch (RejectedExecutionException ex) {
            log.error("ctwing push rejected by executor, rawBody={}", rawBody, ex);
        }
    }
}
