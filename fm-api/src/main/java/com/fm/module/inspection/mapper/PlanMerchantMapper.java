package com.fm.module.inspection.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.inspection.vo.PlanMerchantVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 计划商户分页：工作区商户与计划内最新巡查结果联查，未巡查商户优先排序。
 */
@Mapper
public interface PlanMerchantMapper {

    /**
     * 租户条件由调用方显式传入，故跳过租户插件改写，避免派生表与多表关联被重复加条件。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select({"<script>",
            "SELECT m.id, m.name, m.addr,",
            "       (lr.latest_id IS NOT NULL) AS finished,",
            "       r.result AS last_result, r.patrol_by AS last_patrol_by,",
            "       r.patrol_time AS last_patrol_time, r.danger_id AS danger_id",
            "FROM sys_work_area_merchant wam",
            "INNER JOIN b_merchant m ON m.id = wam.merchant_id",
            "LEFT JOIN (SELECT merchant_id, MAX(id) AS latest_id FROM b_plan_result",
            "           WHERE plan_id = #{planId} AND deleted = 0 GROUP BY merchant_id) lr",
            "       ON lr.merchant_id = m.id",
            "LEFT JOIN b_plan_result r ON r.id = lr.latest_id",
            "WHERE wam.work_area_id = #{workAreaId}",
            "  AND m.tenant_id = #{tenantId}",
            "  AND m.status = 1 AND m.deleted = 0",
            "<if test=\"name != null and name != ''\">",
            "  AND m.name LIKE CONCAT('%', #{name}, '%')",
            "</if>",
            "<if test=\"patrolStatus != null\">",
            "  <choose>",
            "    <when test=\"patrolStatus == 0\"> AND lr.latest_id IS NULL</when>",
            "    <when test=\"patrolStatus == 1\"> AND r.result = 0</when>",
            "    <otherwise> AND r.result = 1</otherwise>",
            "  </choose>",
            "</if>",
            "ORDER BY (lr.latest_id IS NULL) DESC, m.id",
            "</script>"})
    Page<PlanMerchantVO> selectPageByPlan(Page<PlanMerchantVO> page,
                                          @Param("planId") Long planId,
                                          @Param("tenantId") Long tenantId,
                                          @Param("workAreaId") Long workAreaId,
                                          @Param("name") String name,
                                          @Param("patrolStatus") Integer patrolStatus);
}
