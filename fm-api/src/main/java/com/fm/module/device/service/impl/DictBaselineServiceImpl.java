package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.entity.DictAlarmType;
import com.fm.module.device.entity.DictDangerType;
import com.fm.module.device.entity.DictDeviceMaker;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.DictMerchantType;
import com.fm.module.device.mapper.DictAlarmTypeMapper;
import com.fm.module.device.mapper.DictDangerTypeMapper;
import com.fm.module.device.mapper.DictDeviceMakerMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.DictMerchantTypeMapper;
import com.fm.module.device.service.DictBaselineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

/**
 * 租户字典基线：与菜单基线同为「复制后脱钩」，平台后续新增的字典不会自动同步到已有租户。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictBaselineServiceImpl implements DictBaselineService {

    private static final long PLATFORM_TENANT_ID = 0L;

    private final DictDeviceTypeMapper deviceTypeMapper;
    private final DictDeviceMakerMapper deviceMakerMapper;
    private final DictAlarmTypeMapper alarmTypeMapper;
    private final DictDangerTypeMapper dangerTypeMapper;
    private final DictMerchantTypeMapper merchantTypeMapper;

    @Override
    @Transactional
    public int provisionTenantDict(Long tenantId) {
        if (tenantId == null || tenantId == PLATFORM_TENANT_ID) {
            throw new BizException("租户 id 不合法");
        }
        return withIgnoreTenant(() -> {
            int copied = copyDeviceTypes(tenantId) + copyDeviceMakers(tenantId)
                    + copyAlarmTypes(tenantId) + copyDangerTypes(tenantId)
                    + copyMerchantTypes(tenantId);
            log.info("tenant {} dict provisioned, copied={}", tenantId, copied);
            return copied;
        });
    }

    @Override
    @Transactional
    public void removeTenantDict(Long tenantId) {
        if (tenantId == null || tenantId == PLATFORM_TENANT_ID) {
            return;
        }
        withIgnoreTenant(() -> {
            deviceTypeMapper.delete(new LambdaQueryWrapper<DictDeviceType>()
                    .eq(DictDeviceType::getTenantId, tenantId));
            deviceMakerMapper.delete(new LambdaQueryWrapper<DictDeviceMaker>()
                    .eq(DictDeviceMaker::getTenantId, tenantId));
            alarmTypeMapper.delete(new LambdaQueryWrapper<DictAlarmType>()
                    .eq(DictAlarmType::getTenantId, tenantId));
            dangerTypeMapper.delete(new LambdaQueryWrapper<DictDangerType>()
                    .eq(DictDangerType::getTenantId, tenantId));
            merchantTypeMapper.delete(new LambdaQueryWrapper<DictMerchantType>()
                    .eq(DictMerchantType::getTenantId, tenantId));
            return null;
        });
    }

    private int copyDeviceTypes(Long tenantId) {
        Long exists = deviceTypeMapper.selectCount(new LambdaQueryWrapper<DictDeviceType>()
                .eq(DictDeviceType::getTenantId, tenantId));
        if (exists != null && exists > 0) {
            return 0;
        }
        List<DictDeviceType> baseline = deviceTypeMapper.selectList(new LambdaQueryWrapper<DictDeviceType>()
                .eq(DictDeviceType::getTenantId, PLATFORM_TENANT_ID));
        for (DictDeviceType item : baseline) {
            DictDeviceType copy = new DictDeviceType();
            BeanUtils.copyProperties(item, copy);
            copy.setId(IdWorker.getId());
            copy.setTenantId(tenantId);
            deviceTypeMapper.insert(copy);
        }
        return baseline.size();
    }

    private int copyDeviceMakers(Long tenantId) {
        Long exists = deviceMakerMapper.selectCount(new LambdaQueryWrapper<DictDeviceMaker>()
                .eq(DictDeviceMaker::getTenantId, tenantId));
        if (exists != null && exists > 0) {
            return 0;
        }
        List<DictDeviceMaker> baseline = deviceMakerMapper.selectList(new LambdaQueryWrapper<DictDeviceMaker>()
                .eq(DictDeviceMaker::getTenantId, PLATFORM_TENANT_ID));
        for (DictDeviceMaker item : baseline) {
            DictDeviceMaker copy = new DictDeviceMaker();
            BeanUtils.copyProperties(item, copy);
            copy.setId(IdWorker.getId());
            copy.setTenantId(tenantId);
            deviceMakerMapper.insert(copy);
        }
        return baseline.size();
    }

    private int copyAlarmTypes(Long tenantId) {
        Long exists = alarmTypeMapper.selectCount(new LambdaQueryWrapper<DictAlarmType>()
                .eq(DictAlarmType::getTenantId, tenantId));
        if (exists != null && exists > 0) {
            return 0;
        }
        List<DictAlarmType> baseline = alarmTypeMapper.selectList(new LambdaQueryWrapper<DictAlarmType>()
                .eq(DictAlarmType::getTenantId, PLATFORM_TENANT_ID));
        for (DictAlarmType item : baseline) {
            DictAlarmType copy = new DictAlarmType();
            BeanUtils.copyProperties(item, copy);
            copy.setId(IdWorker.getId());
            copy.setTenantId(tenantId);
            alarmTypeMapper.insert(copy);
        }
        return baseline.size();
    }

    private int copyDangerTypes(Long tenantId) {
        Long exists = dangerTypeMapper.selectCount(new LambdaQueryWrapper<DictDangerType>()
                .eq(DictDangerType::getTenantId, tenantId));
        if (exists != null && exists > 0) {
            return 0;
        }
        List<DictDangerType> baseline = dangerTypeMapper.selectList(new LambdaQueryWrapper<DictDangerType>()
                .eq(DictDangerType::getTenantId, PLATFORM_TENANT_ID));
        for (DictDangerType item : baseline) {
            DictDangerType copy = new DictDangerType();
            BeanUtils.copyProperties(item, copy);
            copy.setId(IdWorker.getId());
            copy.setTenantId(tenantId);
            dangerTypeMapper.insert(copy);
        }
        return baseline.size();
    }

    private int copyMerchantTypes(Long tenantId) {
        Long exists = merchantTypeMapper.selectCount(new LambdaQueryWrapper<DictMerchantType>()
                .eq(DictMerchantType::getTenantId, tenantId));
        if (exists != null && exists > 0) {
            return 0;
        }
        List<DictMerchantType> baseline = merchantTypeMapper.selectList(new LambdaQueryWrapper<DictMerchantType>()
                .eq(DictMerchantType::getTenantId, PLATFORM_TENANT_ID));
        for (DictMerchantType item : baseline) {
            DictMerchantType copy = new DictMerchantType();
            BeanUtils.copyProperties(item, copy);
            copy.setId(IdWorker.getId());
            copy.setTenantId(tenantId);
            merchantTypeMapper.insert(copy);
        }
        return baseline.size();
    }

    private <T> T withIgnoreTenant(Supplier<T> action) {
        boolean previous = TenantContext.isIgnoreTenant();
        TenantContext.setIgnoreTenant(true);
        try {
            return action.get();
        } finally {
            TenantContext.setIgnoreTenant(previous);
        }
    }
}
