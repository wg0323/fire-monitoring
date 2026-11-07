package com.fm.module.notification.service;

/**
 * 短信通道：聚合数据单实现，未配置 key 时自动禁用。
 */
public interface SmsService {

    boolean sendAlarm(String phone, String alarmTime, String alarmType, String alarmAddress);
}
