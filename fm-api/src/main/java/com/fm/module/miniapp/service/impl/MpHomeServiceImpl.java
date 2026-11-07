package com.fm.module.miniapp.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fm.common.context.TenantContext;
import com.fm.module.alarm.dto.AlarmQueryDTO;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.service.AlarmService;
import com.fm.module.alarm.vo.AlarmVO;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.inspection.service.PlanService;
import com.fm.module.miniapp.MpConstants;
import com.fm.module.miniapp.converter.MiniappConverter;
import com.fm.module.miniapp.service.MpHomeService;
import com.fm.module.miniapp.vo.MpHomeSummaryVO;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.service.DataScopeService;
import com.fm.module.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小程序首页实现：KPI 与最新列表按登录角色收窄。
 */
@Service
@RequiredArgsConstructor
public class MpHomeServiceImpl implements MpHomeService {

    private static final int LATEST_SIZE = 3;

    private final AlarmService alarmService;
    private final FireAlarmMapper alarmMapper;
    private final DangerMapper dangerMapper;
    private final DeviceMapper deviceMapper;
    private final MerchantMapper merchantMapper;
    private final DataScopeService dataScopeService;
    private final WorkOrderService workOrderService;
    private final PlanService planService;

    @Override
    public MpHomeSummaryVO summary() {
        return MiniappConverter.toSummaryVO(pendingAlarmCount(), openDangerCount(), deviceCount(),
                offlineDeviceCount(), workOrderService.countMpTodo(), planService.countOngoingUnfinished(),
                latestAlarms(), latestDangers());
    }

    private Long pendingAlarmCount() {
        List<Long> merchantIds = scopedMerchantIds();
        if (merchantIds != null && merchantIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<FireAlarm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FireAlarm::getStatus, FireAlarm.STATUS_PENDING);
        if (merchantIds != null) {
            wrapper.in(FireAlarm::getMerchantId, merchantIds);
        }
        return alarmMapper.selectCount(wrapper);
    }

    private Long openDangerCount() {
        LambdaQueryWrapper<Danger> wrapper = dangerScopeWrapper();
        wrapper.in(Danger::getStatus, Danger.STATUS_PENDING_ASSIGN, Danger.STATUS_PENDING_DEAL,
                Danger.STATUS_PENDING_AUDIT, Danger.STATUS_REJECTED);
        return dangerMapper.selectCount(wrapper);
    }

    private Long deviceCount() {
        List<Long> merchantIds = scopedMerchantIds();
        if (merchantIds != null && merchantIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getStatus, 1);
        if (merchantIds != null) {
            wrapper.in(Device::getMerchantId, merchantIds);
        }
        return deviceMapper.selectCount(wrapper);
    }

    private Long offlineDeviceCount() {
        List<Long> merchantIds = scopedMerchantIds();
        if (merchantIds != null && merchantIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getStatus, 1).eq(Device::getOnlineStatus, 0);
        if (merchantIds != null) {
            wrapper.in(Device::getMerchantId, merchantIds);
        }
        return deviceMapper.selectCount(wrapper);
    }

    private List<AlarmVO> latestAlarms() {
        AlarmQueryDTO query = new AlarmQueryDTO();
        query.setPageNum(1);
        query.setPageSize(LATEST_SIZE);
        return alarmService.pageVO(query).getRecords();
    }

    private List<MpHomeSummaryVO.DangerBrief> latestDangers() {
        LambdaQueryWrapper<Danger> wrapper = dangerScopeWrapper();
        wrapper.orderByDesc(Danger::getCreateTime).last("limit " + LATEST_SIZE);
        List<Danger> dangers = dangerMapper.selectList(wrapper);
        if (dangers.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> merchantIds = dangers.stream().map(Danger::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> merchantNames = merchantIds.isEmpty() ? Collections.emptyMap()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
        return dangers.stream().map(danger -> {
            MpHomeSummaryVO.DangerBrief brief = new MpHomeSummaryVO.DangerBrief();
            brief.setId(danger.getId() == null ? null : String.valueOf(danger.getId()));
            brief.setRemark(danger.getRemark());
            brief.setMerchantName(merchantNames.get(danger.getMerchantId()));
            brief.setStatus(danger.getStatus());
            brief.setSourceType(danger.getSourceType());
            brief.setCreateTime(danger.getCreateTime());
            return brief;
        }).collect(Collectors.toList());
    }

    /**
     * 外勤限定工作区商户；null 表示不额外收窄，空列表表示没有可见商户。
     */
    private List<Long> scopedMerchantIds() {
        if (!SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            return null;
        }
        return dataScopeService.workAreaMerchantIds(StpUtil.getLoginIdAsLong());
    }

    /**
     * 隐患数据范围：商户看本店，外勤看指派给本人的，管理员/值守看本租户。
     */
    private LambdaQueryWrapper<Danger> dangerScopeWrapper() {
        LambdaQueryWrapper<Danger> wrapper = new LambdaQueryWrapper<>();
        if (MpConstants.USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            Long merchantId = TenantContext.getMerchantId();
            wrapper.eq(Danger::getMerchantId, merchantId == null ? -1L : merchantId);
        } else if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            wrapper.eq(Danger::getAssigneeType, Danger.ASSIGNEE_FIELD)
                    .eq(Danger::getAssigneeId, StpUtil.getLoginIdAsLong());
        }
        return wrapper;
    }
}
