package com.fm.module.alarm.service;

import com.fm.module.alarm.entity.FireAlarm;

import java.time.LocalDateTime;

/**
 * 报警领域方法：收口、故障转隐患、1 小时内自行恢复。
 * 人工操作与定时任务共用同一入口，保证状态与幂等规则只实现一次。
 */
public interface AlarmDomainService {

    /**
     * 统一收口：status=1、verify_status=2，写结论、来源、收口人与时间。
     * 已收口的报警再次提交直接拒绝。
     */
    void finalizeAlarm(Long alarmId, Integer result, String source, Long operatorId, String operatorName,
                       String remark, String pic1, String pic2, String pic3);

    /**
     * 故障转隐患：人工与定时任务共用；source_alarm_id 唯一 + danger_id 条件更新保证只生成一条。
     *
     * @param convertSource MANUAL / AUTO
     * @return 隐患 id
     */
    Long convertDanger(Long alarmId, String convertSource, Long operatorId, String operatorName);

    /**
     * 1 小时内自行恢复：收口为故障、清空自动转截止时间，不生成隐患。
     *
     * @return true=本次完成收口
     */
    boolean recoverInWindow(Long alarmId);

    /**
     * 追加报警时间线。
     */
    void appendTrace(FireAlarm alarm, int traceType, Integer result, String content, Object detail);

    /**
     * 故障判定：设备上报故障（alarm_level=3）或人工判定故障（result=2）；已判误报的不再视为故障。
     */
    static boolean isFault(FireAlarm alarm) {
        if (alarm == null) {
            return false;
        }
        if (Integer.valueOf(FireAlarm.RESULT_FALSE).equals(alarm.getResult())) {
            return false;
        }
        return Integer.valueOf(3).equals(alarm.getAlarmLevel())
                || Integer.valueOf(FireAlarm.RESULT_FAULT).equals(alarm.getResult());
    }

    /**
     * 是否在 1 小时内自行恢复：recover_time <= danger_convert_deadline。
     */
    static boolean isSelfRecovered(FireAlarm alarm) {
        return alarm != null && alarm.getDangerConvertDeadline() != null && alarm.getRecoverTime() != null
                && !alarm.getRecoverTime().isAfter(alarm.getDangerConvertDeadline());
    }

    /**
     * 定时任务是否应自动转隐患：已到期、未转、且未在窗口内恢复。
     * 历史报警 danger_convert_deadline 为空，不参与自动转。
     */
    static boolean shouldAutoConvert(FireAlarm alarm, LocalDateTime now) {
        return isFault(alarm)
                && alarm.getDangerId() == null
                && alarm.getDangerConvertDeadline() != null
                && !alarm.getDangerConvertDeadline().isAfter(now)
                && (alarm.getRecoverTime() == null || alarm.getRecoverTime().isAfter(alarm.getDangerConvertDeadline()));
    }
}
