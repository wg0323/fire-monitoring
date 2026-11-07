package com.fm.module.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.inspection.entity.Plan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 巡查计划 Mapper。
 */
@Mapper
public interface PlanMapper extends BaseMapper<Plan> {
}
