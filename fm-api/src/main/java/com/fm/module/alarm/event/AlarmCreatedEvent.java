package com.fm.module.alarm.event;

import com.fm.module.alarm.entity.FireAlarm;

/**
 * 报警入库完成事件：由通知模块监听，避免报警域反向依赖通知域。
 */
public class AlarmCreatedEvent {

    private final FireAlarm alarm;

    public AlarmCreatedEvent(FireAlarm alarm) {
        this.alarm = alarm;
    }

    public FireAlarm getAlarm() {
        return alarm;
    }
}
