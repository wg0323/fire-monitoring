package com.fm.dh.wt2ga.ctwing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.RejectedExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 推送入口测试：无论报文是否可解析、线程池是否拒绝，HTTP 都必须立即返回 200。
 */
@ExtendWith(MockitoExtension.class)
class CtwingPushControllerTest {

    private static final String FIXTURE = "{\"timestamp\":1791138556229,\"tenantId\":\"2000509630\","
            + "\"serviceId\":\"temperature_state\",\"protocol\":\"mqtt\",\"productId\":\"17321036\","
            + "\"messageType\":\"eventReport\",\"eventType\":2,"
            + "\"eventContent\":{\"temperature_value\":40.6,\"temperature_state\":1,\"serviceid\":1004},"
            + "\"deviceSn\":\"864852062800536\",\"deviceId\":\"99014977864852062800536\"}";

    @Mock
    private PushDispatcher dispatcher;

    private ThreadPoolTaskExecutor executor;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("ctwing-push-test-");
        executor.initialize();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CtwingPushController(dispatcher, executor, true))
                .build();
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void returns_200_and_hands_off_to_executor() throws Exception {
        mockMvc.perform(post("/callback").contentType(MediaType.APPLICATION_JSON).content(FIXTURE))
                .andExpect(status().isOk());

        verify(dispatcher, timeout(1000)).dispatch(FIXTURE);
    }

    @Test
    void malformed_body_returns_200_and_does_not_throw() throws Exception {
        mockMvc.perform(post("/callback").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isOk());
    }

    @Test
    void rejected_execution_still_returns_200() throws Exception {
        ThreadPoolTaskExecutor rejecting = mock(ThreadPoolTaskExecutor.class);
        doThrow(new RejectedExecutionException("full")).when(rejecting).execute(any(Runnable.class));
        MockMvc rejectingMvc = MockMvcBuilders
                .standaloneSetup(new CtwingPushController(dispatcher, rejecting, false))
                .build();

        rejectingMvc.perform(post("/callback").contentType(MediaType.APPLICATION_JSON).content(FIXTURE))
                .andExpect(status().isOk());
    }
}
