package com.fm.module.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.danger.entity.Danger;
import com.fm.module.device.entity.Merchant;
import com.fm.module.notification.dto.NotificationQueryDTO;
import com.fm.module.notification.vo.MpNotificationVO;
import com.fm.module.notification.vo.SubscribeConfigVO;
import com.fm.module.system.entity.SysUser;

import java.util.Collection;
import java.util.List;

/**
 * 站内通知：报警/隐患生成消息，小程序查看与已读，管理端查询。
 */
public interface MpNotificationService {

    String USER_TYPE_MERCHANT = "MERCHANT";

    void createAlarmNotifications(FireAlarm alarm, Merchant merchant);

    /**
     * 隐患站内消息：按指定接收人写入，接收人为空时不发送。
     */
    void sendDanger(Danger danger, Collection<SysUser> receivers, String content);

    /**
     * 小程序订阅模板配置。
     */
    SubscribeConfigVO subscribeConfig();

    /**
     * 记录当前用户的订阅授权（微信侧仍按一次授权一条控制）。
     */
    void recordSubscribe(List<String> templateIds);

    Page<MpNotificationVO> myPage(long pageNum, long pageSize, Integer type);

    long unreadCount();

    void markRead(Long id);

    Page<MpNotificationVO> adminPage(NotificationQueryDTO query);
}
