package com.fm.module.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单 Mapper。
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
}
