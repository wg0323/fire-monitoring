package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.converter.BuildingConverter;
import com.fm.module.device.dto.BuildingSaveDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.MerchantBuildingService;
import com.fm.module.device.vo.BuildingVO;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 商户楼宇服务。
 */
@Service
@RequiredArgsConstructor
public class MerchantBuildingServiceImpl extends ServiceImpl<MerchantBuildingMapper, MerchantBuilding>
        implements MerchantBuildingService {

    private final MerchantMapper merchantMapper;
    private final MerchantFloorMapper floorMapper;
    private final DeviceMapper deviceMapper;
    private final DataScopeService dataScopeService;

    @Override
    public List<BuildingVO> listByMerchant(Long merchantId) {
        if (merchantId == null) {
            return Collections.emptyList();
        }
        Merchant merchant = scoped(() -> merchantMapper.selectById(merchantId));
        if (merchant == null) {
            return Collections.emptyList();
        }
        List<MerchantBuilding> buildings = scoped(() -> baseMapper.selectList(
                new LambdaQueryWrapper<MerchantBuilding>()
                        .eq(MerchantBuilding::getMerchantId, merchantId)
                        .orderByAsc(MerchantBuilding::getSort)
                        .orderByAsc(MerchantBuilding::getId)));
        Map<Long, Long> deviceCounts = deviceCounts(merchantId);
        return buildings.stream().map(building -> {
            BuildingVO vo = BuildingConverter.toVO(building);
            vo.setDeviceCount(deviceCounts.getOrDefault(building.getId(), 0L));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long create(BuildingSaveDTO request) {
        validate(request);
        MerchantBuilding building = toEntity(request);
        building.setTenantId(TenantContext.getTenantId());
        building.setSort(request.getSort() == null ? 0 : request.getSort());
        building.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        baseMapper.insert(building);
        return building.getId();
    }

    @Override
    @Transactional
    public void update(BuildingSaveDTO request) {
        if (request.getId() == null) {
            throw new BizException("楼宇 id 不能为空");
        }
        requireBuilding(request.getId());
        validate(request);
        MerchantBuilding building = toEntity(request);
        building.setId(request.getId());
        building.setTenantId(null);
        baseMapper.updateById(building);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireBuilding(id);
        Long floorCount = scoped(() -> floorMapper.selectCount(new LambdaQueryWrapper<MerchantFloor>()
                .eq(MerchantFloor::getBuildingId, id)));
        if (floorCount != null && floorCount > 0) {
            throw new BizException("楼宇下仍有楼层，请先删除楼层");
        }
        baseMapper.deleteById(id);
    }

    private MerchantBuilding requireBuilding(Long id) {
        MerchantBuilding building = id == null ? null : scoped(() -> baseMapper.selectById(id));
        if (building == null) {
            throw new BizException("楼宇不存在或无权操作");
        }
        return building;
    }

    private void validate(BuildingSaveDTO request) {
        if (request.getMerchantId() == null) {
            throw new BizException("所属商户不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("楼宇名称不能为空");
        }
        Merchant merchant = scoped(() -> merchantMapper.selectById(request.getMerchantId()));
        if (merchant == null) {
            throw new BizException("商户不存在或无权操作");
        }
    }

    private Map<Long, Long> deviceCounts(Long merchantId) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.select("building_id", "count(*) as cnt")
                .eq("merchant_id", merchantId)
                .isNotNull("building_id")
                .groupBy("building_id");
        List<Map<String, Object>> rows = scoped(() -> deviceMapper.selectMaps(wrapper));
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object buildingId = row.get("building_id");
            Object count = row.get("cnt");
            if (buildingId instanceof Number && count instanceof Number) {
                result.put(((Number) buildingId).longValue(), ((Number) count).longValue());
            }
        }
        return result;
    }

    private MerchantBuilding toEntity(BuildingSaveDTO request) {
        MerchantBuilding building = new MerchantBuilding();
        building.setMerchantId(request.getMerchantId());
        building.setName(request.getName());
        building.setSort(request.getSort());
        building.setRemark(request.getRemark());
        building.setStatus(request.getStatus());
        return building;
    }

    private <T> T scoped(Supplier<T> action) {
        if (!dataScopeService.isPlatform()) {
            return action.get();
        }
        return dataScopeService.withIgnoreTenant(action);
    }
}
