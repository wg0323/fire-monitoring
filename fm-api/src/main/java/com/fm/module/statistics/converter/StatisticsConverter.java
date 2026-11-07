package com.fm.module.statistics.converter;

import com.fm.module.alarm.entity.CallRecord;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.statistics.vo.StatAlarmItemVO;
import com.fm.module.statistics.vo.StatCallItemVO;
import com.fm.module.statistics.vo.StatFaultItemVO;

/**
 * 统计分析列表项转换。
 */
public final class StatisticsConverter {

    private StatisticsConverter() {
    }

    public static StatAlarmItemVO toAlarmItemVO(FireAlarm alarm, String merchantName) {
        StatAlarmItemVO item = new StatAlarmItemVO();
        item.setId(alarm.getId());
        item.setSn(alarm.getSn());
        item.setMerchantName(merchantName);
        item.setPosition(alarm.getPosition());
        item.setAlarmTime(alarm.getAlarmTime());
        item.setHandleSource(alarm.getHandleSource());
        item.setResult(alarm.getResult());
        item.setStatus(alarm.getStatus());
        return item;
    }

    public static StatFaultItemVO toFaultItemVO(FireAlarm alarm, String merchantName, String alarmTypeName) {
        StatFaultItemVO item = new StatFaultItemVO();
        item.setId(alarm.getId());
        item.setSn(alarm.getSn());
        item.setMerchantName(merchantName);
        item.setPosition(alarm.getPosition());
        item.setAlarmTime(alarm.getAlarmTime());
        item.setAlarmTypeName(alarmTypeName);
        item.setHandleSource(alarm.getHandleSource());
        item.setResult(alarm.getResult());
        item.setStatus(alarm.getStatus());
        return item;
    }

    public static StatCallItemVO toCallItemVO(CallRecord call, FireAlarm alarm, String merchantName,
                                              String alarmTypeName, String fileUrl) {
        StatCallItemVO item = new StatCallItemVO();
        item.setId(call.getId());
        item.setMerchantName(merchantName);
        item.setAlarmTypeName(alarmTypeName);
        item.setAlarmTime(alarm == null ? null : alarm.getAlarmTime());
        item.setVerifyStatus(alarm == null ? null : alarm.getVerifyStatus());
        item.setCallBegin(call.getCallBegin());
        item.setCalledNo(call.getCalledNo());
        item.setFileUrl(fileUrl);
        return item;
    }
}
