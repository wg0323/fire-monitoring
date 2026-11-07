package com.fm.module.notification.service.impl;

import com.fm.module.danger.entity.Danger;
import com.fm.module.notification.entity.MpNotification;
import com.fm.module.notification.mapper.MpNotificationMapper;
import com.fm.module.notification.vo.SubscribeConfigVO;
import com.fm.module.notification.service.WxSubscribeService;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 隐患站内消息测试：接收人去重、订阅消息触发与模板配置。
 */
class MpNotificationServiceImplTest {

    private final MpNotificationMapper notificationMapper = mock(MpNotificationMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private final WxSubscribeService wxSubscribeService = mock(WxSubscribeService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private MpNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MpNotificationServiceImpl(notificationMapper, userMapper, dataScopeService,
                wxSubscribeService, redisTemplate);
    }

    @Test
    void sendDangerSkipsDuplicateReceivers() {
        SysUser first = user(100L, "李外勤");
        SysUser second = user(100L, "李外勤");
        Danger danger = danger();

        service.sendDanger(danger, Arrays.asList(first, second), "隐患待整改：消防设施");

        verify(notificationMapper, times(1)).insert(any(MpNotification.class));
        verify(wxSubscribeService, times(1)).sendDanger(any(Danger.class), any(SysUser.class), anyString());
    }

    @Test
    void sendDangerIgnoresEmptyReceivers() {
        service.sendDanger(danger(), Collections.emptyList(), "隐患待整改");

        verify(notificationMapper, never()).insert(any(MpNotification.class));
        verify(wxSubscribeService, never()).sendDanger(any(Danger.class), any(SysUser.class), anyString());
    }

    @Test
    void subscribeConfigReflectsTemplates() {
        when(wxSubscribeService.alarmTemplateId()).thenReturn("alarm-tpl");
        when(wxSubscribeService.dangerTemplateId()).thenReturn("danger-tpl");

        SubscribeConfigVO config = service.subscribeConfig();

        assertEquals("alarm-tpl", config.getAlarmTemplateId());
        assertEquals("danger-tpl", config.getDangerTemplateId());
        assertTrue(config.getEnabled());
    }

    @Test
    void subscribeConfigDisabledWithoutTemplates() {
        when(wxSubscribeService.alarmTemplateId()).thenReturn("");
        when(wxSubscribeService.dangerTemplateId()).thenReturn("");

        assertFalse(service.subscribeConfig().getEnabled());
    }

    private SysUser user(Long id, String name) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setTenantId(1L);
        user.setUserName(name);
        user.setRealName(name);
        user.setStatus(1);
        user.setUserType("TENANT");
        return user;
    }

    private Danger danger() {
        Danger danger = new Danger();
        danger.setId(5L);
        danger.setTenantId(1L);
        danger.setDangerType("消防设施");
        danger.setPosition("一楼大厅");
        return danger;
    }
}
