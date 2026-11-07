package com.fm.module.alarm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.dto.CommandCreateDTO;
import com.fm.module.alarm.entity.CommandTask;
import com.fm.module.alarm.mq.DeviceMessage;

/**
 * 下行指令服务：落任务表后经 MQ 下发，回执异步更新状态。
 */
public interface CommandService {

    Long create(CommandCreateDTO request);

    Page<CommandTask> page(Integer pageNum, Integer pageSize, String sn, String status);

    void handleAck(DeviceMessage message);
}
