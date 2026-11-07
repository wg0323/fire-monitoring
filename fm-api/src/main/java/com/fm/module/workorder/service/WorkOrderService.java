package com.fm.module.workorder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.workorder.dto.WorkOrderFinishDTO;
import com.fm.module.workorder.dto.WorkOrderQueryDTO;
import com.fm.module.workorder.vo.WorkOrderVO;

/**
 * 工单服务：只由派单生成，外勤接单/完成，完成时收口报警。
 */
public interface WorkOrderService {

    Page<WorkOrderVO> pageVO(WorkOrderQueryDTO query);

    /**
     * 小程序工单列表：外勤只看自己的，管理员看本租户。
     */
    Page<WorkOrderVO> mpPage(WorkOrderQueryDTO query);

    /**
     * 小程序首页待处理工单数：待接单 + 处理中，范围与 mpPage 一致。
     */
    long countMpTodo();

    WorkOrderVO detailVO(Long id);

    void createFromAlarm(Long tenantId, Long merchantId, Long alarmId, Long workAreaId,
                         String title, String content, Integer priority, Long handlerId, String handlerName);

    void accept(Long id);

    void finish(WorkOrderFinishDTO request);
}
