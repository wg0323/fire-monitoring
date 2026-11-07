package com.fm.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.vo.AlarmMerchantVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报警主表 Mapper。
 */
@Mapper
public interface FireAlarmMapper extends BaseMapper<FireAlarm> {

    /**
     * 未处理报警按商户聚合，用于火警处置左侧商户列表。
     */
    @Select("SELECT merchant_id AS id, COUNT(*) AS pendingCount, MIN(alarm_level) AS maxLevel "
            + "FROM b_fire_alarm WHERE status = 0 AND merchant_id IS NOT NULL GROUP BY merchant_id")
    List<AlarmMerchantVO> countPendingByMerchant();

    /**
     * 未核警报警按商户聚合，用于火警处置左侧只显示有待核警报警的商户。
     */
    @Select("SELECT merchant_id AS id, COUNT(*) AS pendingCount, MIN(alarm_level) AS maxLevel "
            + "FROM b_fire_alarm WHERE verify_status <> " + FireAlarm.VERIFY_DONE
            + " AND merchant_id IS NOT NULL GROUP BY merchant_id")
    List<AlarmMerchantVO> countUnverifiedByMerchant();

    /**
     * 今日已收口火警的平均响应秒数。
     */
    @Select("SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, alarm_time, finalize_time)), 0) "
            + "FROM b_fire_alarm WHERE alarm_level = 1 AND status = 1 "
            + "AND finalize_time >= #{start} AND finalize_time < #{end}")
    Double avgResponseSeconds(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
