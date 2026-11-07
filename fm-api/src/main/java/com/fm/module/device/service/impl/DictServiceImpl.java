package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.device.converter.DictConverter;
import com.fm.module.device.dto.DictItemDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.DictAdcode;
import com.fm.module.device.entity.DictAlarmType;
import com.fm.module.device.entity.DictDangerType;
import com.fm.module.device.entity.DictDeviceMaker;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.DictMerchantType;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DictAdcodeMapper;
import com.fm.module.device.mapper.DictAlarmTypeMapper;
import com.fm.module.device.mapper.DictDangerTypeMapper;
import com.fm.module.device.mapper.DictDeviceMakerMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.DictMerchantTypeMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
import com.fm.module.device.vo.DictItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 字典查询与维护。tenant_id=0 为平台基线，其余为各租户字典。
 */
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private static final String TYPE_DEVICE_TYPE = "device-type";
    private static final String TYPE_DEVICE_MAKER = "device-maker";
    private static final String TYPE_ALARM_TYPE = "alarm-type";
    private static final String TYPE_DANGER_TYPE = "danger-type";
    private static final String TYPE_MERCHANT_TYPE = "merchant-type";
    private static final long PLATFORM_TENANT_ID = 0L;

    private final DictDeviceTypeMapper deviceTypeMapper;
    private final DictDeviceMakerMapper deviceMakerMapper;
    private final DictAlarmTypeMapper alarmTypeMapper;
    private final DictDangerTypeMapper dangerTypeMapper;
    private final DictMerchantTypeMapper merchantTypeMapper;
    private final DictAdcodeMapper adcodeMapper;
    private final DeviceMapper deviceMapper;
    private final MerchantMapper merchantMapper;
    private final FireAlarmMapper fireAlarmMapper;
    private final DangerMapper dangerMapper;

    @Override
    public List<DictDeviceType> deviceTypes() {
        return withTenantFilter(() -> deviceTypeMapper.selectList(new LambdaQueryWrapper<DictDeviceType>()
                .eq(DictDeviceType::getStatus, 1)
                .orderByAsc(DictDeviceType::getSort)
                .orderByAsc(DictDeviceType::getId)));
    }

    @Override
    public Map<String, String> deviceTypeNames(Collection<String> codes) {
        Set<String> validCodes = validCodes(codes);
        if (validCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return queryDict(() -> deviceTypeMapper.selectList(new LambdaQueryWrapper<DictDeviceType>()
                        .in(DictDeviceType::getCode, validCodes)
                        .orderByAsc(DictDeviceType::getTenantId)))
                .stream().filter(item -> item.getCode() != null && item.getName() != null)
                .collect(Collectors.toMap(DictDeviceType::getCode, DictDeviceType::getName, (a, b) -> a));
    }

    @Override
    public Map<String, String> alarmTypeNames(Collection<String> codes) {
        Set<String> validCodes = validCodes(codes);
        if (validCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return queryDict(() -> alarmTypeMapper.selectList(new LambdaQueryWrapper<DictAlarmType>()
                        .in(DictAlarmType::getCode, validCodes)
                        .orderByAsc(DictAlarmType::getTenantId)))
                .stream().filter(item -> item.getCode() != null && item.getName() != null)
                .collect(Collectors.toMap(DictAlarmType::getCode, DictAlarmType::getName, (a, b) -> a));
    }

    @Override
    public Map<String, String> dangerTypeNames(Collection<String> codes) {
        Set<String> validCodes = validCodes(codes);
        if (validCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return queryDict(() -> dangerTypeMapper.selectList(new LambdaQueryWrapper<DictDangerType>()
                        .in(DictDangerType::getCode, validCodes)
                        .orderByAsc(DictDangerType::getTenantId)))
                .stream().filter(item -> item.getCode() != null && item.getName() != null)
                .collect(Collectors.toMap(DictDangerType::getCode, DictDangerType::getName, (a, b) -> a));
    }

    @Override
    public Map<String, String> merchantTypeNames(Collection<String> codes) {
        Set<String> validCodes = validCodes(codes);
        if (validCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return queryDict(() -> merchantTypeMapper.selectList(new LambdaQueryWrapper<DictMerchantType>()
                        .in(DictMerchantType::getCode, validCodes)
                        .orderByAsc(DictMerchantType::getTenantId)))
                .stream().filter(item -> item.getCode() != null && item.getName() != null)
                .collect(Collectors.toMap(DictMerchantType::getCode, DictMerchantType::getName, (a, b) -> a));
    }

    @Override
    public List<DictDeviceMaker> deviceMakers() {
        return withTenantFilter(() -> deviceMakerMapper.selectList(new LambdaQueryWrapper<DictDeviceMaker>()
                .eq(DictDeviceMaker::getStatus, 1)
                .orderByAsc(DictDeviceMaker::getId)));
    }

    @Override
    public List<DictAlarmType> alarmTypes() {
        return withTenantFilter(() -> alarmTypeMapper.selectList(new LambdaQueryWrapper<DictAlarmType>()
                .eq(DictAlarmType::getStatus, 1)
                .orderByAsc(DictAlarmType::getId)));
    }

    @Override
    public List<DictDangerType> dangerTypes() {
        return withTenantFilter(() -> dangerTypeMapper.selectList(new LambdaQueryWrapper<DictDangerType>()
                .eq(DictDangerType::getStatus, 1)
                .orderByAsc(DictDangerType::getId)));
    }

    @Override
    public List<DictMerchantType> merchantTypes() {
        return withTenantFilter(() -> merchantTypeMapper.selectList(new LambdaQueryWrapper<DictMerchantType>()
                .eq(DictMerchantType::getStatus, 1)
                .orderByAsc(DictMerchantType::getId)));
    }

    @Override
    public List<DictAdcode> adcodes(String keyword) {
        LambdaQueryWrapper<DictAdcode> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(DictAdcode::getName, keyword)
                    .or().like(DictAdcode::getCode, keyword));
        }
        wrapper.last("LIMIT 50");
        return adcodeMapper.selectList(wrapper);
    }

    @Override
    public List<DictItemVO> adminList(String type) {
        return withTenantFilter(() -> adminListInTenant(type));
    }

    private List<DictItemVO> adminListInTenant(String type) {
        if (TYPE_DEVICE_TYPE.equals(type)) {
            return deviceTypeMapper.selectList(new LambdaQueryWrapper<DictDeviceType>()
                            .orderByAsc(DictDeviceType::getSort).orderByAsc(DictDeviceType::getId))
                    .stream().map(DictConverter::toItemVO).collect(Collectors.toList());
        }
        if (TYPE_DEVICE_MAKER.equals(type)) {
            return deviceMakerMapper.selectList(new LambdaQueryWrapper<DictDeviceMaker>()
                            .orderByAsc(DictDeviceMaker::getId))
                    .stream().map(DictConverter::toItemVO).collect(Collectors.toList());
        }
        if (TYPE_ALARM_TYPE.equals(type)) {
            return alarmTypeMapper.selectList(new LambdaQueryWrapper<DictAlarmType>()
                            .orderByAsc(DictAlarmType::getId))
                    .stream().map(DictConverter::toItemVO).collect(Collectors.toList());
        }
        if (TYPE_DANGER_TYPE.equals(type)) {
            return dangerTypeMapper.selectList(new LambdaQueryWrapper<DictDangerType>()
                            .orderByAsc(DictDangerType::getId))
                    .stream().map(DictConverter::toItemVO).collect(Collectors.toList());
        }
        if (TYPE_MERCHANT_TYPE.equals(type)) {
            return merchantTypeMapper.selectList(new LambdaQueryWrapper<DictMerchantType>()
                            .orderByAsc(DictMerchantType::getId))
                    .stream().map(DictConverter::toItemVO).collect(Collectors.toList());
        }
        throw new BizException("不支持的字典类型");
    }

    @Override
    public Long saveItem(String type, DictItemDTO request) {
        return withTenantFilter(() -> saveItemInTenant(type, request));
    }

    private Long saveItemInTenant(String type, DictItemDTO request) {
        if (!StringUtils.hasText(request.getCode()) || !StringUtils.hasText(request.getName())) {
            throw new BizException("字典编码和名称不能为空");
        }
        if (TYPE_DEVICE_TYPE.equals(type)) {
            return save(request, deviceTypeMapper, DictDeviceType::new,
                    DictDeviceType::getCode, DictDeviceType::getId);
        }
        if (TYPE_DEVICE_MAKER.equals(type)) {
            return save(request, deviceMakerMapper, DictDeviceMaker::new,
                    DictDeviceMaker::getCode, DictDeviceMaker::getId);
        }
        if (TYPE_ALARM_TYPE.equals(type)) {
            return save(request, alarmTypeMapper, DictAlarmType::new,
                    DictAlarmType::getCode, DictAlarmType::getId);
        }
        if (TYPE_DANGER_TYPE.equals(type)) {
            return save(request, dangerTypeMapper, DictDangerType::new,
                    DictDangerType::getCode, DictDangerType::getId);
        }
        if (TYPE_MERCHANT_TYPE.equals(type)) {
            return save(request, merchantTypeMapper, DictMerchantType::new,
                    DictMerchantType::getCode, DictMerchantType::getId);
        }
        throw new BizException("不支持的字典类型");
    }

    @Override
    public void deleteItem(String type, Long id) {
        withTenantFilter(() -> {
            deleteItemInTenant(type, id);
            return null;
        });
    }

    private void deleteItemInTenant(String type, Long id) {
        if (TYPE_DEVICE_TYPE.equals(type)) {
            DictDeviceType entity = deviceTypeMapper.selectById(id);
            if (entity != null) {
                assertNotReferenced(countReferences(() -> deviceMapper.selectCount(
                        new LambdaQueryWrapper<Device>().eq(Device::getType, entity.getCode()))));
                deviceTypeMapper.deleteById(id);
            }
        } else if (TYPE_DEVICE_MAKER.equals(type)) {
            DictDeviceMaker entity = deviceMakerMapper.selectById(id);
            if (entity != null) {
                assertNotReferenced(countReferences(() -> deviceMapper.selectCount(
                        new LambdaQueryWrapper<Device>().eq(Device::getMaker, entity.getCode()))));
                deviceMakerMapper.deleteById(id);
            }
        } else if (TYPE_ALARM_TYPE.equals(type)) {
            DictAlarmType entity = alarmTypeMapper.selectById(id);
            if (entity != null) {
                assertNotReferenced(countReferences(() -> fireAlarmMapper.selectCount(
                        new LambdaQueryWrapper<FireAlarm>().eq(FireAlarm::getAlarmType, entity.getCode()))));
                alarmTypeMapper.deleteById(id);
            }
        } else if (TYPE_DANGER_TYPE.equals(type)) {
            DictDangerType entity = dangerTypeMapper.selectById(id);
            if (entity != null) {
                assertNotReferenced(countReferences(() -> dangerMapper.selectCount(
                        new LambdaQueryWrapper<Danger>().eq(Danger::getDangerType, entity.getCode()))));
                dangerTypeMapper.deleteById(id);
            }
        } else if (TYPE_MERCHANT_TYPE.equals(type)) {
            DictMerchantType entity = merchantTypeMapper.selectById(id);
            if (entity != null) {
                assertNotReferenced(countReferences(() -> merchantMapper.selectCount(
                        new LambdaQueryWrapper<Merchant>().eq(Merchant::getType, entity.getCode()))));
                merchantTypeMapper.deleteById(id);
            }
        } else {
            throw new BizException("不支持的字典类型");
        }
    }

    /**
     * 新增或修改字典项。修改时锁定编码：业务表按 code 关联字典，改码会让历史数据失去名称映射。
     */
    private <T> Long save(DictItemDTO request, BaseMapper<T> mapper, Supplier<T> factory,
                          Function<T, String> codeGetter, Function<T, Long> idGetter) {
        T entity = request.getId() == null ? factory.get() : mapper.selectById(request.getId());
        if (entity == null) {
            throw new BizException("字典项不存在");
        }
        if (request.getId() == null) {
            Long exists = mapper.selectCount(new QueryWrapper<T>().eq("code", request.getCode()));
            if (exists != null && exists > 0) {
                throw new BizException("字典编码已存在");
            }
        } else {
            request.setCode(codeGetter.apply(entity));
        }
        BeanUtils.copyProperties(request, entity);
        if (request.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return idGetter.apply(entity);
    }

    private void assertNotReferenced(Long count) {
        if (count != null && count > 0) {
            throw new BizException("该字典项已被业务数据引用，不能删除");
        }
    }

    /** 平台删除基线时检查全部租户的引用，租户只检查本租户 */
    private Long countReferences(Supplier<Long> supplier) {
        if (TenantContext.getTenantId() != PLATFORM_TENANT_ID) {
            return supplier.get();
        }
        return withIgnoreTenant(supplier);
    }

    private Set<String> validCodes(Collection<String> codes) {
        return codes == null ? Collections.emptySet()
                : codes.stream().filter(StringUtils::hasText).collect(Collectors.toSet());
    }

    /** 平台视角跨租户查全量（tenant 0 优先），租户视角由租户插件自动过滤 */
    private <T> List<T> queryDict(Supplier<List<T>> supplier) {
        if (TenantContext.getTenantId() != PLATFORM_TENANT_ID) {
            return supplier.get();
        }
        return withIgnoreTenant(supplier);
    }

    /** 字典维护强制按当前租户过滤：平台账号只操作 tenant_id=0 的基线，不混入租户副本 */
    private <T> T withTenantFilter(Supplier<T> action) {
        boolean previous = TenantContext.isIgnoreTenant();
        TenantContext.setIgnoreTenant(false);
        try {
            return action.get();
        } finally {
            TenantContext.setIgnoreTenant(previous);
        }
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
