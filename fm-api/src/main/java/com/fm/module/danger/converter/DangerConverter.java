package com.fm.module.danger.converter;

import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.entity.DangerResult;
import com.fm.module.danger.vo.DangerAssigneeVO;
import com.fm.module.danger.vo.DangerVO;
import com.fm.module.system.entity.SysUser;
import org.springframework.util.StringUtils;

/**
 * 隐患对象转换。
 */
public final class DangerConverter {

    private DangerConverter() {
    }

    public static DangerAssigneeVO toAssigneeVO(SysUser user, String name) {
        DangerAssigneeVO vo = new DangerAssigneeVO();
        vo.setId(user.getId());
        vo.setName(name);
        vo.setPhone(user.getPhone());
        return vo;
    }

    public static DangerVO toVO(Danger danger, String merchantName, String workAreaName, DangerResult lastDeal) {
        if (danger == null) {
            return null;
        }
        DangerVO vo = new DangerVO();
        vo.setId(danger.getId());
        vo.setMerchantId(danger.getMerchantId());
        vo.setMerchantName(merchantName);
        vo.setAlarmId(danger.getAlarmId());
        vo.setSourceAlarmId(danger.getSourceAlarmId());
        vo.setSourceType(danger.getSourceType());
        vo.setDangerType(danger.getDangerType());
        vo.setRemark(danger.getRemark());
        vo.setPosition(danger.getPosition());
        vo.setPositionDetail(danger.getPositionDetail());
        vo.setPic1(danger.getPic1());
        vo.setPic2(danger.getPic2());
        vo.setPic3(danger.getPic3());
        vo.setDangerLevel(danger.getDangerLevel());
        vo.setDangerLevelFinal(danger.getDangerLevelFinal());
        vo.setStatus(danger.getStatus());
        vo.setCreateBy(danger.getCreateBy());
        vo.setCreateTime(danger.getCreateTime());
        vo.setAssigneeType(danger.getAssigneeType());
        vo.setAssigneeId(danger.getAssigneeId());
        vo.setAssigneeName(danger.getAssigneeName());
        vo.setWorkAreaId(danger.getWorkAreaId());
        vo.setWorkAreaName(workAreaName);
        vo.setAlarmTime(danger.getAlarmTime());
        vo.setConvertTime(danger.getConvertTime());
        vo.setAssignBy(danger.getAssignBy());
        vo.setAssignTime(danger.getAssignTime());
        vo.setDealLimit(danger.getDealLimit());
        vo.setDealBy(danger.getDealBy());
        vo.setDealTime(danger.getDealTime());
        vo.setAuditBy(danger.getAuditBy());
        vo.setAuditByName(danger.getAuditByName());
        vo.setAuditTime(danger.getAuditTime());
        vo.setAuditResult(danger.getAuditResult());
        vo.setAuditRemark(danger.getAuditRemark());
        if (lastDeal != null) {
            vo.setLastDealUserName(StringUtils.hasText(lastDeal.getDealUserName())
                    ? lastDeal.getDealUserName() : lastDeal.getDealBy());
            vo.setLastDealTime(lastDeal.getDealTime());
            vo.setLastDealRemark(lastDeal.getRemark());
            vo.setLastDealPic1(lastDeal.getPic1());
            vo.setLastDealPic2(lastDeal.getPic2());
            vo.setLastDealPic3(lastDeal.getPic3());
        }
        return vo;
    }
}
