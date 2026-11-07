package com.fm.module.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.device.dto.FloorPlanDTO;
import com.fm.module.device.dto.FloorSaveDTO;
import com.fm.module.device.entity.MerchantFloor;

import java.util.List;

/**
 * 商户楼层服务。
 */
public interface MerchantFloorService extends IService<MerchantFloor> {

    List<MerchantFloor> listByMerchant(Long merchantId);

    List<MerchantFloor> listByBuilding(Long buildingId);

    Long create(FloorSaveDTO request);

    void update(FloorSaveDTO request);

    void updatePlan(Long id, FloorPlanDTO request);

    void delete(Long id);
}
