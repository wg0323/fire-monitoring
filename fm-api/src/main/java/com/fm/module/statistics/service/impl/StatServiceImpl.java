package com.fm.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.alarm.entity.CallRecord;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.CallRecordMapper;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.danger.entity.Danger;
import com.fm.module.danger.mapper.DangerMapper;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.DictService;
import com.fm.module.inspection.entity.PlanResult;
import com.fm.module.inspection.mapper.PlanResultMapper;
import com.fm.module.statistics.converter.StatisticsConverter;
import com.fm.module.statistics.dto.StatQueryDTO;
import com.fm.module.statistics.service.StatService;
import com.fm.module.statistics.vo.MonitorHomeVO;
import com.fm.module.statistics.vo.OverviewVO;
import com.fm.module.statistics.vo.StatAlarmItemVO;
import com.fm.module.statistics.vo.StatAlarmVO;
import com.fm.module.statistics.vo.StatCallItemVO;
import com.fm.module.statistics.vo.StatCallVO;
import com.fm.module.statistics.vo.StatComprehensiveVO;
import com.fm.module.statistics.vo.StatDangerTypeVO;
import com.fm.module.statistics.vo.StatDangerVO;
import com.fm.module.statistics.vo.StatFaultItemVO;
import com.fm.module.statistics.vo.StatFaultVO;
import com.fm.module.statistics.vo.StatRankingVO;
import com.fm.module.statistics.vo.StatTrendPointVO;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 概览统计实现：数据量在万级以内，直接走 GROUP BY 聚合，不引入缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int RANK_LIMIT = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int IN_BATCH_SIZE = 1000;
    private static final int ALARM_LEVEL_FIRE = 1;
    private static final int ALARM_LEVEL_FAULT = 3;
    private static final String DATE_PATTERN = "%Y-%m-%d";
    private static final String BUCKET_ALIAS = "bucket";
    private static final String VALUE_ALIAS = "value";
    private static final String NAME_ALIAS = "name";
    private static final String MERCHANT_ALIAS = "mid";

    private final FireAlarmMapper alarmMapper;
    private final DeviceMapper deviceMapper;
    private final DangerMapper dangerMapper;
    private final MerchantMapper merchantMapper;
    private final PlanResultMapper planResultMapper;
    private final CallRecordMapper callRecordMapper;
    private final DictService dictService;
    private final DataScopeService dataScopeService;

    @Override
    public StatComprehensiveVO comprehensive(StatQueryDTO query) {
        Scope scope = scope();
        Window window = window(query);
        if (scope.isEmpty()) {
            return emptyComprehensive(window);
        }
        StatComprehensiveVO vo = new StatComprehensiveVO();
        vo.setAlarmTotal(alarmCount(scope, window, w -> w.eq(FireAlarm::getAlarmLevel, ALARM_LEVEL_FIRE)));
        vo.setFaultTotal(alarmCount(scope, window, this::faultCondition));
        vo.setDangerTotal(dangerCount(scope, window, w -> {}));
        vo.setFireTotal(alarmCount(scope, window,
                w -> w.eq(FireAlarm::getResult, FireAlarm.RESULT_TRUE)));
        vo.setCallTotal(callCount(scope, window));
        vo.setAlarmTrend(alarmTrend(scope, window, ALARM_LEVEL_FIRE));
        vo.setDangerTypeRanking(dangerTypeRanking(scope, window));
        vo.setFaultMerchantRanking(faultMerchantRanking(scope, window));
        vo.setCallMerchantRanking(callMerchantRanking(scope, window));
        return vo;
    }

    @Override
    public StatAlarmVO alarm(StatQueryDTO query) {
        Scope scope = scope();
        Window window = window(query);
        StatAlarmVO vo = new StatAlarmVO();
        if (scope.isEmpty()) {
            vo.setTrend(zeroTrend(window));
            vo.setPage(new Page<>(pageNum(query), pageSize(query), 0L));
            return vo;
        }
        vo.setTrend(alarmTrend(scope, window, ALARM_LEVEL_FIRE));
        vo.setPage(alarmPage(scope, window, query, w -> w.eq(FireAlarm::getAlarmLevel, ALARM_LEVEL_FIRE)));
        return vo;
    }

    @Override
    public StatFaultVO fault(StatQueryDTO query) {
        Scope scope = scope();
        Window window = window(query);
        StatFaultVO vo = new StatFaultVO();
        if (scope.isEmpty()) {
            vo.setTrend(zeroTrend(window));
            vo.setPage(new Page<>(pageNum(query), pageSize(query), 0L));
            return vo;
        }
        vo.setTrend(faultTrend(scope, window));
        vo.setPage(faultPage(scope, window, query));
        return vo;
    }

    @Override
    public StatDangerVO danger(StatQueryDTO query) {
        Scope scope = scope();
        Window window = window(query);
        StatDangerVO vo = emptyDanger();
        if (scope.isEmpty()) {
            return vo;
        }
        QueryWrapper<Danger> wrapper = new QueryWrapper<>();
        wrapper.select("danger_type AS type",
                        "COUNT(*) AS total",
                        "SUM(CASE WHEN status = " + Danger.STATUS_DONE + " THEN 1 ELSE 0 END) AS done",
                        "SUM(CASE WHEN status <> " + Danger.STATUS_DONE
                                + " AND deal_limit IS NOT NULL AND deal_limit < CURDATE() THEN 1 ELSE 0 END) AS overdue")
                .between("create_time", window.getStart(), window.getEnd())
                .groupBy("danger_type")
                .orderByDesc("total");
        applyDangerScope(wrapper, scope);
        List<Map<String, Object>> aggRows = scoped(() -> dangerMapper.selectMaps(wrapper));
        Map<String, String> typeNames = dictService.dangerTypeNames(aggRows.stream()
                .map(row -> row.get("type") == null ? null : String.valueOf(row.get("type")))
                .filter(StringUtils::hasText).collect(Collectors.toSet()));
        long total = 0L;
        long done = 0L;
        long overdue = 0L;
        List<StatDangerTypeVO> rows = new ArrayList<>();
        for (Map<String, Object> row : aggRows) {
            long rowTotal = longValue(row.get("total"));
            long rowDone = longValue(row.get("done"));
            long rowOverdue = longValue(row.get("overdue"));
            total += rowTotal;
            done += rowDone;
            overdue += rowOverdue;
            String code = row.get("type") == null ? null : String.valueOf(row.get("type"));
            StatDangerTypeVO item = new StatDangerTypeVO();
            item.setDangerType(code);
            item.setDangerTypeName(StringUtils.hasText(code) ? typeNames.getOrDefault(code, code) : "未分类");
            item.setTotal(rowTotal);
            item.setDone(rowDone);
            item.setUndone(rowTotal - rowDone);
            item.setOverdue(rowOverdue);
            item.setDoneRate(percent(rowDone, rowTotal));
            item.setOverdueRate(percent(rowOverdue, rowTotal));
            rows.add(item);
        }
        vo.setTotal(total);
        vo.setDone(done);
        vo.setUndone(total - done);
        vo.setOverdue(overdue);
        vo.setDoneRate(percent(done, total));
        vo.setOverdueRate(percent(overdue, total));
        vo.setRows(rows);
        return vo;
    }

    @Override
    public StatAlarmVO fire(StatQueryDTO query) {
        Scope scope = scope();
        Window window = window(query);
        StatAlarmVO vo = new StatAlarmVO();
        if (scope.isEmpty()) {
            vo.setTrend(zeroTrend(window));
            vo.setPage(new Page<>(pageNum(query), pageSize(query), 0L));
            return vo;
        }
        vo.setTrend(fireTrend(scope, window));
        vo.setPage(alarmPage(scope, window, query, w -> w.eq(FireAlarm::getResult, FireAlarm.RESULT_TRUE)));
        return vo;
    }

    @Override
    public StatCallVO call(StatQueryDTO query) {
        Scope scope = scope();
        Window window = window(query);
        StatCallVO vo = new StatCallVO();
        if (scope.isEmpty()) {
            vo.setPage(new Page<>(pageNum(query), pageSize(query), 0L));
            return vo;
        }
        vo.setPage(callPage(scope, window, query));
        return vo;
    }

    private Page<StatAlarmItemVO> alarmPage(Scope scope, Window window, StatQueryDTO query,
                                            Consumer<LambdaQueryWrapper<FireAlarm>> filter) {
        Page<FireAlarm> data = alarmPageData(scope, window, query, filter);
        Set<Long> merchantIds = data.getRecords().stream().map(FireAlarm::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Merchant> merchants = merchants(scope, merchantIds);
        List<StatAlarmItemVO> records = data.getRecords().stream()
                .map(alarm -> StatisticsConverter.toAlarmItemVO(alarm,
                        merchantName(merchants, alarm.getMerchantId())))
                .collect(Collectors.toList());
        return copyPage(data, records);
    }

    private Page<StatFaultItemVO> faultPage(Scope scope, Window window, StatQueryDTO query) {
        Page<FireAlarm> data = alarmPageData(scope, window, query, this::faultCondition);
        Set<Long> merchantIds = data.getRecords().stream().map(FireAlarm::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Merchant> merchants = merchants(scope, merchantIds);
        Map<String, String> typeNames = dictService.alarmTypeNames(data.getRecords().stream()
                .map(FireAlarm::getAlarmType).filter(StringUtils::hasText).collect(Collectors.toSet()));
        List<StatFaultItemVO> records = data.getRecords().stream().map(alarm -> {
            String alarmTypeName = StringUtils.hasText(alarm.getAlarmType())
                    ? typeNames.getOrDefault(alarm.getAlarmType(), alarm.getAlarmType()) : "未分类";
            return StatisticsConverter.toFaultItemVO(alarm, merchantName(merchants, alarm.getMerchantId()),
                    alarmTypeName);
        }).collect(Collectors.toList());
        return copyPage(data, records);
    }

    private Page<FireAlarm> alarmPageData(Scope scope, Window window, StatQueryDTO query,
                                          Consumer<LambdaQueryWrapper<FireAlarm>> filter) {
        Page<FireAlarm> page = new Page<>(pageNum(query), pageSize(query));
        LambdaQueryWrapper<FireAlarm> wrapper = alarmWrapper(scope)
                .between(FireAlarm::getAlarmTime, window.getStart(), window.getEnd())
                .orderByDesc(FireAlarm::getAlarmTime);
        filter.accept(wrapper);
        return scoped(() -> alarmMapper.selectPage(page, wrapper));
    }

    private Page<StatCallItemVO> callPage(Scope scope, Window window, StatQueryDTO query) {
        Page<CallRecord> page = new Page<>(pageNum(query), pageSize(query));
        LambdaQueryWrapper<CallRecord> wrapper = callWrapper(scope)
                .between(CallRecord::getCreateTime, window.getStart(), window.getEnd())
                .orderByDesc(CallRecord::getCreateTime);
        Page<CallRecord> data = scoped(() -> callRecordMapper.selectPage(page, wrapper));
        Set<Long> alarmIds = data.getRecords().stream().map(CallRecord::getAlarmId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FireAlarm> alarms = alarmsByIds(scope, alarmIds);
        Set<Long> merchantIds = alarms.values().stream().map(FireAlarm::getMerchantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Merchant> merchants = merchants(scope, merchantIds);
        Map<String, String> typeNames = dictService.alarmTypeNames(alarms.values().stream()
                .map(FireAlarm::getAlarmType).filter(StringUtils::hasText).collect(Collectors.toSet()));
        List<StatCallItemVO> records = data.getRecords().stream().map(call -> {
            FireAlarm alarm = call.getAlarmId() == null ? null : alarms.get(call.getAlarmId());
            String merchantName = alarm == null ? null : merchantName(merchants, alarm.getMerchantId());
            String alarmTypeName = alarm == null || !StringUtils.hasText(alarm.getAlarmType())
                    ? null : typeNames.getOrDefault(alarm.getAlarmType(), alarm.getAlarmType());
            String fileUrl = StringUtils.hasText(call.getMonitorFileRemote())
                    ? call.getMonitorFileRemote() : call.getMonitorFileLocal();
            return StatisticsConverter.toCallItemVO(call, alarm, merchantName, alarmTypeName, fileUrl);
        }).collect(Collectors.toList());
        return copyPage(data, records);
    }

    private long alarmCount(Scope scope, Window window, Consumer<LambdaQueryWrapper<FireAlarm>> customizer) {
        LambdaQueryWrapper<FireAlarm> wrapper = alarmWrapper(scope)
                .between(FireAlarm::getAlarmTime, window.getStart(), window.getEnd());
        customizer.accept(wrapper);
        return scoped(() -> alarmMapper.selectCount(wrapper));
    }

    private long dangerCount(Scope scope, Window window, Consumer<LambdaQueryWrapper<Danger>> customizer) {
        LambdaQueryWrapper<Danger> wrapper = dangerWrapper(scope)
                .between(Danger::getCreateTime, window.getStart(), window.getEnd());
        customizer.accept(wrapper);
        return scoped(() -> dangerMapper.selectCount(wrapper));
    }

    private long callCount(Scope scope, Window window) {
        LambdaQueryWrapper<CallRecord> wrapper = callWrapper(scope)
                .between(CallRecord::getCreateTime, window.getStart(), window.getEnd());
        return scoped(() -> callRecordMapper.selectCount(wrapper));
    }

    private void faultCondition(LambdaQueryWrapper<FireAlarm> wrapper) {
        wrapper.and(w -> w.eq(FireAlarm::getAlarmLevel, ALARM_LEVEL_FAULT)
                .or().eq(FireAlarm::getResult, FireAlarm.RESULT_FAULT));
    }

    private List<StatTrendPointVO> alarmTrend(Scope scope, Window window, int alarmLevel) {
        QueryWrapper<FireAlarm> wrapper = new QueryWrapper<>();
        wrapper.select("DATE_FORMAT(alarm_time, '" + DATE_PATTERN + "') AS " + BUCKET_ALIAS,
                        "COUNT(*) AS " + VALUE_ALIAS)
                .between("alarm_time", window.getStart(), window.getEnd())
                .eq("alarm_level", alarmLevel)
                .groupBy(BUCKET_ALIAS);
        applyAlarmScope(wrapper, scope);
        return fillBuckets(window, countMap(scoped(() -> alarmMapper.selectMaps(wrapper))));
    }

    private List<StatTrendPointVO> faultTrend(Scope scope, Window window) {
        QueryWrapper<FireAlarm> wrapper = new QueryWrapper<>();
        wrapper.select("DATE_FORMAT(alarm_time, '" + DATE_PATTERN + "') AS " + BUCKET_ALIAS,
                        "COUNT(*) AS " + VALUE_ALIAS)
                .between("alarm_time", window.getStart(), window.getEnd())
                .and(w -> w.eq("alarm_level", ALARM_LEVEL_FAULT).or().eq("result", FireAlarm.RESULT_FAULT))
                .groupBy(BUCKET_ALIAS);
        applyAlarmScope(wrapper, scope);
        return fillBuckets(window, countMap(scoped(() -> alarmMapper.selectMaps(wrapper))));
    }

    private List<StatTrendPointVO> fireTrend(Scope scope, Window window) {
        QueryWrapper<FireAlarm> wrapper = new QueryWrapper<>();
        wrapper.select("DATE_FORMAT(alarm_time, '" + DATE_PATTERN + "') AS " + BUCKET_ALIAS,
                        "COUNT(*) AS " + VALUE_ALIAS)
                .between("alarm_time", window.getStart(), window.getEnd())
                .eq("result", FireAlarm.RESULT_TRUE)
                .groupBy(BUCKET_ALIAS);
        applyAlarmScope(wrapper, scope);
        return fillBuckets(window, countMap(scoped(() -> alarmMapper.selectMaps(wrapper))));
    }

    private List<StatRankingVO> dangerTypeRanking(Scope scope, Window window) {
        QueryWrapper<Danger> wrapper = new QueryWrapper<>();
        wrapper.select("danger_type AS " + NAME_ALIAS, "COUNT(*) AS " + VALUE_ALIAS)
                .between("create_time", window.getStart(), window.getEnd())
                .groupBy("danger_type")
                .orderByDesc(VALUE_ALIAS)
                .last("LIMIT " + RANK_LIMIT);
        applyDangerScope(wrapper, scope);
        List<Map<String, Object>> rows = scoped(() -> dangerMapper.selectMaps(wrapper));
        Map<String, String> typeNames = dictService.dangerTypeNames(rows.stream()
                .map(row -> row.get(NAME_ALIAS) == null ? null : String.valueOf(row.get(NAME_ALIAS)))
                .filter(StringUtils::hasText).collect(Collectors.toSet()));
        return rows.stream().map(row -> {
            Object raw = row.get(NAME_ALIAS);
            String code = raw == null ? null : String.valueOf(raw);
            String name = StringUtils.hasText(code) ? typeNames.getOrDefault(code, code) : "未分类";
            return new StatRankingVO(name, longValue(row.get(VALUE_ALIAS)));
        }).collect(Collectors.toList());
    }

    private List<StatRankingVO> faultMerchantRanking(Scope scope, Window window) {
        QueryWrapper<FireAlarm> wrapper = new QueryWrapper<>();
        wrapper.select("merchant_id AS " + MERCHANT_ALIAS, "COUNT(*) AS " + VALUE_ALIAS)
                .between("alarm_time", window.getStart(), window.getEnd())
                .and(w -> w.eq("alarm_level", ALARM_LEVEL_FAULT).or().eq("result", FireAlarm.RESULT_FAULT))
                .groupBy("merchant_id")
                .orderByDesc(VALUE_ALIAS)
                .last("LIMIT " + RANK_LIMIT);
        applyAlarmScope(wrapper, scope);
        List<Map<String, Object>> rows = scoped(() -> alarmMapper.selectMaps(wrapper));
        Set<Long> merchantIds = rows.stream().map(row -> longOrNull(row.get(MERCHANT_ALIAS)))
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Merchant> merchants = merchants(scope, merchantIds);
        return rows.stream().map(row -> {
            Long merchantId = longOrNull(row.get(MERCHANT_ALIAS));
            return new StatRankingVO(merchantId == null ? "未绑定商户" : merchantName(merchants, merchantId),
                    longValue(row.get(VALUE_ALIAS)));
        }).collect(Collectors.toList());
    }

    private List<StatRankingVO> callMerchantRanking(Scope scope, Window window) {
        List<CallRecord> calls = scoped(() -> callRecordMapper.selectList(callWrapper(scope)
                .select(CallRecord::getAlarmId)
                .between(CallRecord::getCreateTime, window.getStart(), window.getEnd())));
        Set<Long> alarmIds = calls.stream().map(CallRecord::getAlarmId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, FireAlarm> alarms = alarmsByIds(scope, alarmIds);
        Map<Long, Long> counts = new HashMap<>(alarms.size());
        for (CallRecord call : calls) {
            FireAlarm alarm = call.getAlarmId() == null ? null : alarms.get(call.getAlarmId());
            if (alarm != null && alarm.getMerchantId() != null) {
                counts.merge(alarm.getMerchantId(), 1L, Long::sum);
            }
        }
        List<Map.Entry<Long, Long>> sorted = counts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(RANK_LIMIT)
                .collect(Collectors.toList());
        Map<Long, Merchant> merchants = merchants(scope, counts.keySet());
        return sorted.stream().map(entry -> {
            String name = merchantName(merchants, entry.getKey());
            return new StatRankingVO(name == null ? "未绑定商户" : name, entry.getValue());
        }).collect(Collectors.toList());
    }

    private Map<Long, FireAlarm> alarmsByIds(Scope scope, Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, FireAlarm> result = new HashMap<>(ids.size());
        List<Long> all = new ArrayList<>(ids);
        for (int i = 0; i < all.size(); i += IN_BATCH_SIZE) {
            List<Long> batch = all.subList(i, Math.min(i + IN_BATCH_SIZE, all.size()));
            result.putAll(scoped(() -> alarmMapper.selectBatchIds(batch)).stream()
                    .collect(Collectors.toMap(FireAlarm::getId, alarm -> alarm, (left, right) -> left)));
        }
        return result;
    }

    private Map<Long, Merchant> merchants(Scope scope, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (scope.isMultiTenant()) {
            wrapper.in(Merchant::getTenantId, scope.getTenantIds());
        }
        if (scope.getMerchantIds() != null) {
            wrapper.in(Merchant::getId, scope.getMerchantIds());
        }
        wrapper.in(Merchant::getId, ids);
        return scoped(() -> merchantMapper.selectList(wrapper)).stream()
                .collect(Collectors.toMap(Merchant::getId, merchant -> merchant, (left, right) -> left));
    }

    private String merchantName(Map<Long, Merchant> merchants, Long merchantId) {
        if (merchantId == null) {
            return null;
        }
        Merchant merchant = merchants.get(merchantId);
        return merchant == null ? null : merchant.getName();
    }

    private Window window(StatQueryDTO query) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = query != null && query.getEndTime() != null
                ? query.getEndTime().toLocalDate() : today;
        LocalDate startDate = query != null && query.getStartTime() != null
                ? query.getStartTime().toLocalDate() : endDate.minusDays(DEFAULT_TREND_DAYS - 1L);
        if (startDate.isAfter(endDate)) {
            LocalDate swap = startDate;
            startDate = endDate;
            endDate = swap;
        }
        List<String> buckets = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1L)) {
            buckets.add(day.format(formatter));
        }
        return new Window(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX), buckets);
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

    private List<StatTrendPointVO> fillBuckets(Window window, Map<String, Long> counts) {
        List<StatTrendPointVO> points = new ArrayList<>(window.getBuckets().size());
        for (String bucket : window.getBuckets()) {
            points.add(new StatTrendPointVO(bucket, counts.getOrDefault(bucket, 0L)));
        }
        return points;
    }

    private List<StatTrendPointVO> zeroTrend(Window window) {
        return fillBuckets(window, Collections.emptyMap());
    }

    private long pageNum(StatQueryDTO query) {
        if (query == null || query.getPageNum() == null || query.getPageNum() < 1L) {
            return 1L;
        }
        return query.getPageNum();
    }

    private long pageSize(StatQueryDTO query) {
        if (query == null || query.getPageSize() == null || query.getPageSize() < 1L) {
            return 20L;
        }
        return Math.min(query.getPageSize(), MAX_PAGE_SIZE);
    }

    private long longValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private Long longOrNull(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private double percent(long part, long total) {
        if (total <= 0L) {
            return 0.0;
        }
        return Math.min(100.0, Math.round(part * 10000.0 / total) / 100.0);
    }

    private <T> Page<T> copyPage(Page<?> source, List<T> records) {
        Page<T> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(records);
        return target;
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

    private LambdaQueryWrapper<CallRecord> callWrapper(Scope scope) {
        LambdaQueryWrapper<CallRecord> wrapper = new LambdaQueryWrapper<>();
        if (scope.isMultiTenant()) {
            wrapper.in(CallRecord::getTenantId, scope.getTenantIds());
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

    private StatComprehensiveVO emptyComprehensive(Window window) {
        StatComprehensiveVO vo = new StatComprehensiveVO();
        vo.setAlarmTotal(0L);
        vo.setFaultTotal(0L);
        vo.setDangerTotal(0L);
        vo.setFireTotal(0L);
        vo.setCallTotal(0L);
        vo.setAlarmTrend(zeroTrend(window));
        vo.setDangerTypeRanking(Collections.emptyList());
        vo.setFaultMerchantRanking(Collections.emptyList());
        vo.setCallMerchantRanking(Collections.emptyList());
        return vo;
    }

    private StatDangerVO emptyDanger() {
        StatDangerVO vo = new StatDangerVO();
        vo.setTotal(0L);
        vo.setDone(0L);
        vo.setUndone(0L);
        vo.setOverdue(0L);
        vo.setDoneRate(0.0);
        vo.setOverdueRate(0.0);
        vo.setRows(Collections.emptyList());
        return vo;
    }

    private static final class Window {
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final List<String> buckets;

        private Window(LocalDateTime start, LocalDateTime end, List<String> buckets) {
            this.start = start;
            this.end = end;
            this.buckets = buckets;
        }

        private LocalDateTime getStart() {
            return start;
        }

        private LocalDateTime getEnd() {
            return end;
        }

        private List<String> getBuckets() {
            return buckets;
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

    @Override
    public OverviewVO overview() {
        Long merchantId = currentMerchantId();
        if (!dataScopeService.isPlatform()) {
            return buildOverview(merchantId, false, Collections.emptyList());
        }
        List<Long> tenantIds = dataScopeService.authorizedTenantIds();
        if (tenantIds.isEmpty()) {
            return emptyOverview();
        }
        return dataScopeService.withIgnoreTenant(() -> buildOverview(null, true, tenantIds));
    }

    @Override
    public MonitorHomeVO monitorHome() {
        Long merchantId = currentMerchantId();
        if (!dataScopeService.isPlatform()) {
            return buildMonitorHome(merchantId, false, Collections.emptyList());
        }
        List<Long> tenantIds = dataScopeService.authorizedTenantIds();
        if (tenantIds.isEmpty()) {
            return emptyMonitorHome();
        }
        return dataScopeService.withIgnoreTenant(() -> buildMonitorHome(null, true, tenantIds));
    }

    private MonitorHomeVO buildMonitorHome(Long merchantId, boolean multi, List<Long> tenantIds) {
        MonitorHomeVO vo = new MonitorHomeVO();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDate todayDate = LocalDate.now();

        vo.setFireAlarmNum(alarmMapper.selectCount(new LambdaQueryWrapper<FireAlarm>()
                .in(multi, FireAlarm::getTenantId, tenantIds)
                .eq(merchantId != null, FireAlarm::getMerchantId, merchantId)
                .ge(FireAlarm::getAlarmTime, today)));
        vo.setDealFireAlarmNum(alarmMapper.selectCount(new LambdaQueryWrapper<FireAlarm>()
                .in(multi, FireAlarm::getTenantId, tenantIds)
                .eq(merchantId != null, FireAlarm::getMerchantId, merchantId)
                .ge(FireAlarm::getAlarmTime, today)
                .eq(FireAlarm::getStatus, FireAlarm.STATUS_HANDLED)));
        vo.setRealFireAlarmNum(alarmMapper.selectCount(new LambdaQueryWrapper<FireAlarm>()
                .in(multi, FireAlarm::getTenantId, tenantIds)
                .eq(merchantId != null, FireAlarm::getMerchantId, merchantId)
                .ge(FireAlarm::getAlarmTime, today)
                .eq(FireAlarm::getResult, FireAlarm.RESULT_TRUE)));
        vo.setNoDealDangerNum(dangerMapper.selectCount(new LambdaQueryWrapper<Danger>()
                .in(multi, Danger::getTenantId, tenantIds)
                .eq(merchantId != null, Danger::getMerchantId, merchantId)
                .in(Danger::getStatus, Danger.STATUS_PENDING_ASSIGN, Danger.STATUS_PENDING_DEAL,
                        Danger.STATUS_PENDING_AUDIT, Danger.STATUS_REJECTED)));
        vo.setAddDangerNum(dangerMapper.selectCount(new LambdaQueryWrapper<Danger>()
                .in(multi, Danger::getTenantId, tenantIds)
                .eq(merchantId != null, Danger::getMerchantId, merchantId)
                .ge(Danger::getCreateTime, today)));
        vo.setExpectDangerNum(dangerMapper.selectCount(new LambdaQueryWrapper<Danger>()
                .in(multi, Danger::getTenantId, tenantIds)
                .eq(merchantId != null, Danger::getMerchantId, merchantId)
                .ne(Danger::getStatus, Danger.STATUS_DONE)
                .isNotNull(Danger::getDealLimit)
                .lt(Danger::getDealLimit, todayDate)));

        long patrolNum = countPatrolMerchants(merchantId, multi, tenantIds, today);
        vo.setPatrolNum(patrolNum);

        Long nspNum = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .in(multi, Merchant::getTenantId, tenantIds));
        vo.setNspNum(nspNum);
        vo.setPatrolPercent(nspNum > 0 ? Math.round(patrolNum * 10000.0 / nspNum) / 100.0 : 0.0);

        Long total = deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .in(multi, Device::getTenantId, tenantIds)
                .eq(merchantId != null, Device::getMerchantId, merchantId));
        Long online = deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .in(multi, Device::getTenantId, tenantIds)
                .eq(merchantId != null, Device::getMerchantId, merchantId)
                .eq(Device::getOnlineStatus, 1));
        vo.setTotalDeviceNum(total);
        vo.setNormalDeviceNum(online);
        vo.setOfflineDeviceNum(total - online);
        vo.setFaultDeviceNum(0L);
        vo.setAlarmDeviceNum(0L);
        return vo;
    }

    private long countPatrolMerchants(Long merchantId, boolean multi, List<Long> tenantIds, LocalDateTime today) {
        QueryWrapper<PlanResult> wrapper = new QueryWrapper<>();
        wrapper.select("count(distinct merchant_id) as num")
                .in(multi, "tenant_id", tenantIds)
                .eq(merchantId != null, "merchant_id", merchantId)
                .ge("patrol_time", today);
        List<Map<String, Object>> rows = planResultMapper.selectMaps(wrapper);
        if (rows.isEmpty() || rows.get(0).get("num") == null) {
            return 0L;
        }
        return ((Number) rows.get(0).get("num")).longValue();
    }

    private MonitorHomeVO emptyMonitorHome() {
        MonitorHomeVO vo = new MonitorHomeVO();
        vo.setFireAlarmNum(0L);
        vo.setDealFireAlarmNum(0L);
        vo.setRealFireAlarmNum(0L);
        vo.setNoDealDangerNum(0L);
        vo.setAddDangerNum(0L);
        vo.setExpectDangerNum(0L);
        vo.setPatrolNum(0L);
        vo.setPatrolPercent(0.0);
        vo.setNspNum(0L);
        vo.setTotalDeviceNum(0L);
        vo.setNormalDeviceNum(0L);
        vo.setFaultDeviceNum(0L);
        vo.setAlarmDeviceNum(0L);
        vo.setOfflineDeviceNum(0L);
        return vo;
    }

    private OverviewVO buildOverview(Long merchantId, boolean multi, List<Long> tenantIds) {
        OverviewVO vo = new OverviewVO();
        vo.setAlarmTotal(alarmMapper.selectCount(new LambdaQueryWrapper<FireAlarm>()
                .in(multi, FireAlarm::getTenantId, tenantIds)
                .eq(merchantId != null, FireAlarm::getMerchantId, merchantId)));
        vo.setAlarmToday(alarmMapper.selectCount(new LambdaQueryWrapper<FireAlarm>()
                .in(multi, FireAlarm::getTenantId, tenantIds)
                .eq(merchantId != null, FireAlarm::getMerchantId, merchantId)
                .ge(FireAlarm::getAlarmTime, LocalDate.now().atStartOfDay())));
        vo.setAlarmPending(alarmMapper.selectCount(new LambdaQueryWrapper<FireAlarm>()
                .in(multi, FireAlarm::getTenantId, tenantIds)
                .eq(merchantId != null, FireAlarm::getMerchantId, merchantId)
                .eq(FireAlarm::getStatus, 0)));
        vo.setDeviceTotal(deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .in(multi, Device::getTenantId, tenantIds)
                .eq(merchantId != null, Device::getMerchantId, merchantId)));
        vo.setDeviceOnline(deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .in(multi, Device::getTenantId, tenantIds)
                .eq(merchantId != null, Device::getMerchantId, merchantId)
                .eq(Device::getOnlineStatus, 1)));
        vo.setDeviceOffline(vo.getDeviceTotal() - vo.getDeviceOnline());
        vo.setAlarmByType(alarmByType(merchantId, multi, tenantIds));
        vo.setDeviceByType(deviceByType(merchantId, multi, tenantIds));
        vo.setAlarmTrend(alarmTrend(merchantId, multi, tenantIds));
        return vo;
    }

    private OverviewVO emptyOverview() {
        OverviewVO vo = new OverviewVO();
        List<OverviewVO.TrendPoint> points = new ArrayList<>(7);
        LocalDate start = LocalDate.now().minusDays(6);
        for (int i = 0; i < 7; i++) {
            points.add(new OverviewVO.TrendPoint(start.plusDays(i).toString(), 0L));
        }
        vo.setAlarmTrend(points);
        vo.setAlarmByType(Collections.emptyList());
        vo.setDeviceByType(Collections.emptyList());
        return vo;
    }

    private Long currentMerchantId() {
        return null;
    }

    private List<OverviewVO.TypeCount> alarmByType(Long merchantId, boolean multi, List<Long> tenantIds) {
        QueryWrapper<FireAlarm> wrapper = new QueryWrapper<>();
        wrapper.select("alarm_type as name", "count(*) as value")
                .in(multi, "tenant_id", tenantIds)
                .eq(merchantId != null, "merchant_id", merchantId)
                .groupBy("alarm_type");
        return alarmMapper.selectMaps(wrapper).stream()
                .map(row -> new OverviewVO.TypeCount(
                        String.valueOf(row.get("name")),
                        ((Number) row.get("value")).longValue()))
                .collect(Collectors.toList());
    }

    private List<OverviewVO.TypeCount> deviceByType(Long merchantId, boolean multi, List<Long> tenantIds) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.select("type as name", "count(*) as value")
                .in(multi, "tenant_id", tenantIds)
                .eq(merchantId != null, "merchant_id", merchantId)
                .groupBy("type");
        return deviceMapper.selectMaps(wrapper).stream()
                .map(row -> new OverviewVO.TypeCount(
                        String.valueOf(row.get("name")),
                        ((Number) row.get("value")).longValue()))
                .collect(Collectors.toList());
    }

    private List<OverviewVO.TrendPoint> alarmTrend(Long merchantId, boolean multi, List<Long> tenantIds) {
        LocalDate start = LocalDate.now().minusDays(6);
        QueryWrapper<FireAlarm> wrapper = new QueryWrapper<>();
        wrapper.select("date(alarm_time) as d", "count(*) as value")
                .in(multi, "tenant_id", tenantIds)
                .eq(merchantId != null, "merchant_id", merchantId)
                .ge("alarm_time", start.atStartOfDay())
                .groupBy("date(alarm_time)");
        Map<String, Long> counts = new HashMap<>(8);
        for (Map<String, Object> row : alarmMapper.selectMaps(wrapper)) {
            counts.put(String.valueOf(row.get("d")), ((Number) row.get("value")).longValue());
        }
        List<OverviewVO.TrendPoint> points = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            points.add(new OverviewVO.TrendPoint(day.toString(), counts.getOrDefault(day.toString(), 0L)));
        }
        return points;
    }
}
