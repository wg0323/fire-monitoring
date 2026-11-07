package com.fm.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fm.common.constant.TimeZones;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.dto.CommandCreateDTO;
import com.fm.module.alarm.entity.CommandTask;
import com.fm.module.alarm.mapper.CommandTaskMapper;
import com.fm.module.alarm.mq.CommandAckPayload;
import com.fm.module.alarm.mq.CommandCorrelationData;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.alarm.mq.MqConstants;
import com.fm.module.alarm.service.CommandService;
import com.fm.module.device.entity.Device;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 下行指令服务实现：先落任务表再发 MQ，发布确认驱动 SENDING -> SENT，回执按任务 id 更新状态。
 * 同时承担 RabbitTemplate 的确认/退回回调，把投递结果与任务状态机关联起来。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandServiceImpl implements CommandService,
        RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    /** 已实现下行指令监听的采集器设备类型 */
    private static final Set<String> DOWNLINK_DEVICE_TYPES = new HashSet<>(Arrays.asList(
            "dh-cms12gb", "dh-wsm20gb"
    ));

    private static final String COMMAND_ROUTING_KEY_PREFIX = "command.";
    private static final String SEND_FAIL_RESULT = "指令下发失败";
    private static final String PUBLISH_FAIL_RESULT = "指令投递未确认，等待补偿重试";

    private final CommandTaskMapper taskMapper;
    private final DeviceMapper deviceMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final DataScopeService dataScopeService;

    @Override
    @Transactional
    public Long create(CommandCreateDTO request) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getSn, request.getSn()));
        if (device == null) {
            throw new BizException("设备不存在");
        }
        if (!DOWNLINK_DEVICE_TYPES.contains(device.getType())) {
            throw new BizException("该设备类型不支持指令下发");
        }
        CommandTask task = new CommandTask();
        task.setTenantId(device.getTenantId());
        task.setSn(request.getSn());
        task.setCommandType(request.getCommandType());
        task.setPayload(request.getParams());
        task.setStatus(CommandTask.STATUS_PENDING);
        task.setReqTime(LocalDateTime.now());
        task.setRetryCount(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 事务内先置投递中间态；只有 broker confirm ack 后才允许流转为 SENT
        taskMapper.update(null, new LambdaUpdateWrapper<CommandTask>()
                .eq(CommandTask::getId, task.getId())
                .set(CommandTask::getStatus, MqConstants.COMMAND_STATUS_SENDING)
                .set(CommandTask::getUpdateTime, LocalDateTime.now()));
        DeviceMessage message = buildMessage(task.getId(), device, request);
        Long taskId = task.getId();
        afterCommit(() -> dispatch(taskId, device, message));
        return taskId;
    }

    /**
     * 事务提交后再发 MQ，避免回执先于提交导致状态更新不到。
     */
    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void dispatch(Long taskId, Device device, DeviceMessage message) {
        try {
            send(taskId, device, message);
        } catch (RuntimeException ex) {
            log.error("send command failed, commandId={}", taskId, ex);
            updateStatus(() -> taskMapper.update(null, new LambdaUpdateWrapper<CommandTask>()
                    .eq(CommandTask::getId, taskId)
                    .set(CommandTask::getStatus, CommandTask.STATUS_FAIL)
                    .set(CommandTask::getResult, SEND_FAIL_RESULT)
                    .set(CommandTask::getUpdateTime, LocalDateTime.now())));
        }
    }

    @Override
    public Page<CommandTask> page(Integer pageNum, Integer pageSize, String sn, String status) {
        LambdaQueryWrapper<CommandTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sn)) {
            wrapper.eq(CommandTask::getSn, sn);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(CommandTask::getStatus, status);
        }
        wrapper.orderByDesc(CommandTask::getReqTime);
        Page<CommandTask> page = new Page<>(pageNum == null ? 1 : pageNum,
                pageSize == null ? 10 : pageSize);
        if (!dataScopeService.isPlatform()) {
            return taskMapper.selectPage(page, wrapper);
        }
        List<Long> tenantIds = dataScopeService.authorizedTenantIds();
        if (tenantIds.isEmpty()) {
            return page;
        }
        wrapper.in(CommandTask::getTenantId, tenantIds);
        return dataScopeService.withIgnoreTenant(() -> taskMapper.selectPage(page, wrapper));
    }

    @Override
    public void handleAck(DeviceMessage message) {
        CommandAckPayload payload = objectMapper.convertValue(message.getPayload(), CommandAckPayload.class);
        if (payload == null || !StringUtils.hasText(payload.getCommandId())) {
            log.warn("command ack without commandId, messageId={}", message.getMessageId());
            return;
        }
        Long taskId = parseId(payload.getCommandId());
        if (taskId == null) {
            log.warn("command ack with invalid commandId={}", payload.getCommandId());
            return;
        }
        String status = StringUtils.hasText(payload.getStatus())
                ? payload.getStatus() : CommandTask.STATUS_FAIL;
        // SENDING 也允许收口：采集器回执可能早于 broker confirm 到达
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<CommandTask>()
                .eq(CommandTask::getId, taskId)
                .in(CommandTask::getStatus, CommandTask.STATUS_PENDING,
                        MqConstants.COMMAND_STATUS_SENDING, CommandTask.STATUS_SENT)
                .set(CommandTask::getStatus, status)
                .set(CommandTask::getResult, payload.getResult())
                .set(CommandTask::getRespTime, toTime(payload.getRespTime()))
                .set(CommandTask::getUpdateTime, LocalDateTime.now()));
        if (updated == 0) {
            log.warn("command ack matched no sent task, commandId={}", payload.getCommandId());
        }
    }

    /**
     * 发布确认：ack 由 SENDING 流转 SENT；nack 触发有限重投，超限转失败等待补偿。
     * 不可路由消息会先走 returnedMessage 置为 FAIL，本方法按状态条件更新，不会把它改回 SENT。
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (!(correlationData instanceof CommandCorrelationData)) {
            if (!ack) {
                log.error("command publish nack without correlation context, cause={}", cause);
            }
            return;
        }
        CommandCorrelationData data = (CommandCorrelationData) correlationData;
        if (!ack) {
            log.error("command publish nack, commandId={}, exchange={}, routingKey={}, "
                            + "payloadLength={}, payloadDigest={}, cause={}",
                    data.getCommandId(), data.getExchange(), data.getRoutingKey(),
                    data.getPayloadLength(), data.getPayloadDigest(), cause);
            retryOrFail(data);
            return;
        }
        markSent(data.getTaskId(), data.getCommandId());
    }

    /**
     * 消息到达交换机但无匹配队列：mandatory=true 下由 broker 退回，必须视为投递失败。
     */
    @Override
    public void returnedMessage(ReturnedMessage returned) {
        MessageProperties properties = returned.getMessage().getMessageProperties();
        String commandId = StringUtils.hasText(properties.getCorrelationId())
                ? properties.getCorrelationId() : properties.getMessageId();
        byte[] body = returned.getMessage().getBody();
        log.error("command message returned, commandId={}, exchange={}, routingKey={}, "
                        + "replyCode={}, replyText={}, bodyLength={}",
                commandId, returned.getExchange(), returned.getRoutingKey(),
                returned.getReplyCode(), returned.getReplyText(), body == null ? 0 : body.length);
        Long taskId = parseId(commandId);
        if (taskId != null) {
            markPublishFailed(taskId, commandId);
        }
    }

    /**
     * 有限重投入口：复用 CorrelationData 累计次数，避免引入新依赖；
     * 重投会重复投递，消费端按 commandId 幂等（见 handleAck 的状态条件更新）。
     */
    private void retryOrFail(CommandCorrelationData data) {
        int retry = data.getRetryCount().incrementAndGet();
        if (retry > CommandCorrelationData.MAX_RETRY || data.getMessage() == null) {
            log.error("command publish failed after {} retries, commandId={}, exchange={}, routingKey={}",
                    CommandCorrelationData.MAX_RETRY, data.getCommandId(),
                    data.getExchange(), data.getRoutingKey());
            markPublishFailed(data.getTaskId(), data.getCommandId());
            return;
        }
        log.warn("command publish retry {}/{}, commandId={}, exchange={}, routingKey={}",
                retry, CommandCorrelationData.MAX_RETRY, data.getCommandId(),
                data.getExchange(), data.getRoutingKey());
        try {
            rabbitTemplate.send(data.getExchange(), data.getRoutingKey(), data.getMessage(), data);
        } catch (RuntimeException ex) {
            log.error("command publish resend failed, commandId={}, exchange={}, routingKey={}",
                    data.getCommandId(), data.getExchange(), data.getRoutingKey(), ex);
            markPublishFailed(data.getTaskId(), data.getCommandId());
        }
    }

    private void markSent(Long taskId, String commandId) {
        updateStatus(() -> {
            int updated = taskMapper.update(null, new LambdaUpdateWrapper<CommandTask>()
                    .eq(CommandTask::getId, taskId)
                    .eq(CommandTask::getStatus, MqConstants.COMMAND_STATUS_SENDING)
                    .set(CommandTask::getStatus, CommandTask.STATUS_SENT)
                    .set(CommandTask::getUpdateTime, LocalDateTime.now()));
            if (updated == 0) {
                log.debug("command task no longer SENDING on publish confirm, commandId={}", commandId);
            }
        });
    }

    private void markPublishFailed(Long taskId, String commandId) {
        updateStatus(() -> {
            int updated = taskMapper.update(null, new LambdaUpdateWrapper<CommandTask>()
                    .eq(CommandTask::getId, taskId)
                    .in(CommandTask::getStatus, MqConstants.COMMAND_STATUS_SENDING, CommandTask.STATUS_SENT)
                    .set(CommandTask::getStatus, CommandTask.STATUS_FAIL)
                    .set(CommandTask::getResult, PUBLISH_FAIL_RESULT)
                    .set(CommandTask::getUpdateTime, LocalDateTime.now()));
            if (updated == 0) {
                log.debug("command task not in publishable state on failure, commandId={}", commandId);
            }
        });
    }

    /**
     * 确认回调运行在 rabbit client 线程，无登录租户上下文；按主键更新时临时关闭租户过滤，
     * 否则会被 tenant_id=0 条件挡住。
     */
    private void updateStatus(Runnable action) {
        TenantContext.set(null, "MQ", "ALL", true);
        try {
            action.run();
        } finally {
            TenantContext.clear();
        }
    }

    private DeviceMessage buildMessage(Long taskId, Device device, CommandCreateDTO request) {
        String commandId = String.valueOf(taskId);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("commandId", commandId);
        payload.put("commandType", request.getCommandType());
        if (StringUtils.hasText(request.getParams())) {
            try {
                payload.set("params", objectMapper.readTree(request.getParams()));
            } catch (JsonProcessingException ex) {
                throw new BizException("指令参数不是合法 JSON");
            }
        }
        if (request.getTimeoutSeconds() != null) {
            payload.put("timeoutSeconds", request.getTimeoutSeconds());
        }

        DeviceMessage message = new DeviceMessage();
        message.setMessageId(commandId);
        message.setSn(request.getSn());
        message.setTenantId(device.getTenantId());
        message.setType(MqConstants.TYPE_COMMAND_DOWN);
        message.setEventTime(System.currentTimeMillis());
        message.setVersion(1);
        message.setPayload(payload);
        return message;
    }

    private void send(Long taskId, Device device, DeviceMessage message) {
        String commandId = message.getMessageId();
        String routingKey = COMMAND_ROUTING_KEY_PREFIX + device.getType();
        try {
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            properties.setMessageId(commandId);
            properties.setCorrelationId(commandId);
            Message amqpMessage = new Message(objectMapper.writeValueAsBytes(message), properties);
            CommandCorrelationData correlationData = new CommandCorrelationData(
                    taskId, MqConstants.EXCHANGE_COMMAND_DOWN, routingKey, amqpMessage);
            rabbitTemplate.send(MqConstants.EXCHANGE_COMMAND_DOWN, routingKey,
                    amqpMessage, correlationData);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize command failed, sn=" + device.getSn(), ex);
        }
    }

    private Long parseId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime toTime(Long epochMillis) {
        if (epochMillis == null) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), TimeZones.BUSINESS);
    }
}
