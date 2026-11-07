package com.fm.module.inspection.converter;

import com.fm.module.device.entity.Merchant;
import com.fm.module.inspection.entity.Plan;
import com.fm.module.inspection.entity.PlanResult;
import com.fm.module.inspection.vo.PlanMerchantVO;
import com.fm.module.inspection.vo.PlanResultVO;
import com.fm.module.inspection.vo.PlanVO;

/**
 * 巡查计划对象转换。
 */
public final class PlanConverter {

    private PlanConverter() {
    }

    public static PlanMerchantVO toMerchantVO(Merchant merchant, PlanResult result) {
        PlanMerchantVO vo = new PlanMerchantVO();
        vo.setId(merchant.getId());
        vo.setName(merchant.getName());
        vo.setAddr(merchant.getAddr());
        vo.setFinished(result != null);
        if (result != null) {
            vo.setLastResult(result.getResult());
            vo.setLastPatrolBy(result.getPatrolBy());
            vo.setLastPatrolTime(result.getPatrolTime());
            vo.setDangerId(result.getDangerId());
        }
        return vo;
    }

    public static PlanVO toVO(Plan plan, String workAreaName, String createByName, Integer status,
                              Long merchantTotal, Long finishCount, Long dangerCount) {
        PlanVO vo = new PlanVO();
        vo.setId(plan.getId());
        vo.setTenantId(plan.getTenantId());
        vo.setWorkAreaId(plan.getWorkAreaId());
        vo.setWorkAreaName(workAreaName);
        vo.setName(plan.getName());
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setStatus(status);
        vo.setRemark(plan.getRemark());
        vo.setCreateBy(createByName);
        vo.setCreateTime(plan.getCreateTime());
        vo.setMerchantTotal(merchantTotal);
        vo.setFinishCount(finishCount);
        vo.setDangerCount(dangerCount);
        return vo;
    }

    public static PlanResultVO toResultVO(PlanResult item, String merchantName) {
        PlanResultVO vo = new PlanResultVO();
        vo.setId(item.getId());
        vo.setPlanId(item.getPlanId());
        vo.setMerchantId(item.getMerchantId());
        vo.setMerchantName(merchantName);
        vo.setUserId(item.getUserId());
        vo.setWorkAreaId(item.getWorkAreaId());
        vo.setDangerId(item.getDangerId());
        vo.setPatrolBy(item.getPatrolBy());
        vo.setPatrolTime(item.getPatrolTime());
        vo.setLng(item.getLng());
        vo.setLat(item.getLat());
        vo.setResult(item.getResult());
        vo.setRemark(item.getRemark());
        vo.setPic1(item.getPic1());
        vo.setPic2(item.getPic2());
        vo.setPic3(item.getPic3());
        return vo;
    }
}
