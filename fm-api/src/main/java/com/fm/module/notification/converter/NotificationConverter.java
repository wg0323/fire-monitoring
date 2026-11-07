package com.fm.module.notification.converter;

import com.fm.module.notification.entity.MpNotification;
import com.fm.module.notification.vo.MpNotificationVO;
import com.fm.module.notification.vo.SubscribeConfigVO;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

/**
 * 通知对象转换。
 */
public final class NotificationConverter {

    private NotificationConverter() {
    }

    public static MpNotificationVO toVO(MpNotification item) {
        if (item == null) {
            return null;
        }
        MpNotificationVO vo = new MpNotificationVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    public static SubscribeConfigVO subscribeConfig(String alarmTemplateId, String dangerTemplateId) {
        SubscribeConfigVO vo = new SubscribeConfigVO();
        vo.setAlarmTemplateId(alarmTemplateId);
        vo.setDangerTemplateId(dangerTemplateId);
        vo.setEnabled(StringUtils.hasText(alarmTemplateId) || StringUtils.hasText(dangerTemplateId));
        return vo;
    }
}
