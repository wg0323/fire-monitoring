package com.fm.module.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.monitor.entity.WaterHistory;
import com.fm.module.monitor.vo.WaterTrendRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 水压/液位历史数据 Mapper。
 */
@Mapper
public interface WaterHistoryMapper extends BaseMapper<WaterHistory> {

    /**
     * 按时间桶聚合历史数据，format 由服务端按周/月/年生成。
     * end 非空时追加时间上界，缩小 DATE_FORMAT 函数分组需要扫描的历史数据范围。
     */
    String TREND_SQL = "<script>SELECT DATE_FORMAT(f_measure_time, #{format}) AS bucket, "
            + "MAX(f_measure_type) AS measure_type, "
            + "AVG(f_measure_value) AS avg_value, MAX(f_measure_value) AS max_value, "
            + "MIN(f_measure_value) AS min_value "
            + "FROM b_water_history WHERE f_sn = #{sn} AND f_measure_time >= #{start} "
            + "<if test='end != null'>AND f_measure_time &lt; #{end} </if>"
            + "GROUP BY DATE_FORMAT(f_measure_time, #{format}) ORDER BY bucket</script>";

    /**
     * 兼容既有 3 参调用方：不传时间上界，查询行为与改造前一致。
     */
    default List<WaterTrendRow> selectTrend(String sn, String format, LocalDateTime start) {
        return selectTrend(sn, format, start, null);
    }

    /**
     * 带时间上界的聚合查询；end 为空时退化为只按下界过滤。
     */
    @Select(TREND_SQL)
    List<WaterTrendRow> selectTrend(@Param("sn") String sn, @Param("format") String format,
                                    @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
