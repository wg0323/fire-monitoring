package com.fm.module.device.converter;

import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.vo.BuildingNodeVO;
import com.fm.module.device.vo.BuildingVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 楼宇对象转换。
 */
public final class BuildingConverter {

    private BuildingConverter() {
    }

    public static BuildingVO toVO(MerchantBuilding building) {
        if (building == null) {
            return null;
        }
        BuildingVO vo = new BuildingVO();
        BeanUtils.copyProperties(building, vo);
        return vo;
    }

    public static List<BuildingVO> toVOList(List<MerchantBuilding> buildings) {
        return buildings.stream().map(BuildingConverter::toVO).collect(Collectors.toList());
    }

    public static BuildingNodeVO toNodeVO(MerchantBuilding building) {
        if (building == null) {
            return null;
        }
        BuildingNodeVO vo = new BuildingNodeVO();
        BeanUtils.copyProperties(building, vo);
        return vo;
    }
}
