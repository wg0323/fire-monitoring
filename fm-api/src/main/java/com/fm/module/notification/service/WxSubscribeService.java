package com.fm.module.notification.service;

import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.danger.entity.Danger;
import com.fm.module.device.entity.Merchant;
import com.fm.module.system.entity.SysUser;

/**
 * 小程序订阅消息：向绑定了微信的账号发送报警与隐患通知。
 * 未配置模板或凭据时自动跳过。
 */
public interface WxSubscribeService {

    void sendAlarm(FireAlarm alarm, Merchant merchant);

    String alarmTemplateId();

    /**
     * 隐患通知：模板字段 thing1 类型、thing2 位置、time3 时间、thing4 状态。
     */
    void sendDanger(Danger danger, SysUser receiver, String content);

    String dangerTemplateId();
}
