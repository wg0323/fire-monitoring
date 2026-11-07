package com.fm.module.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.monitor.service.DashboardService;
import com.fm.module.monitor.vo.MerchantStatusPointVO;
import com.fm.module.monitor.vo.ScreenAlarmItemVO;
import com.fm.module.monitor.vo.ScreenAlarmVO;
import com.fm.module.monitor.vo.ScreenComprehensiveVO;
import com.fm.module.monitor.vo.ScreenDangerItemVO;
import com.fm.module.monitor.vo.ScreenDangerVO;
import com.fm.module.monitor.vo.ScreenDeviceTypeStatVO;
import com.fm.module.monitor.vo.ScreenDeviceVO;
import com.fm.module.monitor.vo.ScreenFaultItemVO;
import com.fm.module.monitor.vo.ScreenTrendPointVO;
import com.fm.module.monitor.vo.ScreenTypeCountVO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
import com.fm.module.inspection.entity.Plan;
import com.fm.module.inspection.entity.PlanResult;
import com.fm.module.inspection.mapper.PlanMapper;
import com.fm.module.inspection.mapper.PlanResultMapper;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 大屏聚合：按 30 秒轮询频率直查聚合 SQL，不引入缓存；数据范围与后台列表一致。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_ALARM = 1;
    private static final int STATUS_FAULT = 2;
    private static final int STATUS_OFFLINE = 3;
    private static final int STATUS_NOT_CONNECTED = 4;

    private static final int ENABLED = 1;
    private static final int ONLINE = 1;
    /** 大屏滚动列表的截断条数：只取最新 N 条用于轮询展示，不是分页，超出部分不返回 */
    private static final int SCROLL_LIMIT = 20;
    private static final String RANGE_YEAR = "year";
    private static final String RANGE_MONTH = "month";
    private static final String BUCKET_ALIAS = "bucket";
    private static final String VALUE_ALIAS = "value";

    private final FireAlarmMapper alarmMapper;
    private final DeviceMapper deviceMapper;
    private final DangerMapper dangerMapper;
    private final MerchantMapper merchantMapper;
    private final PlanMapper planMapper;
    private final PlanResultMapper planResultMapper;
    private final DictService dictService;
    private final DataScopeService dataScopeService;

    @Override
    public ScreenComprehensiveVO comprehensive() {
        Scope scope = scope();
        if (scope.isEmpty()) {
            return emptyComprehensive();
        }
        ScreenComprehensiveVO vo = emptyComprehensive();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDate todayDate = LocalDate.now();
        vo.setFireTotal(alarmCount(scope, w -> w.eq(FireAlarm::getAlarmLevel, 1)
                .ge(FireAlarm::getAlarmTime, today)));
        vo.setFireHandled(alarmCount(scope, w -> w.eq(FireAlarm::getAlarmLevel, 1)
                .ge(FireAlarm::getAlarmTime, today).eq(FireAlarm::getStatus, FireAlarm.STATUS_HANDLED)));
        vo.setFireTrue(alarmCount(scope, w -> w.eq(FireAlarm::getAlarmLevel, 1)
                .ge(FireAlarm::getAlarmTime, today).eq(FireAlarm::getResult, FireAlarm.RESULT_TRUE)));
        vo.setDangerUnhandled(dangerCount(scope, w -> w.in(Danger::getStatus,
                Danger.STATUS_PENDING_ASSIGN, Danger.STATUS_PENDING_DEAL,
                Danger.STATUS_PENDING_AUDIT, Danger.STATUS_REJECTED)));
        vo.setDangerToday(dangerCount(scope, w -> w.ge(Danger::getCreateTime, today)));
        vo.setDangerOverdue(dangerCount(scope, w -> w.ne(Danger::getStatus, Danger.STATUS_DONE)
                .isNotNull(Danger::getDealLimit).lt(Danger::getDealLimit, todayDate)));

        List<Device> devices = devices(scope);
        List<FireAlarm> unresolvedAlarms = unresolvedAlarms(scope);
        Map<String, Integer> alarmKinds = unresolvedAlarmKinds(unresolvedAlarms);
        Map<Long, Integer> merchantStatuses = merchantStatuses(devices, alarmKinds);
        long patrolMerchants = patrolMerchants(scope, today);
        long merchantTotal = merchantCount(scope);
        vo.setPatrolMerchants(patrolMerchants);
        vo.setPatrolPercent(percent(patrolMerchants, merchantTotal));
        vo.setAccessDevices(devices.stream()
                .filter(device -> Integer.valueOf(ENABLED).equals(device.getStatus())).count());
        vo.setAccessMerchants(accessMerchants(scope, devices));
        fillDeviceCounts(vo, devices, alarmKinds);
        vo.setMerchantPoints(merchantPoints(scope, merchantStatuses, merchantAlarmTimes(devices, unresolvedAlarms)));
        return vo;
    }

    @Override
    public ScreenAlarmVO alarm(String range) {
        Scope scope = scope();
        if (scope.isEmpty()) {
            return emptyAlarm();
        }
        ScreenAlarmVO vo = emptyAlarm();
        vo.setTrend(alarmTrend(scope, range));
        List<FireAlarm> rows = latestAlarms(scope, FaultFilter.ALL);
        Map<Long, Merchant> merchants = merchants(scope, merchantIds(rows));
        vo.setAlarms(rows.stream().map(row -> alarmItem(row, merchants.get(row.getMerchantId())))
                .collect(Collectors.toList()));
        List<Device> devices = devices(scope);
        List<FireAlarm> unresolvedAlarms = unresolvedAlarms(scope);
        vo.setMerchantPoints(merchantPoints(scope,
                merchantStatuses(devices, unresolvedAlarmKinds(unresolvedAlarms)),
                merchantAlarmTimes(devices, unresolvedAlarms)));
        return vo;
    }

    @Override
    public ScreenDangerVO danger(String range) {
        Scope scope = scope();
        if (scope.isEmpty()) {
            return emptyDanger();
        }
        ScreenDangerVO vo = emptyDanger();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthBegin = monthStart.atStartOfDay();
        LocalDateTime nextMonth = monthStart.plusMonths(1).atStartOfDay();
        LocalDate today = LocalDate.now();
        long total = dangerCount(scope, w -> w.ge(Danger::getCreateTime, monthBegin)
                .lt(Danger::getCreateTime, nextMonth));
        long done = dangerCount(scope, w -> w.ge(Danger::getCreateTime, monthBegin)
                .lt(Danger::getCreateTime, nextMonth).eq(Danger::getStatus, Danger.STATUS_DONE));
        vo.setMonthTotal(total);
        vo.setMonthDone(done);
        vo.setMonthUnfinished(total - done);
        vo.setMonthOverdue(dangerCount(scope, w -> w.ne(Danger::getStatus, Danger.STATUS_DONE)
                .isNotNull(Danger::getDealLimit).lt(Danger::getDealLimit, today)));
        vo.setTrend(dangerTrend(scope, range));

        LambdaQueryWrapper<Danger> wrapper = dangerWrapper(scope);
        // 截断语义：大屏滚动列表只取最新 SCROLL_LIMIT 条隐患，不能用 selectPage 替代（会改变返回结构）
        wrapper.in(Danger::getStatus, Danger.STATUS_PENDING_ASSIGN, Danger.STATUS_PENDING_DEAL,
                        Danger.STATUS_PENDING_AUDIT, Danger.STATUS_REJECTED)
                .orderByDesc(Danger::getCreateTime).last("LIMIT " + SCROLL_LIMIT);
        List<Danger> dangers = scoped(() -> dangerMapper.selectList(wrapper));
        Map<Long, Merchant> merchants = merchants(scope, merchantIdsOfDanger(dangers));
        Map<String, String> typeNames = dictService.dangerTypeNames(dangers.stream()
                .map(Danger::getDangerType).filter(StringUtils::hasText).collect(Collectors.toSet()));
        vo.setDangers(dangers.stream().map(row -> dangerItem(row, merchants.get(row.getMerchantId()), typeNames))
                .collect(Collectors.toList()));

        vo.setPlanCount(planCount(scope, monthStart, monthStart.plusMonths(1).minusDays(1)));
        vo.setPlanDone(planDone(scope, monthStart, today));
        vo.setPlanDanger(planResultCount(scope, monthBegin, nextMonth));
        vo.setTypeRanking(dangerRanking(scope, monthBegin, nextMonth));
        List<Device> devices = devices(scope);
        List<FireAlarm> unresolvedAlarms = unresolvedAlarms(scope);
        vo.setMerchantPoints(merchantPoints(scope,
                merchantStatuses(devices, unresolvedAlarmKinds(unresolvedAlarms)),
                merchantAlarmTimes(devices, unresolvedAlarms)));
        return vo;
    }

    @Override
    public ScreenDeviceVO device(String range) {
        Scope scope = scope();
        if (scope.isEmpty()) {
            return emptyDevice();
        }
        ScreenDeviceVO vo = emptyDevice();
        List<Device> devices = devices(scope);
        List<FireAlarm> unresolvedAlarms = unresolvedAlarms(scope);
        Map<String, Integer> alarmKinds = unresolvedAlarmKinds(unresolvedAlarms);
        Map<String, String> typeNames = dictService.deviceTypeNames(devices.stream()
                .map(Device::getType).filter(StringUtils::hasText).collect(Collectors.toSet()));
        Map<String, ScreenDeviceTypeStatVO> stats = new LinkedHashMap<>();
        for (Device device : devices) {
            String type = StringUtils.hasText(device.getType()) ? device.getType() : "-";
            ScreenDeviceTypeStatVO stat = stats.computeIfAbsent(type, key -> {
                ScreenDeviceTypeStatVO item = new ScreenDeviceTypeStatVO();
                item.setType(key);
                item.setTypeName(typeNames.getOrDefault(key, key));
                item.setTotal(0L);
                item.setNormal(0L);
                item.setAlarm(0L);
                item.setFault(0L);
                item.setOffline(0L);
                return item;
            });
            stat.setTotal(stat.getTotal() + 1);
            int status = deviceStatus(device, alarmKinds);
            if (status == STATUS_ALARM) {
                stat.setAlarm(stat.getAlarm() + 1);
            } else if (status == STATUS_FAULT) {
                stat.setFault(stat.getFault() + 1);
            } else if (status == STATUS_OFFLINE) {
                stat.setOffline(stat.getOffline() + 1);
            } else {
                stat.setNormal(stat.getNormal() + 1);
            }
        }
        List<ScreenDeviceTypeStatVO> types = new ArrayList<>(stats.values());
        types.sort((left, right) -> Long.compare(right.getTotal(), left.getTotal()));
        types.forEach(this::fillTypePercents);
        vo.setTypes(types);
        vo.setFaultTrend(faultTrend(scope, range));
        vo.setFaults(faultItems(scope, devices));
        vo.setMerchantPoints(merchantPoints(scope,
                merchantStatuses(devices, alarmKinds),
                merchantAlarmTimes(devices, unresolvedAlarms)));
        return vo;
    }

    private List<ScreenFaultItemVO> faultItems(Scope scope, List<Device> devices) {
        List<FireAlarm> rows = latestAlarms(scope, FaultFilter.FAULT);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Device> deviceBySn = devices.stream()
                .filter(device -> device.getSn() != null)
                .collect(Collectors.toMap(Device::getSn, device -> device, (left, right) -> left));
        Map<Long, Merchant> merchants = merchants(scope, merchantIds(rows));
        return rows.stream().map(row -> {
            ScreenFaultItemVO item = new ScreenFaultItemVO();
            item.setId(row.getId());
            item.setDeviceSn(row.getSn());
            Device device = deviceBySn.get(row.getSn());
            item.setDeviceName(device == null ? null : device.getName());
            Merchant merchant = merchants.get(row.getMerchantId());
            item.setMerchantName(merchant == null ? null : merchant.getName());
            item.setPosition(row.getPosition());
            item.setAlarmTime(row.getAlarmTime());
            item.setStatus(row.getStatus());
            item.setRecoverTime(row.getRecoverTime());
            return item;
        }).collect(Collectors.toList());
    }

    private List<FireAlarm> latestAlarms(Scope scope, FaultFilter filter) {
        LambdaQueryWrapper<FireAlarm> wrapper = alarmWrapper(scope);
        if (filter == FaultFilter.FAULT) {
            wrapper.and(w -> w.eq(FireAlarm::getAlarmLevel, 3).or().eq(FireAlarm::getResult, 2));
        }
        // 截断语义：大屏滚动列表只取最新 SCROLL_LIMIT 条报警，不能用 selectPage 替代（会改变返回结构）
        wrapper.orderByDesc(FireAlarm::getAlarmTime).last("LIMIT " + SCROLL_LIMIT);
        return scoped(() -> alarmMapper.selectList(wrapper));
    }

    private List<ScreenTrendPointVO> alarmTrend(Scope scope, String range) {
        Window window = window(range);
        QueryWrapper<FireAlarm> wrapper = new QueryWrapper<>();
        wrapper.select("DATE_FORMAT(alarm_time, '" + window.getFormat() + "') AS " + BUCKET_ALIAS,
                        "COUNT(*) AS " + VALUE_ALIAS)
                .ge("alarm_time", window.getStart())
                .groupBy(BUCKET_ALIAS);
        applyAlarmScope(wrapper, scope);
        return fillBuckets(window, countMap(scoped(() -> alarmMapper.selectMaps(wrapper))));
    }

    private List<ScreenTrendPointVO> faultTrend(Scope scope, String range) {
        Window window = window(range);
        QueryWrapper<FireAlarm> wrapper = new QueryWrapper<>();
        wrapper.select("DATE_FORMAT(alarm_time, '" + window.getFormat() + "') AS " + BUCKET_ALIAS,
                        "COUNT(*) AS " + VALUE_ALIAS)
                .ge("alarm_time", window.getStart())
                .and(w -> w.eq("alarm_level", 3).or().eq("result", 2))
                .groupBy(BUCKET_ALIAS);
        applyAlarmScope(wrapper, scope);
        return fillBuckets(window, countMap(scoped(() -> alarmMapper.selectMaps(wrapper))));
    }

    private List<ScreenTrendPointVO> dangerTrend(Scope scope, String range) {
        Window window = window(range);
        QueryWrapper<Danger> wrapper = new QueryWrapper<>();
        wrapper.select("DATE_FORMAT(create_time, '" + window.getFormat() + "') AS " + BUCKET_ALIAS,
                        "COUNT(*) AS " + VALUE_ALIAS)
                .ge("create_time", window.getStart())
                .groupBy(BUCKET_ALIAS);
        applyDangerScope(wrapper, scope);
        return fillBuckets(window, countMap(scoped(() -> dangerMapper.selectMaps(wrapper))));
    }

    private Map<String, Long> countMap(List<Map<String, Object>> rows) {
        Map<String, Long> result = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object bucket = row.get(BUCKET_ALIAS);
            Object value = row.get(VALUE_ALIAS);
            if (bucket != null && value instanceof Number) {
                result.put(String.valueOf(bucket), ((Number) value).longValue());
            }
        }
        return result;
    }

    private List<ScreenTrendPointVO> fillBuckets(Window window, Map<String, Long> counts) {
        List<ScreenTrendPointVO> points = new ArrayList<>(window.getBuckets().size());
        for (String bucket : window.getBuckets()) {
            points.add(new ScreenTrendPointVO(bucket, counts.getOrDefault(bucket, 0L)));
        }
        return points;
    }

    private Window window(String range) {
        String normalized = StringUtils.hasText(range) ? range.trim().toLowerCase() : "week";
        LocalDate today = LocalDate.now();
        List<String> buckets = new ArrayList<>();
        if (RANGE_YEAR.equals(normalized)) {
            YearMonth start = YearMonth.from(today).minusMonths(11);
            for (int i = 0; i < 12; i++) {
                buckets.add(start.plusMonths(i).toString());
            }
            return new Window("%Y-%m", buckets, start.atDay(1).atStartOfDay());
        }
        int days = RANGE_MONTH.equals(normalized) ? 30 : 7;
        LocalDate start = today.minusDays(days - 1L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < days; i++) {
            buckets.add(start.plusDays(i).format(formatter));
        }
        return new Window("%Y-%m-%d", buckets, start.atStartOfDay());
    }

    private long accessMerchants(Scope scope, List<Device> devices) {
        Set<Long> merchantIds = devices.stream()
                .filter(device -> Integer.valueOf(ENABLED).equals(device.getStatus()))
                .map(Device::getMerchantId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (merchantIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<Merchant> wrapper = merchantWrapper(scope)
                .in(Merchant::getId, merchantIds).eq(Merchant::getStatus, ENABLED);
        return scoped(() -> merchantMapper.selectCount(wrapper));
    }

    private long merchantCount(Scope scope) {
        LambdaQueryWrapper<Merchant> wrapper = merchantWrapper(scope).eq(Merchant::getStatus, ENABLED);
        return scoped(() -> merchantMapper.selectCount(wrapper));
    }

    private long patrolMerchants(Scope scope, LocalDateTime today) {
        QueryWrapper<PlanResult> wrapper = new QueryWrapper<>();
        wrapper.select("COUNT(DISTINCT merchant_id) AS " + VALUE_ALIAS).ge("patrol_time", today);
        applyPlanResultScope(wrapper, scope);
        List<Map<String, Object>> rows = scoped(() -> planResultMapper.selectMaps(wrapper));
        if (rows.isEmpty() || !(rows.get(0).get(VALUE_ALIAS) instanceof Number)) {
            return 0L;
        }
        return ((Number) rows.get(0).get(VALUE_ALIAS)).longValue();
    }

    private long planCount(Scope scope, LocalDate monthStart, LocalDate monthEnd) {
        LambdaQueryWrapper<Plan> wrapper = planWrapper(scope)
                .le(Plan::getStartDate, monthEnd).ge(Plan::getEndDate, monthStart);
        return scoped(() -> planMapper.selectCount(wrapper));
    }

    private long planDone(Scope scope, LocalDate monthStart, LocalDate today) {
        LambdaQueryWrapper<Plan> wrapper = planWrapper(scope)
                .ge(Plan::getEndDate, monthStart).le(Plan::getEndDate, today);
        return scoped(() -> planMapper.selectCount(wrapper));
    }

    private long planResultCount(Scope scope, LocalDateTime begin, LocalDateTime end) {
        LambdaQueryWrapper<PlanResult> wrapper = planResultWrapper(scope)
                .eq(PlanResult::getResult, PlanResult.RESULT_DANGER)
                .ge(PlanResult::getPatrolTime, begin).lt(PlanResult::getPatrolTime, end);
        return scoped(() -> planResultMapper.selectCount(wrapper));
    }

    private List<ScreenTypeCountVO> dangerRanking(Scope scope, LocalDateTime begin, LocalDateTime end) {
        QueryWrapper<Danger> wrapper = new QueryWrapper<>();
        wrapper.select("danger_type AS name", "COUNT(*) AS " + VALUE_ALIAS)
                .ge("create_time", begin).lt("create_time", end)
                .groupBy("danger_type").orderByDesc(VALUE_ALIAS);
        applyDangerScope(wrapper, scope);
        List<Map<String, Object>> rows = scoped(() -> dangerMapper.selectMaps(wrapper));
        // 字典映射必须基于排行自身的 code 集合查询，不能复用滚动列表的映射
        // （滚动列表只含最新若干条，排行里的类型可能不在其中，会导致回退显示 code）
        Map<String, String> typeNames = dictService.dangerTypeNames(rows.stream()
                .map(row -> row.get("name") == null ? null : String.valueOf(row.get("name")))
                .filter(StringUtils::hasText).collect(Collectors.toSet()));
        return rows.stream()
                .map(row -> {
                    Object raw = row.get("name");
                    String code = raw == null ? null : String.valueOf(raw);
                    String name = code == null ? "未分类" : typeNames.getOrDefault(code, code);
                    long value = row.get(VALUE_ALIAS) instanceof Number
                            ? ((Number) row.get(VALUE_ALIAS)).longValue() : 0L;
                    return new ScreenTypeCountVO(name, value);
                })
                .collect(Collectors.toList());
    }

    private void fillDeviceCounts(ScreenComprehensiveVO vo, List<Device> devices,
                                  Map<String, Integer> alarmKinds) {
        long normal = 0L;
        long alarm = 0L;
        long fault = 0L;
        long offline = 0L;
        for (Device device : devices) {
            int status = deviceStatus(device, alarmKinds);
            if (status == STATUS_ALARM) {
                alarm++;
            } else if (status == STATUS_FAULT) {
                fault++;
            } else if (status == STATUS_OFFLINE) {
                offline++;
            } else {
                normal++;
            }
        }
        long total = devices.size();
        vo.setDeviceTotal(total);
        vo.setDeviceNormal(normal);
        vo.setDeviceAlarm(alarm);
        vo.setDeviceFault(fault);
        vo.setDeviceOffline(offline);
        vo.setNormalPercent(percent(normal, total));
        vo.setAlarmPercent(percent(alarm, total));
        vo.setFaultPercent(percent(fault, total));
        vo.setOfflinePercent(percent(offline, total));
    }

    private void fillTypePercents(ScreenDeviceTypeStatVO stat) {
        long total = stat.getTotal() == null ? 0L : stat.getTotal();
        stat.setNormalPercent(percent(stat.getNormal(), total));
        stat.setAlarmPercent(percent(stat.getAlarm(), total));
        stat.setFaultPercent(percent(stat.getFault(), total));
        stat.setOfflinePercent(percent(stat.getOffline(), total));
    }

    private List<MerchantStatusPointVO> merchantPoints(Scope scope, Map<Long, Integer> merchantStatuses,
                                                       Map<Long, LocalDateTime> merchantAlarmTimes) {
        LambdaQueryWrapper<Merchant> wrapper = merchantWrapper(scope)
                .eq(Merchant::getStatus, ENABLED)
                .isNotNull(Merchant::getLng).isNotNull(Merchant::getLat);
        List<Merchant> merchants = scoped(() -> merchantMapper.selectList(wrapper));
        Map<String, String> typeNames = dictService.merchantTypeNames(merchants.stream()
                .map(Merchant::getType).filter(StringUtils::hasText).collect(Collectors.toSet()));
        return merchants.stream().map(merchant -> {
            MerchantStatusPointVO point = new MerchantStatusPointVO();
            point.setMerchantId(merchant.getId());
            point.setName(merchant.getName());
            point.setLng(merchant.getLng());
            point.setLat(merchant.getLat());
            point.setPicUrl(merchant.getPlacePic1());
            point.setType(merchant.getType());
            point.setTypeName(typeNames.get(merchant.getType()));
            point.setAlarmTime(merchantAlarmTimes.get(merchant.getId()));
            point.setContact(StringUtils.hasText(merchant.getContact1())
                    ? merchant.getContact1() : merchant.getOwnerName());
            point.setPhone(StringUtils.hasText(merchant.getPhone1())
                    ? merchant.getPhone1() : merchant.getPhone());
            int status = merchantStatuses.getOrDefault(merchant.getId(), STATUS_NOT_CONNECTED);
            point.setStatus(status);
            point.setStatusText(statusText(status));
            return point;
        }).collect(Collectors.toList());
    }

    private Map<Long, Integer> merchantStatuses(List<Device> devices, Map<String, Integer> alarmKinds) {
        Map<Long, Integer> result = new HashMap<>();
        for (Device device : devices) {
            if (device.getMerchantId() == null) {
                continue;
            }
            int status = deviceStatus(device, alarmKinds);
            Integer existing = result.get(device.getMerchantId());
            if (existing == null || severity(status) > severity(existing)) {
                result.put(device.getMerchantId(), status);
            }
        }
        return result;
    }

    private List<FireAlarm> unresolvedAlarms(Scope scope) {
        LambdaQueryWrapper<FireAlarm> wrapper = alarmWrapper(scope);
        wrapper.eq(FireAlarm::getStatus, FireAlarm.STATUS_PENDING)
                .and(w -> w.eq(FireAlarm::getAlarmLevel, 1)
                        .or().eq(FireAlarm::getAlarmLevel, 3)
                        .or().eq(FireAlarm::getResult, FireAlarm.RESULT_FAULT));
        return scoped(() -> alarmMapper.selectList(wrapper));
    }

    private Map<String, Integer> unresolvedAlarmKinds(List<FireAlarm> alarms) {
        Map<String, Integer> result = new HashMap<>();
        for (FireAlarm alarm : alarms) {
            if (!StringUtils.hasText(alarm.getSn())) {
                continue;
            }
            int kind = alarm.getAlarmLevel() != null && alarm.getAlarmLevel() == 1 ? STATUS_ALARM : STATUS_FAULT;
            Integer existing = result.get(alarm.getSn());
            if (existing == null || severity(kind) > severity(existing)) {
                result.put(alarm.getSn(), kind);
            }
        }
        return result;
    }

    private Map<Long, LocalDateTime> merchantAlarmTimes(List<Device> devices, List<FireAlarm> alarms) {
        Map<String, LocalDateTime> timeBySn = new HashMap<>();
        for (FireAlarm alarm : alarms) {
            if (!StringUtils.hasText(alarm.getSn()) || alarm.getAlarmTime() == null) {
                continue;
            }
            LocalDateTime existing = timeBySn.get(alarm.getSn());
            if (existing == null || alarm.getAlarmTime().isAfter(existing)) {
                timeBySn.put(alarm.getSn(), alarm.getAlarmTime());
            }
        }
        Map<Long, LocalDateTime> result = new HashMap<>();
        for (Device device : devices) {
            if (device.getMerchantId() == null) {
                continue;
            }
            LocalDateTime time = timeBySn.get(device.getSn());
            if (time == null) {
                continue;
            }
            LocalDateTime existing = result.get(device.getMerchantId());
            if (existing == null || time.isAfter(existing)) {
                result.put(device.getMerchantId(), time);
            }
        }
        return result;
    }

    private int deviceStatus(Device device, Map<String, Integer> alarmKinds) {
        Integer alarmKind = alarmKinds.get(device.getSn());
        if (alarmKind != null) {
            return alarmKind;
        }
        if (!Integer.valueOf(ONLINE).equals(device.getOnlineStatus())) {
            return STATUS_OFFLINE;
        }
        return STATUS_NORMAL;
    }

    private int severity(int status) {
        if (status == STATUS_ALARM) {
            return 3;
        }
        if (status == STATUS_FAULT) {
            return 2;
        }
        if (status == STATUS_OFFLINE) {
            return 1;
        }
        return 0;
    }

    private String statusText(int status) {
        if (status == STATUS_ALARM) {
            return "火警";
        }
        if (status == STATUS_FAULT) {
            return "故障";
        }
        if (status == STATUS_OFFLINE) {
            return "离线";
        }
        if (status == STATUS_NOT_CONNECTED) {
            return "未接入";
        }
        return "正常";
    }

    private List<Device> devices(Scope scope) {
        return scoped(() -> deviceMapper.selectList(deviceWrapper(scope)));
    }

    private LambdaQueryWrapper<Device> deviceWrapper(Scope scope) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (scope.isMultiTenant()) {
            wrapper.in(Device::getTenantId, scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in(Device::getMerchantId, scope.getMerchantIds());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<FireAlarm> alarmWrapper(Scope scope) {
        LambdaQueryWrapper<FireAlarm> wrapper = new LambdaQueryWrapper<>();
        if (scope.isMultiTenant()) {
            wrapper.in(FireAlarm::getTenantId, scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in(FireAlarm::getMerchantId, scope.getMerchantIds());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<Danger> dangerWrapper(Scope scope) {
        LambdaQueryWrapper<Danger> wrapper = new LambdaQueryWrapper<>();
        if (scope.isMultiTenant()) {
            wrapper.in(Danger::getTenantId, scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in(Danger::getMerchantId, scope.getMerchantIds());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<Merchant> merchantWrapper(Scope scope) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (scope.isMultiTenant()) {
            wrapper.in(Merchant::getTenantId, scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in(Merchant::getId, scope.getMerchantIds());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<Plan> planWrapper(Scope scope) {
        LambdaQueryWrapper<Plan> wrapper = new LambdaQueryWrapper<>();
        if (scope.isMultiTenant()) {
            wrapper.in(Plan::getTenantId, scope.getTenantIds());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<PlanResult> planResultWrapper(Scope scope) {
        LambdaQueryWrapper<PlanResult> wrapper = new LambdaQueryWrapper<>();
        if (scope.isMultiTenant()) {
            wrapper.in(PlanResult::getTenantId, scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in(PlanResult::getMerchantId, scope.getMerchantIds());
        }
        return wrapper;
    }

    private void applyAlarmScope(QueryWrapper<FireAlarm> wrapper, Scope scope) {
        if (scope.isMultiTenant()) {
            wrapper.in("tenant_id", scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in("merchant_id", scope.getMerchantIds());
        }
    }

    private void applyDangerScope(QueryWrapper<Danger> wrapper, Scope scope) {
        if (scope.isMultiTenant()) {
            wrapper.in("tenant_id", scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in("merchant_id", scope.getMerchantIds());
        }
    }

    private void applyPlanResultScope(QueryWrapper<PlanResult> wrapper, Scope scope) {
        if (scope.isMultiTenant()) {
            wrapper.in("tenant_id", scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in("merchant_id", scope.getMerchantIds());
        }
    }

    private long alarmCount(Scope scope, Consumer<LambdaQueryWrapper<FireAlarm>> customizer) {
        LambdaQueryWrapper<FireAlarm> wrapper = alarmWrapper(scope);
        customizer.accept(wrapper);
        return scoped(() -> alarmMapper.selectCount(wrapper));
    }

    private long dangerCount(Scope scope, Consumer<LambdaQueryWrapper<Danger>> customizer) {
        LambdaQueryWrapper<Danger> wrapper = dangerWrapper(scope);
        customizer.accept(wrapper);
        return scoped(() -> dangerMapper.selectCount(wrapper));
    }

    private Map<Long, Merchant> merchants(Scope scope, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<Merchant> wrapper = merchantWrapper(scope).in(Merchant::getId, ids);
        return scoped(() -> merchantMapper.selectList(wrapper)).stream()
                .collect(Collectors.toMap(Merchant::getId, merchant -> merchant, (left, right) -> left));
    }

    private Set<Long> merchantIds(List<FireAlarm> alarms) {
        return alarms.stream().map(FireAlarm::getMerchantId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<Long> merchantIdsOfDanger(List<Danger> dangers) {
        return dangers.stream().map(Danger::getMerchantId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private ScreenAlarmItemVO alarmItem(FireAlarm alarm, Merchant merchant) {
        ScreenAlarmItemVO item = new ScreenAlarmItemVO();
        item.setId(alarm.getId());
        item.setAlarmTime(alarm.getAlarmTime());
        item.setMerchantName(merchant == null ? null : merchant.getName());
        item.setPosition(alarm.getPosition());
        item.setAlarmLevel(alarm.getAlarmLevel());
        item.setStatus(alarm.getStatus());
        item.setResult(alarm.getResult());
        item.setLng(merchant == null ? null : merchant.getLng());
        item.setLat(merchant == null ? null : merchant.getLat());
        return item;
    }

    private ScreenDangerItemVO dangerItem(Danger danger, Merchant merchant, Map<String, String> typeNames) {
        ScreenDangerItemVO item = new ScreenDangerItemVO();
        item.setId(danger.getId());
        item.setMerchantName(merchant == null ? null : merchant.getName());
        item.setDangerType(danger.getDangerType());
        String typeCode = danger.getDangerType();
        item.setDangerTypeName(StringUtils.hasText(typeCode)
                ? typeNames.getOrDefault(typeCode, typeCode) : "未分类");
        item.setStatus(danger.getStatus());
        item.setSourceType(danger.getSourceType());
        item.setCreateTime(danger.getCreateTime());
        item.setDealLimit(danger.getDealLimit());
        return item;
    }

    private double percent(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.min(100.0, Math.round(part * 10000.0 / total) / 100.0);
    }

    private Scope scope() {
        Scope scope = new Scope();
        scope.setMultiTenant(dataScopeService.isPlatform());
        scope.setTenantIds(scope.isMultiTenant() ? dataScopeService.authorizedTenantIds()
                : Collections.emptyList());
        scope.setMerchantIds(dataScopeService.scopedMerchantIds());
        return scope;
    }

    private <T> T scoped(Supplier<T> supplier) {
        if (dataScopeService.isPlatform()) {
            return dataScopeService.withIgnoreTenant(supplier);
        }
        return supplier.get();
    }

    private ScreenComprehensiveVO emptyComprehensive() {
        ScreenComprehensiveVO vo = new ScreenComprehensiveVO();
        vo.setFireTotal(0L);
        vo.setFireHandled(0L);
        vo.setFireTrue(0L);
        vo.setDangerUnhandled(0L);
        vo.setDangerToday(0L);
        vo.setDangerOverdue(0L);
        vo.setPatrolMerchants(0L);
        vo.setPatrolPercent(0.0);
        vo.setAccessMerchants(0L);
        vo.setAccessDevices(0L);
        vo.setDeviceTotal(0L);
        vo.setDeviceNormal(0L);
        vo.setDeviceAlarm(0L);
        vo.setDeviceFault(0L);
        vo.setDeviceOffline(0L);
        vo.setNormalPercent(0.0);
        vo.setAlarmPercent(0.0);
        vo.setFaultPercent(0.0);
        vo.setOfflinePercent(0.0);
        vo.setMerchantPoints(Collections.emptyList());
        return vo;
    }

    private ScreenAlarmVO emptyAlarm() {
        ScreenAlarmVO vo = new ScreenAlarmVO();
        vo.setTrend(Collections.emptyList());
        vo.setAlarms(Collections.emptyList());
        vo.setMerchantPoints(Collections.emptyList());
        return vo;
    }

    private ScreenDangerVO emptyDanger() {
        ScreenDangerVO vo = new ScreenDangerVO();
        vo.setMonthTotal(0L);
        vo.setMonthDone(0L);
        vo.setMonthUnfinished(0L);
        vo.setMonthOverdue(0L);
        vo.setTrend(Collections.emptyList());
        vo.setDangers(Collections.emptyList());
        vo.setPlanCount(0L);
        vo.setPlanDone(0L);
        vo.setPlanDanger(0L);
        vo.setTypeRanking(Collections.emptyList());
        vo.setMerchantPoints(Collections.emptyList());
        return vo;
    }

    private ScreenDeviceVO emptyDevice() {
        ScreenDeviceVO vo = new ScreenDeviceVO();
        vo.setTypes(Collections.emptyList());
        vo.setFaultTrend(Collections.emptyList());
        vo.setFaults(Collections.emptyList());
        vo.setMerchantPoints(Collections.emptyList());
        return vo;
    }

    private enum FaultFilter {
        ALL,
        FAULT
    }

    private static final class Window {
        private final String format;
        private final List<String> buckets;
        private final LocalDateTime start;

        private Window(String format, List<String> buckets, LocalDateTime start) {
            this.format = format;
            this.buckets = buckets;
            this.start = start;
        }

        private String getFormat() {
            return format;
        }

        private List<String> getBuckets() {
            return buckets;
        }

        private LocalDateTime getStart() {
            return start;
        }
    }

    private static final class Scope {
        private boolean multiTenant;
        private List<Long> tenantIds;
        private List<Long> merchantIds;

        private boolean isEmpty() {
            return (multiTenant && tenantIds.isEmpty())
                    || (merchantIds != null && merchantIds.isEmpty());
        }

        private boolean isMultiTenant() {
            return multiTenant;
        }

        private void setMultiTenant(boolean multiTenant) {
            this.multiTenant = multiTenant;
        }

        private List<Long> getTenantIds() {
            return tenantIds;
        }

        private void setTenantIds(List<Long> tenantIds) {
            this.tenantIds = tenantIds;
        }

        private List<Long> getMerchantIds() {
            return merchantIds;
        }

        private void setMerchantIds(List<Long> merchantIds) {
            this.merchantIds = merchantIds;
        }
    }
}
