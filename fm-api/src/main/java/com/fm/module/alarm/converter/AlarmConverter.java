package com.fm.module.alarm.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.entity.FireAlarmTrace;
import com.fm.module.alarm.vo.AlarmDispatchOptionVO;
import com.fm.module.alarm.vo.AlarmKpiVO;
import com.fm.module.alarm.vo.AlarmMerchantVO;
import com.fm.module.alarm.vo.AlarmTraceVO;
import com.fm.module.alarm.vo.AlarmVO;
import com.fm.module.device.entity.Merchant;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysWorkArea;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 报警对象转换。
 */
public final class AlarmConverter {

    private AlarmConverter() {
    }

    public static AlarmVO toVO(FireAlarm alarm) {
        if (alarm == null) {
            return null;
        }
        AlarmVO vo = new AlarmVO();
        BeanUtils.copyProperties(alarm, vo);
        return vo;
    }

    public static Page<AlarmVO> toVOPage(Page<FireAlarm> page) {
        Page<AlarmVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(AlarmConverter::toVO).collect(Collectors.toList()));
        return result;
    }

    public static List<AlarmTraceVO> toTraceVOList(List<FireAlarmTrace> traces) {
        return traces.stream().map(AlarmConverter::toTraceVO).collect(Collectors.toList());
    }

    public static AlarmTraceVO toTraceVO(FireAlarmTrace trace) {
        if (trace == null) {
            return null;
        }
        AlarmTraceVO vo = new AlarmTraceVO();
        BeanUtils.copyProperties(trace, vo);
        return vo;
    }

    public static AlarmKpiVO toKpiVO(Long unhandledFire, Long pendingReview, Long verifying,
                                     Long todayHandled, Double avgResponseMinutes) {
        AlarmKpiVO vo = new AlarmKpiVO();
        vo.setUnhandledFire(unhandledFire);
        vo.setPendingReview(pendingReview);
        vo.setVerifying(verifying);
        vo.setTodayHandled(todayHandled);
        vo.setAvgResponseMinutes(avgResponseMinutes);
        return vo;
    }

    public static AlarmMerchantVO toMerchantVO(Merchant merchant, AlarmMerchantVO stat) {
        AlarmMerchantVO vo = new AlarmMerchantVO();
        vo.setId(merchant.getId());
        vo.setName(merchant.getName());
        vo.setAddr(merchant.getAddr());
        vo.setStatus(merchant.getStatus());
        vo.setPendingCount(stat == null ? 0L : stat.getPendingCount());
        vo.setMaxLevel(stat == null ? null : stat.getMaxLevel());
        return vo;
    }

    public static AlarmDispatchOptionVO toDispatchOptionVO(SysWorkArea area,
                                                           List<AlarmDispatchOptionVO.Handler> handlers) {
        AlarmDispatchOptionVO vo = new AlarmDispatchOptionVO();
        vo.setWorkAreaId(area.getId());
        vo.setWorkAreaName(area.getName());
        vo.setHandlers(handlers);
        return vo;
    }

    public static AlarmDispatchOptionVO.Handler toHandler(SysUser user) {
        AlarmDispatchOptionVO.Handler handler = new AlarmDispatchOptionVO.Handler();
        handler.setId(user.getId());
        handler.setName(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUserName());
        handler.setPhone(user.getPhone());
        return handler;
    }
}
