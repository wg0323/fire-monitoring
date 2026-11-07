package com.fm.module.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.device.dto.BuildingSaveDTO;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.vo.BuildingVO;

import java.util.List;

/**
 * 商户楼宇服务。
 */
public interface MerchantBuildingService extends IService<MerchantBuilding> {

    List<BuildingVO> listByMerchant(Long merchantId);

    Long create(BuildingSaveDTO request);

    void update(BuildingSaveDTO request);

    void delete(Long id);
}
