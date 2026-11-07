package com.fm.module.miniapp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.exception.BizException;
import com.fm.module.miniapp.dto.PhoneLoginDTO;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.service.AuthService;
import com.fm.module.system.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 手机号白名单登录测试：未开通、重复、禁用与角色集合。
 */
class WxMiniappServiceImplPhoneTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final AuthService authService = mock(AuthService.class);
    private WxMiniappServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WxMiniappServiceImpl(new RestTemplateBuilder(), new ObjectMapper(),
                userMapper, authService);
        ReflectionTestUtils.setField(service, "mockLoginEnabled", true);
    }

    @Test
    void unregisteredPhoneIsRejected() {
        when(userMapper.selectByPhone("13800009999")).thenReturn(Collections.emptyList());
        BizException ex = org.junit.jupiter.api.Assertions.assertThrows(BizException.class,
                () -> service.phoneLogin(request("13800009999"), "127.0.0.1"));
        assertEquals(401, ex.getCode());
        verify(authService, never()).loginByUserId(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void duplicatePhoneIsRejected() {
        when(userMapper.selectByPhone("13800009999")).thenReturn(Arrays.asList(
                user(1L, "TENANT"), user(2L, "TENANT")));
        BizException ex = org.junit.jupiter.api.Assertions.assertThrows(BizException.class,
                () -> service.phoneLogin(request("13800009999"), "127.0.0.1"));
        assertEquals(401, ex.getCode());
        verify(authService, never()).loginByUserId(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void disabledUserIsRejected() {
        SysUser user = user(1L, "TENANT");
        user.setStatus(0);
        when(userMapper.selectByPhone("13800009999")).thenReturn(Collections.singletonList(user));
        BizException ex = org.junit.jupiter.api.Assertions.assertThrows(BizException.class,
                () -> service.phoneLogin(request("13800009999"), "127.0.0.1"));
        assertEquals(403, ex.getCode());
    }

    @Test
    void platformUserCannotLoginMiniapp() {
        when(userMapper.selectByPhone("13800009999")).thenReturn(Collections.singletonList(
                user(1L, "PLATFORM")));
        BizException ex = org.junit.jupiter.api.Assertions.assertThrows(BizException.class,
                () -> service.phoneLogin(request("13800009999"), "127.0.0.1"));
        assertEquals(403, ex.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void fieldUserLogsInWithMiniappRoles() {
        when(userMapper.selectByPhone("13800009999")).thenReturn(Collections.singletonList(
                user(1L, "TENANT")));
        when(authService.loginByUserId(anyLong(), anyString(), eq("app"), any(Set.class)))
                .thenReturn(new LoginVO());

        service.phoneLogin(request("13800009999"), "127.0.0.1");

        ArgumentCaptor<Set<String>> captor = ArgumentCaptor.forClass(Set.class);
        verify(authService).loginByUserId(eq(1L), eq("127.0.0.1"), eq("app"), captor.capture());
        Set<String> roles = captor.getValue();
        assertTrue(roles.contains(SysRole.TENANT_FIELD));
        assertTrue(roles.contains(SysRole.TENANT_ADMIN));
        assertTrue(roles.contains(SysRole.MERCHANT));
        assertFalse(roles.contains(SysRole.TENANT_DUTY));
        assertFalse(roles.contains(SysRole.PLATFORM_ADMIN));
    }

    private PhoneLoginDTO request(String phone) {
        PhoneLoginDTO request = new PhoneLoginDTO();
        request.setPhone(phone);
        return request;
    }

    private SysUser user(Long id, String userType) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setTenantId(1L);
        user.setUserType(userType);
        user.setStatus(1);
        user.setUserName("u" + id);
        return user;
    }
}
