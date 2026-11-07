package com.fm.module.inspection.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.inspection.dto.PlanMerchantQueryDTO;
import com.fm.module.inspection.dto.PlanQueryDTO;
import com.fm.module.inspection.dto.PlanResultSaveDTO;
import com.fm.module.inspection.dto.PlanSaveDTO;
import com.fm.module.inspection.entity.Plan;
import com.fm.module.inspection.vo.PlanMerchantVO;
import com.fm.module.inspection.vo.PlanResultVO;
import com.fm.module.inspection.vo.PlanVO;

import java.util.List;

/**
 * 巡查计划服务。
 */
public interface PlanService extends IService<Plan> {

    Page<PlanVO> page(PlanQueryDTO query);

    /**
     * 小程序首页：进行中且仍有商户未巡查的计划数，范围与 page 一致。
     */
    long countOngoingUnfinished();

    Long create(PlanSaveDTO request);

    void update(PlanSaveDTO request);

    void delete(Long id);

    Page<PlanResultVO> results(Long planId, long pageNum, long pageSize);

    PlanVO detail(Long id);

    List<PlanMerchantVO> merchants(Long planId);

    Page<PlanMerchantVO> merchantPage(Long planId, PlanMerchantQueryDTO query);

    void saveResult(PlanResultSaveDTO request, Long userId);
}
