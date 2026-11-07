package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.converter.DeviceConverter;
import com.fm.module.device.dto.DeviceQueryDTO;
import com.fm.module.device.dto.DeviceSaveDTO;
import com.fm.module.device.dto.FloorPointsSaveDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.DictDeviceMaker;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DictDeviceMakerMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DeviceService;
import com.fm.module.device.service.DeviceRegistrySync;
import com.fm.module.device.vo.DeviceVO;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 设备服务：主表瘦身 + 三模式定位校验。
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SN_MAX_LENGTH = 32;
    private static final int NAME_MAX_LENGTH = 64;
    private static final int POSITION_MAX_LENGTH = 255;
    private static final int CARD_NO_MAX_LENGTH = 24;
    private static final int CHANNEL_MAX = 255;
    private static final Long EMPTY_ID = -1L;

    private final DictDeviceTypeMapper deviceTypeMapper;
    private final DictDeviceMakerMapper deviceMakerMapper;
    private final MerchantFloorMapper floorMapper;
    private final MerchantBuildingMapper buildingMapper;
    private final MerchantMapper merchantMapper;
    private final DeviceRegistrySync registrySync;
    private final DataScopeService dataScopeService;

    @Override
    public Page<DeviceVO> page(DeviceQueryDTO query) {
        Page<Device> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Device> wrapper = buildQueryWrapper(query);
        List<Long> tenantIds = applyScope(wrapper);
        if (tenantIds != null && tenantIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize());
        }
        Page<Device> rows = tenantIds == null
                ? baseMapper.selectPage(page, wrapper)
                : dataScopeService.withIgnoreTenant(() -> baseMapper.selectPage(page, wrapper));
        Page<DeviceVO> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(enrich(rows.getRecords()));
        return result;
    }

    @Override
    public DeviceVO getVOById(Serializable id) {
        Device device = getById(id);
        if (device == null) {
            return null;
        }
        return enrich(Collections.singletonList(device)).get(0);
    }

    @Override
    public List<DeviceVO> listForExport(DeviceQueryDTO query) {
        LambdaQueryWrapper<Device> wrapper = buildQueryWrapper(query);
        List<Long> tenantIds = applyScope(wrapper);
        if (tenantIds != null && tenantIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Device> rows = tenantIds == null
                ? baseMapper.selectList(wrapper)
                : dataScopeService.withIgnoreTenant(() -> baseMapper.selectList(wrapper));
        return enrich(rows);
    }

    private LambdaQueryWrapper<Device> buildQueryWrapper(DeviceQueryDTO query) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Device::getName, query.getKeyword())
                    .or().like(Device::getSn, query.getKeyword())
                    .or().like(Device::getPosition, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Device::getType, query.getType());
        }
        if (query.getMerchantId() != null) {
            wrapper.eq(Device::getMerchantId, query.getMerchantId());
        }
        if (Boolean.TRUE.equals(query.getUnbound())) {
            wrapper.isNull(Device::getMerchantId);
        }
        if (query.getStatus() != null) {
            wrapper.eq(Device::getStatus, query.getStatus());
        }
        if (query.getOnlineStatus() != null) {
            wrapper.eq(Device::getOnlineStatus, query.getOnlineStatus());
        }
        if (query.getLocationType() != null) {
            wrapper.eq(Device::getLocationType, query.getLocationType());
        }
        wrapper.orderByDesc(Device::getCreateTime);
        return wrapper;
    }

    /**
     * 应用租户与商户数据范围。
     *
     * @return null=无需忽略租户插件；非空=平台账号需要限定的租户列表
     */
    private List<Long> applyScope(LambdaQueryWrapper<Device> wrapper) {
        List<Long> merchantIds = dataScopeService.scopedMerchantIds();
        if (merchantIds != null) {
            if (merchantIds.isEmpty()) {
                wrapper.eq(Device::getId, EMPTY_ID);
            } else {
                wrapper.in(Device::getMerchantId, merchantIds);
            }
        }
        if (!dataScopeService.isPlatform()) {
            return null;
        }
        List<Long> tenantIds = dataScopeService.authorizedTenantIds();
        if (!tenantIds.isEmpty()) {
            wrapper.in(Device::getTenantId, tenantIds);
        }
        return tenantIds;
    }

    @Override
    public Device getById(Serializable id) {
        Device device;
        if (!dataScopeService.isPlatform()) {
            device = super.getById(id);
        } else {
            device = dataScopeService.withIgnoreTenant(() -> super.getById(id));
            if (device != null && !dataScopeService.canAccess(device.getTenantId())) {
                return null;
            }
        }
        if (device == null) {
            return null;
        }
        List<Long> merchantIds = dataScopeService.scopedMerchantIds();
        if (merchantIds != null
                && (device.getMerchantId() == null || !merchantIds.contains(device.getMerchantId()))) {
            return null;
        }
        return device;
    }

    private List<DeviceVO> enrich(List<Device> devices) {
        List<DeviceVO> list = DeviceConverter.toVOList(devices);
        if (list.isEmpty()) {
            return list;
        }
        Map<String, String> typeNames = typeNames(list.stream().map(DeviceVO::getType)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        Map<String, String> makerNames = makerNames(list.stream().map(DeviceVO::getMaker)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        Map<Long, String> merchantNames = merchantNames(list.stream().map(DeviceVO::getMerchantId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        Set<Long> buildingIds = list.stream().map(DeviceVO::getBuildingId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> floorIds = list.stream().map(DeviceVO::getFloorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> buildingNames = buildingNames(buildingIds);
        Map<Long, String> floorNames = floorNames(floorIds);
        for (DeviceVO vo : list) {
            vo.setTypeName(typeNames.get(vo.getType()));
            vo.setMakerName(makerNames.get(vo.getMaker()));
            vo.setMerchantName(merchantNames.get(vo.getMerchantId()));
            vo.setBuildingName(buildingNames.get(vo.getBuildingId()));
            vo.setFloorName(floorNames.get(vo.getFloorId()));
            vo.setLocationText(locationText(vo));
        }
        return list;
    }

    private String locationText(DeviceVO vo) {
        if (StringUtils.hasText(vo.getPosition())) {
            return vo.getPosition();
        }
        Integer locationType = vo.getLocationType();
        if (locationType != null && locationType == Device.LOCATION_FLOOR
                && (vo.getBuildingName() != null || vo.getFloorName() != null)) {
            return ((vo.getBuildingName() == null ? "" : vo.getBuildingName())
                    + (vo.getFloorName() == null ? "" : " " + vo.getFloorName())).trim();
        }
        return "-";
    }

    private Map<String, String> typeNames(List<String> codes) {
        if (codes.isEmpty()) {
            return Collections.emptyMap();
        }
        return deviceTypeMapper.selectList(new LambdaQueryWrapper<DictDeviceType>()
                        .in(DictDeviceType::getCode, codes))
                .stream().collect(Collectors.toMap(DictDeviceType::getCode, DictDeviceType::getName, (a, b) -> a));
    }

    private Map<String, String> makerNames(List<String> codes) {
        if (codes.isEmpty()) {
            return Collections.emptyMap();
        }
        return deviceMakerMapper.selectList(new LambdaQueryWrapper<DictDeviceMaker>()
                        .in(DictDeviceMaker::getCode, codes))
                .stream().collect(Collectors.toMap(DictDeviceMaker::getCode, DictDeviceMaker::getName, (a, b) -> a));
    }

    private Map<Long, String> merchantNames(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Merchant> merchants = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> merchantMapper.selectBatchIds(ids))
                : merchantMapper.selectBatchIds(ids);
        return merchants.stream().collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
    }

    private Map<Long, String> buildingNames(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MerchantBuilding> buildings = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> buildingMapper.selectBatchIds(ids))
                : buildingMapper.selectBatchIds(ids);
        return buildings.stream().collect(Collectors.toMap(MerchantBuilding::getId, MerchantBuilding::getName,
                (a, b) -> a));
    }

    private Map<Long, String> floorNames(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MerchantFloor> floors = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> floorMapper.selectBatchIds(ids))
                : floorMapper.selectBatchIds(ids);
        return floors.stream().collect(Collectors.toMap(MerchantFloor::getId, MerchantFloor::getName, (a, b) -> a));
    }

    @Override
    @Transactional
    public Long create(DeviceSaveDTO request) {
        validate(request);
        Long exists = dataScopeService.withIgnoreTenant(() -> baseMapper.selectCount(
                new LambdaQueryWrapper<Device>().eq(Device::getSn, request.getSn())));
        if (exists != null && exists > 0) {
            throw new BizException("设备 SN 已存在");
        }
        Device device = toEntity(request);
        device.setTenantId(resolveTenantId(request));
        device.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        device.setOnlineStatus(0);
        device.setChannelNum(request.getChannelNum() == null ? 1 : request.getChannelNum());
        baseMapper.insert(device);
        registrySync.refresh(device);
        return device.getId();
    }

    /**
     * 平台账号必须显式指定归属运营中心，租户账号只能落本租户。
     */
    private Long resolveTenantId(DeviceSaveDTO request) {
        if (!"PLATFORM".equals(TenantContext.getUserType())) {
            return TenantContext.getTenantId();
        }
        if (request.getTenantId() == null || request.getTenantId() <= 0) {
            throw new BizException("请选择设备所属运营中心");
        }
        return request.getTenantId();
    }

    @Override
    @Transactional
    public void update(DeviceSaveDTO request) {
        if (request.getId() == null) {
            throw new BizException("设备 id 不能为空");
        }
        Device existing = baseMapper.selectById(request.getId());
        if (existing == null) {
            throw new BizException("设备不存在");
        }
        if (StringUtils.hasText(request.getSn()) && !request.getSn().equals(existing.getSn())) {
            throw new BizException("设备编号保存后不可修改");
        }
        request.setSn(existing.getSn());
        validate(request);
        Device device = toEntity(request);
        device.setId(request.getId());
        device.setTenantId(null);
        clearUnusedLocation(request);
        baseMapper.updateById(device);
        registrySync.refresh(baseMapper.selectById(request.getId()));
    }

    @Override
    public void delete(Long id) {
        Device existing = baseMapper.selectById(id);
        baseMapper.deleteById(id);
        if (existing != null) {
            registrySync.remove(existing.getSn());
        }
    }

    @Override
    public List<Device> floorPoints(Long floorId) {
        MerchantFloor floor = requireFloor(floorId);
        return scoped(() -> baseMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getFloorId, floor.getId())
                .eq(Device::getLocationType, Device.LOCATION_FLOOR)
                .isNotNull(Device::getFloorX)
                .isNotNull(Device::getFloorY)
                .orderByAsc(Device::getFloorX)
                .orderByAsc(Device::getFloorY)));
    }

    @Override
    public List<Device> floorCandidates(Long floorId) {
        MerchantFloor floor = requireFloor(floorId);
        return scoped(() -> baseMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getMerchantId, floor.getMerchantId())
                .eq(Device::getStatus, 1)
                .and(wrapper -> wrapper.isNull(Device::getFloorId)
                        .or().ne(Device::getFloorId, floor.getId())
                        .or().isNull(Device::getFloorX))
                .orderByAsc(Device::getSn)
                .last("LIMIT 200")));
    }

    @Override
    @Transactional
    public void saveFloorPoints(Long floorId, FloorPointsSaveDTO request) {
        MerchantFloor floor = requireFloor(floorId);
        List<FloorPointsSaveDTO.Point> points = request == null || request.getPoints() == null
                ? Collections.emptyList() : request.getPoints();
        List<Long> removedIds = request == null || request.getRemovedDeviceIds() == null
                ? Collections.emptyList() : request.getRemovedDeviceIds();
        Set<Long> pointDeviceIds = new HashSet<>();
        for (FloorPointsSaveDTO.Point point : points) {
            if (point.getDeviceId() == null) {
                throw new BizException("设备不能为空");
            }
            if (!pointDeviceIds.add(point.getDeviceId())) {
                throw new BizException("同一设备只能有一个点位");
            }
            validateCoordinate(point.getX(), point.getY());
        }
        Set<Long> allIds = new HashSet<>(pointDeviceIds);
        allIds.addAll(removedIds.stream().filter(id -> id != null).collect(Collectors.toSet()));
        if (allIds.isEmpty()) {
            return;
        }
        List<Device> devices = baseMapper.selectBatchIds(allIds);
        if (devices.size() != allIds.size()) {
            throw new BizException("部分设备不存在或无权操作");
        }
        Map<Long, Device> deviceMap = devices.stream()
                .collect(Collectors.toMap(Device::getId, Function.identity(), (a, b) -> a));
        for (Device device : devices) {
            if (!floor.getMerchantId().equals(device.getMerchantId())) {
                throw new BizException("设备与楼层不属于同一商户");
            }
        }
        for (FloorPointsSaveDTO.Point point : points) {
            baseMapper.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, point.getDeviceId())
                    .set(Device::getLocationType, Device.LOCATION_FLOOR)
                    .set(Device::getBuildingId, floor.getBuildingId())
                    .set(Device::getFloorId, floor.getId())
                    .set(Device::getFloorX, point.getX())
                    .set(Device::getFloorY, point.getY())
                    .set(Device::getLng, null)
                    .set(Device::getLat, null));
        }
        for (Long deviceId : removedIds) {
            Device device = deviceId == null ? null : deviceMap.get(deviceId);
            if (device == null || !floor.getId().equals(device.getFloorId())) {
                continue;
            }
            baseMapper.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, deviceId)
                    .set(Device::getLocationType, Device.LOCATION_NONE)
                    .set(Device::getBuildingId, null)
                    .set(Device::getFloorId, null)
                    .set(Device::getFloorX, null)
                    .set(Device::getFloorY, null));
        }
    }

    private void validateCoordinate(BigDecimal x, BigDecimal y) {
        if (x == null || y == null) {
            throw new BizException("点位坐标不能为空");
        }
        if (x.compareTo(BigDecimal.ZERO) < 0 || x.compareTo(HUNDRED) > 0
                || y.compareTo(BigDecimal.ZERO) < 0 || y.compareTo(HUNDRED) > 0) {
            throw new BizException("点位坐标必须在 0-100 之间");
        }
    }

    private MerchantFloor requireFloor(Long floorId) {
        MerchantFloor floor = floorId == null ? null : scoped(() -> floorMapper.selectById(floorId));
        if (floor == null) {
            throw new BizException("楼层不存在或无权操作");
        }
        return floor;
    }

    private <T> T scoped(Supplier<T> action) {
        if (!dataScopeService.isPlatform()) {
            return action.get();
        }
        return dataScopeService.withIgnoreTenant(action);
    }

    /**
     * 三模式定位校验：0 未设置、1 GPS、2 楼层平面图。
     */
    private void validate(DeviceSaveDTO request) {
        if (!StringUtils.hasText(request.getSn())) {
            throw new BizException("设备 SN 不能为空");
        }
        request.setSn(request.getSn().trim());
        if (request.getSn().length() > SN_MAX_LENGTH) {
            throw new BizException("设备编号不能超过 " + SN_MAX_LENGTH + " 个字符");
        }
        if (StringUtils.hasText(request.getName()) && request.getName().length() > NAME_MAX_LENGTH) {
            throw new BizException("设备名称不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }
        if (StringUtils.hasText(request.getPosition()) && request.getPosition().length() > POSITION_MAX_LENGTH) {
            throw new BizException("安装位置不能超过 " + POSITION_MAX_LENGTH + " 个字符");
        }
        if (StringUtils.hasText(request.getCardNo()) && request.getCardNo().length() > CARD_NO_MAX_LENGTH) {
            throw new BizException("4G 卡号不能超过 " + CARD_NO_MAX_LENGTH + " 个字符");
        }
        if (request.getChannelNum() != null && (request.getChannelNum() < 1 || request.getChannelNum() > CHANNEL_MAX)) {
            throw new BizException("通道数必须在 1-" + CHANNEL_MAX + " 之间");
        }
        if (isFuture(request.getActiveDate()) || isFuture(request.getInstallDate())) {
            throw new BizException("激活日期与安装日期不能晚于今天");
        }
        if (!StringUtils.hasText(request.getType())) {
            throw new BizException("设备类型不能为空");
        }
        Long typeCount = deviceTypeMapper.selectCount(new LambdaQueryWrapper<DictDeviceType>()
                .eq(DictDeviceType::getCode, request.getType())
                .eq(DictDeviceType::getStatus, 1));
        if (typeCount == null || typeCount == 0) {
            throw new BizException("设备类型不存在或已禁用");
        }
        if (StringUtils.hasText(request.getMaker())) {
            Long makerCount = deviceMakerMapper.selectCount(new LambdaQueryWrapper<DictDeviceMaker>()
                    .eq(DictDeviceMaker::getCode, request.getMaker())
                    .eq(DictDeviceMaker::getStatus, 1));
            if (makerCount == null || makerCount == 0) {
                throw new BizException("设备制造商不存在或已禁用");
            }
        }
        if (request.getMerchantId() != null && merchantMapper.selectById(request.getMerchantId()) == null) {
            throw new BizException("商户不存在或无权操作");
        }

        int locationType = request.getLocationType() == null ? Device.LOCATION_NONE : request.getLocationType();
        if (locationType == Device.LOCATION_GPS) {
            if (request.getLng() == null || request.getLat() == null) {
                throw new BizException("GPS 定位必须填写经纬度");
            }
            if (request.getLng() < -180 || request.getLng() > 180
                    || request.getLat() < -90 || request.getLat() > 90) {
                throw new BizException("经纬度超出合法范围");
            }
        } else if (locationType == Device.LOCATION_FLOOR) {
            if (request.getBuildingId() == null || request.getFloorId() == null) {
                throw new BizException("楼层定位必须填写楼宇与楼层");
            }
            if ((request.getFloorX() == null) != (request.getFloorY() == null)) {
                throw new BizException("平面坐标必须同时填写或同时留空");
            }
            if (request.getFloorX() != null) {
                if (request.getFloorX().compareTo(BigDecimal.ZERO) < 0
                        || request.getFloorX().compareTo(HUNDRED) > 0
                        || request.getFloorY().compareTo(BigDecimal.ZERO) < 0
                        || request.getFloorY().compareTo(HUNDRED) > 0) {
                    throw new BizException("平面坐标必须在 0-100 之间");
                }
            }
            MerchantFloor floor = floorMapper.selectById(request.getFloorId());
            if (floor == null) {
                throw new BizException("楼层不存在");
            }
            if (!floor.getBuildingId().equals(request.getBuildingId())) {
                throw new BizException("楼层与楼宇不匹配");
            }
            if (request.getMerchantId() != null && !floor.getMerchantId().equals(request.getMerchantId())) {
                throw new BizException("楼层与商户不匹配");
            }
        } else if (locationType != Device.LOCATION_NONE) {
            throw new BizException("定位类型不合法");
        }
    }

    private boolean isFuture(LocalDateTime time) {
        return time != null && time.toLocalDate().isAfter(LocalDate.now());
    }

    /**
     * 切换定位模式时清空不适用的旧坐标，避免脏数据残留。
     */
    private void clearUnusedLocation(DeviceSaveDTO request) {
        int locationType = request.getLocationType() == null ? Device.LOCATION_NONE : request.getLocationType();
        LambdaUpdateWrapper<Device> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Device::getId, request.getId());
        if (locationType == Device.LOCATION_NONE) {
            wrapper.set(Device::getLng, null).set(Device::getLat, null)
                    .set(Device::getBuildingId, null).set(Device::getFloorId, null)
                    .set(Device::getFloorX, null).set(Device::getFloorY, null);
        } else if (locationType == Device.LOCATION_GPS) {
            wrapper.set(Device::getBuildingId, null).set(Device::getFloorId, null)
                    .set(Device::getFloorX, null).set(Device::getFloorY, null);
        } else {
            wrapper.set(Device::getLng, null).set(Device::getLat, null);
        }
        baseMapper.update(null, wrapper);
    }

    private Device toEntity(DeviceSaveDTO request) {
        Device device = new Device();
        device.setName(request.getName());
        device.setSn(request.getSn());
        device.setType(request.getType());
        device.setMerchantId(request.getMerchantId());
        device.setMaker(request.getMaker());
        device.setPosition(request.getPosition());
        device.setLng(request.getLng());
        device.setLat(request.getLat());
        device.setLocationType(request.getLocationType() == null ? Device.LOCATION_NONE : request.getLocationType());
        device.setBuildingId(request.getBuildingId());
        device.setFloorId(request.getFloorId());
        device.setFloorX(request.getFloorX());
        device.setFloorY(request.getFloorY());
        device.setSim(request.getSim());
        device.setCardNo(request.getCardNo());
        device.setChannelNum(request.getChannelNum());
        device.setStatus(request.getStatus());
        device.setActiveDate(request.getActiveDate());
        device.setExpireDate(request.getExpireDate());
        device.setInstallDate(request.getInstallDate());
        device.setFExt(request.getFExt());
        return device;
    }
}
