package com.fm.module.alarm.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.dto.AlarmCallDTO;
import com.fm.module.alarm.dto.AlarmDispatchDTO;
import com.fm.module.alarm.dto.AlarmMerchantHandleDTO;
import com.fm.module.alarm.dto.AlarmQueryDTO;
import com.fm.module.alarm.dto.AlarmVerifyDTO;
import com.fm.module.alarm.entity.FireAlarmTrace;
import com.fm.module.alarm.mq.DeviceMessage;
import com.fm.module.alarm.vo.AlarmDispatchOptionVO;
import com.fm.module.alarm.vo.AlarmKpiVO;
import com.fm.module.alarm.vo.AlarmMerchantVO;
import com.fm.module.alarm.vo.AlarmVO;

import java.util.List;

/**
 * 报警域服务：MQ 入库、查询、商户处置、值守复核、派单、转隐患与通话记录。
 */
public interface AlarmService {

    void handleAlarm(DeviceMessage message);

    void handleRecover(DeviceMessage message);

    Page<AlarmVO> pageVO(AlarmQueryDTO query);

    AlarmVO detail(Long id);

    List<FireAlarmTrace> timeline(Long alarmId);

    /** 值守核警 / 商户火警复核 */
    void verify(AlarmVerifyDTO request);

    /** 商户处置：火警进入待复核，误报直接收口；故障报警不受理，由转隐患流程处理 */
    void merchantHandle(AlarmMerchantHandleDTO request);

    void dispatch(AlarmDispatchDTO request);

    /** 人工转隐患，返回隐患 id */
    Long convertDanger(Long alarmId);

    /** 登记电话告警/电话核警结果：写通话记录与报警追踪表，不改变报警结论 */
    void call(AlarmCallDTO request);

    AlarmKpiVO kpi();

    /** keyword 可为空；unverifiedOnly 为 true 时只返回有未核警报警的企业 */
    List<AlarmMerchantVO> merchantList(String keyword, Boolean unverifiedOnly);

    List<AlarmDispatchOptionVO> dispatchOptions();
}
