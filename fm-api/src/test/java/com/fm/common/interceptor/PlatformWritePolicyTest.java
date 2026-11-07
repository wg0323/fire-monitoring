package com.fm.common.interceptor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平台写权限策略测试。
 */
class PlatformWritePolicyTest {

    @Test
    void platformCannotWriteBusinessData() {
        assertFalse(PlatformWritePolicy.isWritable("POST", "/api/admin/alarm/verify"));
        assertFalse(PlatformWritePolicy.isWritable("PUT", "/api/admin/device/merchant/1"));
        assertFalse(PlatformWritePolicy.isWritable("DELETE", "/api/admin/danger/1"));
        assertFalse(PlatformWritePolicy.isWritable("POST", "/api/mp/danger"));
        assertFalse(PlatformWritePolicy.isWritable("POST", "/api/mp/alarm/verify"));
    }

    @Test
    void platformCanWriteSystemResources() {
        assertTrue(PlatformWritePolicy.isWritable("POST", "/api/admin/system/user"));
        assertTrue(PlatformWritePolicy.isWritable("PUT", "/api/admin/system/menu/1"));
        assertTrue(PlatformWritePolicy.isWritable("POST", "/api/admin/device/dict/alarm-type"));
        assertTrue(PlatformWritePolicy.isWritable("PUT", "/api/admin/auth/password"));
    }

    @Test
    void readRequestsAreAlwaysAllowed() {
        assertTrue(PlatformWritePolicy.isWritable("GET", "/api/admin/alarm/page"));
        assertTrue(PlatformWritePolicy.isWritable("HEAD", "/api/admin/device/merchant/1"));
    }
}
