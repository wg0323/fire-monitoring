package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.dto.FloorPlanDTO;
import com.fm.module.device.dto.FloorSaveDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.MerchantFloorService;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 商户楼层服务。
 */
@Service
@RequiredArgsConstructor
public class MerchantFloorServiceImpl extends ServiceImpl<MerchantFloorMapper, MerchantFloor>
        implements MerchantFloorService {

    private final MerchantBuildingMapper buildingMapper;
    private final MerchantMapper merchantMapper;
    private final DeviceMapper deviceMapper;
    private final DataScopeService dataScopeService;

    @Override
    public List<MerchantFloor> listByMerchant(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyList();
        }
        Merchant merchant = scoped(() -> merchantMapper.selectById(merchantId));
        if (merchant == null) {
            return Collections.emptyList();
        }
        return scoped(() -> baseMapper.selectList(new LambdaQueryWrapper<MerchantFloor>()
                .eq(MerchantFloor::getMerchantId, merchantId)
                .orderByAsc(MerchantFloor::getFloorNo)
                .orderByAsc(MerchantFloor::getId)));
    }

    @Override
    public List<MerchantFloor> listByBuilding(Long buildingId) {
        if (buildingId == null) {
            return Collections.emptyList();
        }
        MerchantBuilding building = scoped(() -> buildingMapper.selectById(buildingId));
        if (building == null) {
            return Collections.emptyList();
        }
        return scoped(() -> baseMapper.selectList(new LambdaQueryWrapper<MerchantFloor>()
                .eq(MerchantFloor::getBuildingId, buildingId)
                .orderByAsc(MerchantFloor::getFloorNo)
                .orderByAsc(MerchantFloor::getId)));
    }

    @Override
    @Transactional
    public Long create(FloorSaveDTO request) {
        validate(request);
        MerchantFloor floor = toEntity(request);
        floor.setTenantId(TenantContext.getTenantId());
        floor.setSort(request.getSort() == null ? 0 : request.getSort());
        floor.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        baseMapper.insert(floor);
        return floor.getId();
    }

    @Override
    @Transactional
    public void update(FloorSaveDTO request) {
        if (request.getId() == null) {
            throw new BizException("楼层 id 不能为空");
        }
        requireFloor(request.getId());
        validate(request);
        MerchantFloor floor = toEntity(request);
        floor.setId(request.getId());
        floor.setTenantId(null);
        baseMapper.updateById(floor);
    }

    @Override
    @Transactional
    public void updatePlan(Long id, FloorPlanDTO request) {
        requireFloor(id);
        if (request == null || !StringUtils.hasText(request.getPlanUrl())) {
            throw new BizException("请先上传平面图");
        }
        if (request.getPlanWidth() == null || request.getPlanWidth() <= 0
                || request.getPlanHeight() == null || request.getPlanHeight() <= 0) {
            throw new BizException("平面图宽高不合法");
        }
        baseMapper.update(null, new LambdaUpdateWrapper<MerchantFloor>()
                .eq(MerchantFloor::getId, id)
                .set(MerchantFloor::getFloorPlanUrl, request.getPlanUrl())
                .set(MerchantFloor::getPlanWidth, request.getPlanWidth())
                .set(MerchantFloor::getPlanHeight, request.getPlanHeight()));
        // 替换平面图后旧点位失效，清空楼层内所有点位，设备回到未布点列表
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getFloorId, id)
                .eq(Device::getLocationType, Device.LOCATION_FLOOR)
                .set(Device::getLocationType, Device.LOCATION_NONE)
                .set(Device::getBuildingId, null)
                .set(Device::getFloorId, null)
                .set(Device::getFloorX, null)
                .set(Device::getFloorY, null));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireFloor(id);
        Long deviceCount = scoped(() -> deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getFloorId, id)
                .eq(Device::getLocationType, Device.LOCATION_FLOOR)));
        if (deviceCount != null && deviceCount > 0) {
            throw new BizException("该楼层仍有布点设备，请先移除点位后再删除");
        }
        baseMapper.deleteById(id);
    }

    private MerchantFloor requireFloor(Long id) {
        MerchantFloor floor = id == null ? null : scoped(() -> baseMapper.selectById(id));
        if (floor == null) {
            throw new BizException("楼层不存在或无权操作");
        }
        return floor;
    }

    private void validate(FloorSaveDTO request) {
        if (request.getBuildingId() == null || request.getMerchantId() == null) {
            throw new BizException("所属楼宇与商户不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("楼层名称不能为空");
        }
        MerchantBuilding building = scoped(() -> buildingMapper.selectById(request.getBuildingId()));
        if (building == null) {
            throw new BizException("楼宇不存在");
        }
        if (!building.getMerchantId().equals(request.getMerchantId())) {
            throw new BizException("楼层与楼宇所属商户不匹配");
        }
    }

    private MerchantFloor toEntity(FloorSaveDTO request) {
        MerchantFloor floor = new MerchantFloor();
        floor.setBuildingId(request.getBuildingId());
        floor.setMerchantId(request.getMerchantId());
        floor.setName(request.getName());
        floor.setFloorNo(request.getFloorNo());
        floor.setFloorPlanUrl(request.getFloorPlanUrl());
        floor.setPlanWidth(request.getPlanWidth());
        floor.setPlanHeight(request.getPlanHeight());
        floor.setSort(request.getSort());
        floor.setStatus(request.getStatus());
        return floor;
    }

    private <T> T scoped(Supplier<T> action) {
        if (!dataScopeService.isPlatform()) {
            return action.get();
        }
        return dataScopeService.withIgnoreTenant(action);
    }
}
