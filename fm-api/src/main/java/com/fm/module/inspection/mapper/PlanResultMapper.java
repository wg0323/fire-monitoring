package com.fm.module.inspection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.inspection.entity.PlanResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 巡查结果 Mapper。
 */
@Mapper
public interface PlanResultMapper extends BaseMapper<PlanResult> {

    @Select("SELECT COUNT(DISTINCT merchant_id) FROM b_plan_result WHERE plan_id = #{planId} AND deleted = 0")
    long countFinished(@Param("planId") Long planId);

    @Select("SELECT COUNT(DISTINCT merchant_id) FROM b_plan_result "
            + "WHERE plan_id = #{planId} AND result = 1 AND deleted = 0")
    long countDanger(@Param("planId") Long planId);
}
