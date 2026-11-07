package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.dto.MerchantQueryDTO;
import com.fm.module.device.dto.MerchantSaveDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.DictMerchantTypeMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysUserMerchant;
import com.fm.module.system.entity.SysUserRole;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserMerchantMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商户服务测试：删除保护、状态校验、工作区租户校验与商户登录账号开户。
 */
class MerchantServiceImplTest {

    private static final long TENANT_ID = 1L;
    private static final long MERCHANT_ID = 100L;
    private static final long ACCOUNT_ID = 1000L;
    private static final long MERCHANT_ROLE_ID = 9104L;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, Merchant.class);
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, SysWorkArea.class);
        TableInfoHelper.initTableInfo(assistant, SysWorkAreaMerchant.class);
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysUserMerchant.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
    }

    private final MerchantMapper merchantMapper = mock(MerchantMapper.class);
    private final MerchantBuildingMapper buildingMapper = mock(MerchantBuildingMapper.class);
    private final MerchantFloorMapper floorMapper = mock(MerchantFloorMapper.class);
    private final DeviceMapper deviceMapper = mock(DeviceMapper.class);
    private final DictMerchantTypeMapper merchantTypeMapper = mock(DictMerchantTypeMapper.class);
    private final DictDeviceTypeMapper deviceTypeMapper = mock(DictDeviceTypeMapper.class);
    private final SysWorkAreaMapper workAreaMapper = mock(SysWorkAreaMapper.class);
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper = mock(SysWorkAreaMerchantMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserMerchantMapper userMerchantMapper = mock(SysUserMerchantMapper.class);
    private final SysRoleMapper roleMapper = mock(SysRoleMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DataScopeService dataScopeService = mock(DataScopeService.class);
    private MerchantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MerchantServiceImpl(dataScopeService, buildingMapper, floorMapper, deviceMapper,
                merchantTypeMapper, deviceTypeMapper, workAreaMapper, workAreaMerchantMapper, userMapper,
                userMerchantMapper, roleMapper, userRoleMapper, passwordEncoder);
        ReflectionTestUtils.setField(service, "baseMapper", merchantMapper);
        TenantContext.set(TENANT_ID, "TENANT", "TENANT", false);
        when(dataScopeService.isPlatform()).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void deleteRejectsMerchantWithDevices() {
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(TENANT_ID));
        when(deviceMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BizException.class, () -> service.delete(MERCHANT_ID));

        verify(merchantMapper, never()).deleteById(MERCHANT_ID);
        verify(workAreaMerchantMapper, never()).delete(any());
    }

    @Test
    void deleteCleansWorkAreaRelations() {
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(TENANT_ID));
        when(deviceMapper.selectCount(any())).thenReturn(0L);

        service.delete(MERCHANT_ID);

        verify(workAreaMerchantMapper).delete(any());
        verify(merchantMapper).deleteById(MERCHANT_ID);
    }

    @Test
    void updateStatusRejectsInvalidValue() {
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(TENANT_ID));

        assertThrows(BizException.class, () -> service.updateStatus(MERCHANT_ID, 2));

        verify(merchantMapper, never()).update(any(), any());
    }

    @Test
    void bindWorkAreasRejectsCrossTenantArea() {
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(TENANT_ID));
        SysWorkArea otherTenantArea = new SysWorkArea();
        otherTenantArea.setId(9L);
        otherTenantArea.setTenantId(2L);
        when(workAreaMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(otherTenantArea));

        assertThrows(BizException.class, () -> service.bindWorkAreas(MERCHANT_ID, Arrays.asList(9L)));

        verify(workAreaMerchantMapper, never()).delete(any());
    }

    @Test
    void pageWithEmptyWorkAreaReturnsEmptyPage() {
        when(workAreaMerchantMapper.selectList(any())).thenReturn(Collections.emptyList());
        MerchantQueryDTO query = new MerchantQueryDTO();
        query.setWorkAreaId(9L);

        assertEquals(0L, service.page(query).getTotal());
        verify(merchantMapper, never()).selectPage(any(), any());
    }

    @Test
    void createMerchantOpensContactAndOwnerAccounts() {
        mockNewMerchant(merchantRequest("13900000001", "张三"));

        service.create(merchantRequest("13900000001", "张三"));

        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper, times(2)).insert(userCaptor.capture());
        List<SysUser> accounts = userCaptor.getAllValues();

        SysUser contact = accounts.stream()
                .filter(user -> ("merchant_" + MERCHANT_ID).equals(user.getUserName()))
                .findFirst().orElseThrow(IllegalStateException::new);
        assertEquals("MERCHANT", contact.getUserType());
        assertEquals(TENANT_ID, contact.getTenantId());
        assertEquals(MERCHANT_ID, contact.getMerchantId());
        assertEquals("13900000001", contact.getPhone());
        assertEquals("张三", contact.getRealName());
        assertEquals(1, contact.getStatus());

        SysUser owner = accounts.stream()
                .filter(user -> ("owner_" + MERCHANT_ID).equals(user.getUserName()))
                .findFirst().orElseThrow(IllegalStateException::new);
        assertEquals("13900000000", owner.getPhone());
        assertEquals("测试商户", owner.getRealName());

        verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    @Test
    void createMerchantSkipsAccountWhenPhoneInvalid() {
        MerchantSaveDTO request = merchantRequest("无须作答", "张三");
        request.setPhone("0895-1234567");
        mockNewMerchant(request);

        service.create(request);

        verify(userMapper, never()).insert(any(SysUser.class));
        verify(userRoleMapper, never()).insert(any(SysUserRole.class));
    }

    @Test
    void createMerchantAttachesExistingAccountForSamePhone() {
        mockNewMerchant(merchantRequest("13900000002", "张三"));
        SysUser holder = new SysUser();
        holder.setId(ACCOUNT_ID);
        holder.setTenantId(TENANT_ID);
        holder.setMerchantId(200L);
        holder.setUserType("MERCHANT");
        holder.setStatus(1);
        holder.setUserName("merchant_200");
        when(userMapper.selectByPhone("13900000002")).thenReturn(Collections.singletonList(holder));

        service.create(merchantRequest("13900000002", "张三"));

        // 第一联系人复用已有账号，仅新建业主账号
        verify(userMapper, times(1)).insert(any(SysUser.class));
        verify(userMerchantMapper, times(2)).insert(any(SysUserMerchant.class));
    }

    @Test
    void createMerchantSkipsAccountWhenPhoneUsedByDeletedAccount() {
        MerchantSaveDTO request = merchantRequest("13900000003", "张三");
        request.setPhone("0895-1234567");
        mockNewMerchant(request);
        SysUser deleted = new SysUser();
        deleted.setId(9L);
        when(userMapper.selectByPhoneIncludeDeleted("13900000003"))
                .thenReturn(Collections.singletonList(deleted));

        service.create(request);

        verify(userMapper, never()).insert(any(SysUser.class));
        verify(userMerchantMapper, never()).insert(any(SysUserMerchant.class));
    }

    @Test
    void updateStatusSyncsMerchantAccountStatus() {
        when(merchantMapper.selectById(MERCHANT_ID)).thenReturn(merchant(TENANT_ID));
        SysUser contact = new SysUser();
        contact.setId(ACCOUNT_ID);
        contact.setMerchantId(MERCHANT_ID);
        contact.setUserType("MERCHANT");
        contact.setUserName("merchant_" + MERCHANT_ID);
        SysUser owner = new SysUser();
        owner.setId(ACCOUNT_ID + 1);
        owner.setMerchantId(MERCHANT_ID);
        owner.setUserType("MERCHANT");
        owner.setUserName("owner_" + MERCHANT_ID);
        when(userMapper.selectList(any())).thenReturn(Arrays.asList(contact, owner));

        service.updateStatus(MERCHANT_ID, 0);

        verify(userMapper, times(2)).update(any(), any());
    }

    private void mockNewMerchant(MerchantSaveDTO request) {
        when(merchantTypeMapper.selectCount(any())).thenReturn(1L);
        when(merchantMapper.insert(any(Merchant.class))).thenAnswer(invocation -> {
            Merchant inserted = invocation.getArgument(0);
            inserted.setId(MERCHANT_ID);
            return 1;
        });
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.countByUserNameIncludeDeleted(anyString())).thenReturn(0L);
        when(userMapper.selectByPhone(request.getPhone1())).thenReturn(Collections.emptyList());
        when(userMapper.selectByPhoneIncludeDeleted(request.getPhone1())).thenReturn(Collections.emptyList());
        when(userMapper.selectByPhone(request.getPhone())).thenReturn(Collections.emptyList());
        when(userMapper.selectByPhoneIncludeDeleted(request.getPhone())).thenReturn(Collections.emptyList());
        when(userMerchantMapper.selectCount(any())).thenReturn(0L);
        SysRole merchantRole = new SysRole();
        merchantRole.setId(MERCHANT_ROLE_ID);
        merchantRole.setTenantId(TENANT_ID);
        merchantRole.setRoleKey("MERCHANT");
        when(roleMapper.selectOne(any())).thenReturn(merchantRole);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser inserted = invocation.getArgument(0);
            inserted.setId(ACCOUNT_ID);
            return 1;
        });
    }

    private MerchantSaveDTO merchantRequest(String phone1, String contact1) {
        MerchantSaveDTO request = new MerchantSaveDTO();
        request.setName("测试商户");
        request.setType("type1");
        request.setMerchantKind(1);
        request.setPhone("13900000000");
        request.setAddr("测试地址");
        request.setLng(97.1);
        request.setLat(31.1);
        request.setPhone1(phone1);
        request.setContact1(contact1);
        request.setStatus(1);
        return request;
    }

    private Merchant merchant(Long tenantId) {
        Merchant merchant = new Merchant();
        merchant.setId(MERCHANT_ID);
        merchant.setTenantId(tenantId);
        merchant.setName("测试商户");
        merchant.setStatus(1);
        return merchant;
    }
}
