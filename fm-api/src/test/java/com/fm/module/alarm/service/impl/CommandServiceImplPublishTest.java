package com.fm.module.alarm.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.mapper.CommandTaskMapper;
import com.fm.module.alarm.mq.CommandCorrelationData;
import com.fm.module.alarm.mq.MqConstants;
import com.fm.module.device.mapper.DeviceMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.module.alarm.entity.CommandTask;
import com.fm.module.system.service.DataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 发布确认驱动的状态机测试：ack 置 SENT、nack 有限重投、超限/退回置失败。
 */
class CommandServiceImplPublishTest {

    private static final String EXCHANGE = MqConstants.EXCHANGE_COMMAND_DOWN;
    private static final String ROUTING_KEY = "command.test";
    private static final Long TASK_ID = 123L;

    private final CommandTaskMapper taskMapper = mock(CommandTaskMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final CommandServiceImpl service = new CommandServiceImpl(
            taskMapper, deviceMapper, rabbitTemplate, objectMapper, dataScopeService);

    @BeforeEach
    void initMybatisPlusLambdaCache() {
        // 纯 Mockito 环境没有 MyBatis 启动过程，LambdaUpdateWrapper 需要先注册实体元数据
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), CommandTask.class);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void ackUpdatesTaskStatusAndClearsTenantContext() {
        service.confirm(correlationData(), true, null);

        verify(taskMapper).update(isNull(), any());
        assertNull(TenantContext.getDataScope(), "回调线程不得残留租户上下文");
    }

    @Test
    void nackResendsWithSameCorrelationData() {
        CommandCorrelationData data = correlationData();

        service.confirm(data, false, "broker nack");

        verify(rabbitTemplate).send(eq(EXCHANGE), eq(ROUTING_KEY), eq(data.getMessage()), eq(data));
    }

    @Test
    void nackBeyondMaxRetryMarksTaskFailedWithoutResend() {
        CommandCorrelationData data = correlationData();
        data.getRetryCount().set(CommandCorrelationData.MAX_RETRY);

        service.confirm(data, false, "broker nack");

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(), any());
        verify(taskMapper).update(isNull(), any());
    }

    @Test
    void returnedMessageMarksTaskFailed() {
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        message.getMessageProperties().setCorrelationId(String.valueOf(TASK_ID));
        ReturnedMessage returned = new ReturnedMessage(message, 312, "NO_ROUTE", EXCHANGE, ROUTING_KEY);

        service.returnedMessage(returned);

        verify(taskMapper).update(isNull(), any());
        assertNull(TenantContext.getDataScope());
    }

    private CommandCorrelationData correlationData() {
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        return new CommandCorrelationData(TASK_ID, EXCHANGE, ROUTING_KEY, message);
    }
}
