package com.fm.module.workorder.converter;

import com.fm.module.workorder.entity.WorkOrder;
import com.fm.module.workorder.vo.WorkOrderVO;
import org.springframework.beans.BeanUtils;

/**
 * 工单对象转换。
 */
public final class WorkOrderConverter {

    private WorkOrderConverter() {
    }

    public static WorkOrderVO toVO(WorkOrder order) {
        if (order == null) {
            return null;
        }
        WorkOrderVO vo = new WorkOrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }
}
