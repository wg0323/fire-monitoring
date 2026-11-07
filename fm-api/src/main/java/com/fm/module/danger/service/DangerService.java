package com.fm.module.danger.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.danger.dto.DangerAssignDTO;
import com.fm.module.danger.dto.DangerAuditDTO;
import com.fm.module.danger.dto.DangerBatchAssignDTO;
import com.fm.module.danger.dto.DangerDealDTO;
import com.fm.module.danger.dto.DangerQueryDTO;
import com.fm.module.danger.dto.DangerSaveDTO;
import com.fm.module.danger.vo.DangerAssigneeVO;
import com.fm.module.danger.vo.DangerBatchAssignResultVO;
import com.fm.module.danger.vo.DangerTimelineVO;
import com.fm.module.danger.vo.DangerVO;

import java.util.List;

/**
 * 隐患闭环：上报 → 指派 → 整改 → 审核。
 */
public interface DangerService {

    /**
     * 随手拍上报：来源 0，直接指派给商户，进入待整改。
     */
    Long report(DangerSaveDTO request);

    /**
     * 巡查发现：来源 1，直接指派给商户，进入待整改。
     */
    Long reportInspection(DangerSaveDTO request);

    /**
     * 故障转隐患指派：待指派 → 待整改，责任人必须为隐患工作区外勤。
     */
    void assign(Long id, DangerAssignDTO request);

    /**
     * 批量指派：逐条独立事务，返回成功数与失败明细，成功记录合并发一条通知。
     */
    DangerBatchAssignResultVO batchAssign(DangerBatchAssignDTO request);

    /**
     * 责任人提交整改：待整改 → 待审核。
     */
    void deal(Long id, DangerDealDTO request);

    /**
     * 租户管理员审核：通过 → 已完成；驳回 → 退回待整改。
     */
    void audit(Long id, DangerAuditDTO request);

    Page<DangerVO> page(DangerQueryDTO query);

    DangerVO detail(Long id);

    List<DangerTimelineVO> timeline(Long id);

    /**
     * 可指派外勤：限定隐患所属工作区。
     */
    List<DangerAssigneeVO> assigneeOptions(Long dangerId);
}
