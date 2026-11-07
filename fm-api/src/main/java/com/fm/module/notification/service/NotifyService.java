package com.fm.module.notification.service;

import com.fm.module.alarm.entity.FireAlarm;

/**
 * 报警通知编排：按渠道逐个尝试，单个渠道失败不影响其他渠道与报警落库。
 */
public interface NotifyService {

    void onAlarm(FireAlarm alarm);
}
