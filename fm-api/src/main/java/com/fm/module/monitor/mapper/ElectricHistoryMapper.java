package com.fm.module.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.monitor.entity.ElectricHistory;
import com.fm.module.monitor.vo.ElectricTrendRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 电气火灾历史数据 Mapper。
 */
@Mapper
public interface ElectricHistoryMapper extends BaseMapper<ElectricHistory> {

    /**
     * 按时间桶聚合 A 相核心指标，format 由服务端按周/月/年生成。
     * end 非空时追加时间上界，缩小 DATE_FORMAT 函数分组需要扫描的历史数据范围。
     */
    String TREND_SQL = "<script>SELECT DATE_FORMAT(f_measure_time, #{format}) AS bucket, "
            + "AVG(f_voltage_a) AS voltage_avg, MAX(f_voltage_a) AS voltage_max, "
            + "MIN(f_voltage_a) AS voltage_min, "
            + "AVG(f_current_a) AS current_avg, MAX(f_current_a) AS current_max, "
            + "MIN(f_current_a) AS current_min, "
            + "AVG(f_residual_current) AS residual_avg, MAX(f_residual_current) AS residual_max, "
            + "MIN(f_residual_current) AS residual_min, "
            + "AVG(f_temperature) AS temperature_avg, MAX(f_temperature) AS temperature_max, "
            + "MIN(f_temperature) AS temperature_min "
            + "FROM b_electric_history WHERE f_sn = #{sn} AND f_measure_time >= #{start} "
            + "<if test='end != null'>AND f_measure_time &lt; #{end} </if>"
            + "GROUP BY DATE_FORMAT(f_measure_time, #{format}) ORDER BY bucket</script>";

    /**
     * 兼容既有 3 参调用方：不传时间上界，查询行为与改造前一致。
     */
    default List<ElectricTrendRow> selectTrend(String sn, String format, LocalDateTime start) {
        return selectTrend(sn, format, start, null);
    }

    /**
     * 带时间上界的聚合查询；end 为空时退化为只按下界过滤。
     */
    @Select(TREND_SQL)
    List<ElectricTrendRow> selectTrend(@Param("sn") String sn, @Param("format") String format,
                                       @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
