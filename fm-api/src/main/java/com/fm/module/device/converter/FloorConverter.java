package com.fm.module.device.converter;

import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.vo.FloorNodeVO;
import com.fm.module.device.vo.FloorVO;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 楼层对象转换。
 */
public final class FloorConverter {

    private FloorConverter() {
    }

    public static FloorVO toVO(MerchantFloor floor) {
        if (floor == null) {
            return null;
        }
        FloorVO vo = new FloorVO();
        vo.setId(floor.getId());
        vo.setTenantId(floor.getTenantId());
        vo.setBuildingId(floor.getBuildingId());
        vo.setMerchantId(floor.getMerchantId());
        vo.setName(floor.getName());
        vo.setFloorNo(floor.getFloorNo());
        vo.setFloorPlanUrl(floor.getFloorPlanUrl());
        vo.setPlanWidth(floor.getPlanWidth());
        vo.setPlanHeight(floor.getPlanHeight());
        vo.setSort(floor.getSort());
        vo.setStatus(floor.getStatus());
        vo.setHasPlan(StringUtils.hasText(floor.getFloorPlanUrl()));
        return vo;
    }

    public static List<FloorVO> toVOList(List<MerchantFloor> floors) {
        return floors.stream().map(FloorConverter::toVO).collect(Collectors.toList());
    }

    public static FloorNodeVO toNodeVO(MerchantFloor floor) {
        if (floor == null) {
            return null;
        }
        FloorNodeVO vo = new FloorNodeVO();
        BeanUtils.copyProperties(floor, vo);
        return vo;
    }
}
