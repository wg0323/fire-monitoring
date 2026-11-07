package com.fm.module.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.exception.BizException;
import com.fm.module.device.constant.DeviceTypeCodes;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.monitor.dto.MonitorQueryDTO;
import com.fm.module.monitor.entity.ElectricHistory;
import com.fm.module.monitor.entity.ElectricRealtime;
import com.fm.module.monitor.entity.WaterHistory;
import com.fm.module.monitor.entity.WaterRealtime;
import com.fm.module.monitor.mapper.ElectricHistoryMapper;
import com.fm.module.monitor.mapper.ElectricRealtimeMapper;
import com.fm.module.monitor.mapper.WaterHistoryMapper;
import com.fm.module.monitor.mapper.WaterRealtimeMapper;
import com.fm.module.monitor.service.ArchiveService;
import com.fm.module.monitor.converter.MonitorConverter;
import com.fm.module.monitor.service.MonitorService;
import com.fm.module.monitor.vo.ElectricTrendRow;
import com.fm.module.monitor.vo.MonitorDeviceVO;
import com.fm.module.monitor.vo.MonitorHistoryVO;
import com.fm.module.monitor.vo.MonitorTrendVO;
import com.fm.module.monitor.vo.TrendPointVO;
import com.fm.module.monitor.vo.WaterTrendRow;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 电气/水压查询：数据表无租户列，权限通过 b_device.sn 关联后按设备租户过滤。
 */
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private static final String RANGE_YEAR = "year";
    private static final String RANGE_MONTH = "month";
    private static final String UNIT_MPA = "MPa";
    private static final String UNIT_CM = "cm";
    private static final String UNIT_VOLT = "V";
    private static final String UNIT_AMPERE = "A";
    private static final String UNIT_CELSIUS = "℃";
    private static final BigDecimal FACTOR_PRESSURE = new BigDecimal("0.01");
    private static final BigDecimal FACTOR_TENTH = new BigDecimal("0.1");
    private static final int SCALE_TWO = 2;
    private static final int SCALE_ONE = 1;
    private static final int SCALE_ZERO = 0;

    private final ElectricRealtimeMapper electricRealtimeMapper;
    private final ElectricHistoryMapper electricHistoryMapper;
    private final WaterRealtimeMapper waterRealtimeMapper;
    private final WaterHistoryMapper waterHistoryMapper;
    private final DeviceMapper deviceMapper;
    private final MerchantMapper merchantMapper;
    private final DataScopeService dataScopeService;

    @Override
    public Page<MonitorDeviceVO> electricPage(MonitorQueryDTO query) {
        Page<ElectricRealtime> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ElectricRealtime> wrapper = new LambdaQueryWrapper<>();
        List<String> sns = filterSns(query.getKeyword());
        if (sns.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize());
        }
        wrapper.in(ElectricRealtime::getSn, sns);
        wrapper.orderByDesc(ElectricRealtime::getUpdateTime);
        Page<ElectricRealtime> rows = electricRealtimeMapper.selectPage(page, wrapper);
        List<MonitorDeviceVO> list = rows.getRecords().stream()
                .map(MonitorConverter::toDeviceVO)
                .collect(Collectors.toList());
        Page<MonitorDeviceVO> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(enrichDevices(list));
        return result;
    }

    @Override
    public List<MonitorHistoryVO> electricHistory(Long deviceId, int limit) {
        Device device = accessibleDevice(deviceId);
        if (device == null) {
            return Collections.emptyList();
        }
        List<ElectricHistory> rows = electricHistoryMapper.selectList(new LambdaQueryWrapper<ElectricHistory>()
                .eq(ElectricHistory::getSn, device.getSn())
                .orderByDesc(ElectricHistory::getMeasureTime)
                .last("LIMIT " + clampLimit(limit)));
        return rows.stream().map(MonitorConverter::toHistoryVO).collect(Collectors.toList());
    }

    @Override
    public Page<MonitorDeviceVO> waterPage(MonitorQueryDTO query) {
        Page<WaterRealtime> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<WaterRealtime> wrapper = new LambdaQueryWrapper<>();
        List<String> sns = filterSns(query.getKeyword());
        if (sns.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize());
        }
        wrapper.in(WaterRealtime::getSn, sns);
        wrapper.orderByDesc(WaterRealtime::getUpdateTime);
        Page<WaterRealtime> rows = waterRealtimeMapper.selectPage(page, wrapper);
        List<MonitorDeviceVO> list = rows.getRecords().stream()
                .map(MonitorConverter::toDeviceVO)
                .collect(Collectors.toList());
        Page<MonitorDeviceVO> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(enrichDevices(list));
        return result;
    }

    @Override
    public List<MonitorHistoryVO> waterHistory(Long deviceId, int limit) {
        Device device = accessibleDevice(deviceId);
        if (device == null) {
            return Collections.emptyList();
        }
        List<WaterHistory> rows = waterHistoryMapper.selectList(new LambdaQueryWrapper<WaterHistory>()
                .eq(WaterHistory::getSn, device.getSn())
                .orderByDesc(WaterHistory::getMeasureTime)
                .last("LIMIT " + clampLimit(limit)));
        return rows.stream().map(MonitorConverter::toHistoryVO).collect(Collectors.toList());
    }

    @Override
    public List<MonitorTrendVO> trend(Long deviceId, String range) {
        Device device = accessibleDevice(deviceId);
        if (device == null) {
            return Collections.emptyList();
        }
        return trend(device, range);
    }

    @Override
    public List<MonitorTrendVO> trend(Long deviceId, String range, String category) {
        Device device = accessibleDevice(deviceId);
        if (device == null) {
            return Collections.emptyList();
        }
        boolean water = ArchiveService.CATEGORY_WATER.equals(category);
        if (!water && !ArchiveService.CATEGORY_ELECTRIC.equals(category)) {
            throw new BizException("档案类型不支持");
        }
        if ((water && !DeviceTypeCodes.isWater(device.getType()))
                || (!water && !DeviceTypeCodes.isElectric(device.getType()))) {
            throw new BizException("设备类型与档案类型不匹配");
        }
        return trend(device, range);
    }

    private List<MonitorTrendVO> trend(Device device, String range) {
        TrendWindow window = resolveWindow(range);
        if (DeviceTypeCodes.isWater(device.getType())) {
            return waterTrend(device, window);
        }
        if (DeviceTypeCodes.isElectric(device.getType())) {
            return electricTrend(device, window);
        }
        return Collections.emptyList();
    }

    private List<MonitorTrendVO> waterTrend(Device device, TrendWindow window) {
        List<WaterTrendRow> rows = waterHistoryMapper.selectTrend(
                device.getSn(), window.format, window.start, window.end);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Integer measureType = rows.stream().map(WaterTrendRow::getMeasureType)
                .filter(Objects::nonNull).findFirst().orElse(1);
        boolean pressure = measureType == 1;
        MonitorTrendVO series = buildSeries(pressure ? "pressure" : "level",
                pressure ? "水压" : "液位", pressure ? UNIT_MPA : UNIT_CM,
                pressure ? FACTOR_PRESSURE : BigDecimal.ONE, pressure ? SCALE_TWO : SCALE_ZERO, window,
                toBucketMap(rows, WaterTrendRow::getBucket, WaterTrendRow::getAvgValue),
                toBucketMap(rows, WaterTrendRow::getBucket, WaterTrendRow::getMaxValue),
                toBucketMap(rows, WaterTrendRow::getBucket, WaterTrendRow::getMinValue));
        return series == null ? Collections.emptyList() : Collections.singletonList(series);
    }

    private List<MonitorTrendVO> electricTrend(Device device, TrendWindow window) {
        List<ElectricTrendRow> rows = electricHistoryMapper.selectTrend(
                device.getSn(), window.format, window.start, window.end);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<MonitorTrendVO> result = new ArrayList<>();
        addSeries(result, buildSeries("voltage", "电压（A 相）", UNIT_VOLT, BigDecimal.ONE, SCALE_ZERO, window,
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getVoltageAvg),
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getVoltageMax),
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getVoltageMin)));
        addSeries(result, buildSeries("current", "电流（A 相）", UNIT_AMPERE, FACTOR_TENTH, SCALE_ONE, window,
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getCurrentAvg),
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getCurrentMax),
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getCurrentMin)));
        addSeries(result, buildSeries("residual", "剩余电流", UNIT_AMPERE, FACTOR_TENTH, SCALE_ONE, window,
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getResidualAvg),
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getResidualMax),
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getResidualMin)));
        addSeries(result, buildSeries("temperature", "温度", UNIT_CELSIUS, FACTOR_TENTH, SCALE_ONE, window,
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getTemperatureAvg),
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getTemperatureMax),
                toBucketMap(rows, ElectricTrendRow::getBucket, ElectricTrendRow::getTemperatureMin)));
        return result;
    }

    private void addSeries(List<MonitorTrendVO> result, MonitorTrendVO series) {
        if (series != null) {
            result.add(series);
        }
    }

    private <T> Map<String, BigDecimal> toBucketMap(List<T> rows, Function<T, String> bucketFn,
                                                    Function<T, BigDecimal> valueFn) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (T row : rows) {
            BigDecimal value = valueFn.apply(row);
            if (value != null) {
                map.put(bucketFn.apply(row), value);
            }
        }
        return map;
    }

    private MonitorTrendVO buildSeries(String metric, String label, String unit, BigDecimal factor, int scale,
                                       TrendWindow window, Map<String, BigDecimal> avgMap,
                                       Map<String, BigDecimal> maxMap, Map<String, BigDecimal> minMap) {
        MonitorTrendVO vo = new MonitorTrendVO();
        vo.setMetric(metric);
        vo.setLabel(label);
        vo.setUnit(unit);
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (String bucket : window.buckets) {
            BigDecimal avg = avgMap.get(bucket);
            TrendPointVO point = new TrendPointVO();
            point.setTime(bucket);
            point.setValue(scale(avg, factor, scale));
            vo.getPoints().add(point);
            if (avg != null) {
                total = total.add(avg);
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        vo.setMax(scale(extreme(maxMap, true), factor, scale));
        vo.setMin(scale(extreme(minMap, false), factor, scale));
        vo.setAvg(scale(total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP), factor, scale));
        vo.setHasData(true);
        return vo;
    }

    private BigDecimal extreme(Map<String, BigDecimal> values, boolean max) {
        BigDecimal result = null;
        for (BigDecimal value : values.values()) {
            if (value == null) {
                continue;
            }
            if (result == null || (max ? value.compareTo(result) > 0 : value.compareTo(result) < 0)) {
                result = value;
            }
        }
        return result;
    }

    private BigDecimal scale(BigDecimal value, BigDecimal factor, int scale) {
        return value == null ? null : value.multiply(factor).setScale(scale, RoundingMode.HALF_UP);
    }

    private TrendWindow resolveWindow(String range) {
        String normalized = StringUtils.hasText(range) ? range.trim().toLowerCase() : "week";
        LocalDate today = LocalDate.now();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        List<String> buckets = new ArrayList<>();
        if (RANGE_YEAR.equals(normalized)) {
            YearMonth start = YearMonth.from(today).minusMonths(11);
            for (int i = 0; i < 12; i++) {
                buckets.add(start.plusMonths(i).toString());
            }
            return new TrendWindow("%Y-%m", buckets, start.atDay(1).atStartOfDay(), end);
        }
        int days = RANGE_MONTH.equals(normalized) ? 30 : 7;
        LocalDate start = today.minusDays(days - 1L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < days; i++) {
            buckets.add(start.plusDays(i).format(formatter));
        }
        return new TrendWindow("%Y-%m-%d", buckets, start.atStartOfDay(), end);
    }

    private static final class TrendWindow {
        private final String format;
        private final List<String> buckets;
        private final LocalDateTime start;
        private final LocalDateTime end;

        private TrendWindow(String format, List<String> buckets, LocalDateTime start, LocalDateTime end) {
            this.format = format;
            this.buckets = buckets;
            this.start = start;
            this.end = end;
        }
    }

    /**
     * 当前账号可见设备的 SN 列表。水电实时/历史表没有 tenant_id 列，租户插件无法过滤，
     * 因此无论有无关键字都必须用 b_device 反查出的 SN 白名单收口；
     * 若返回「不加条件」，无关键字的分页查询会直接泄露全平台监测数据。
     */
    private List<String> filterSns(String keyword) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Device::getSn);
        if (StringUtils.hasText(keyword)) {
            List<Long> merchantIds = merchantIdsByName(keyword);
            wrapper.and(w -> {
                w.like(Device::getName, keyword).or().like(Device::getSn, keyword)
                        .or().like(Device::getPosition, keyword);
                if (!merchantIds.isEmpty()) {
                    w.or().in(Device::getMerchantId, merchantIds);
                }
            });
        }
        return scopedDevices(wrapper).stream()
                .map(Device::getSn).filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private List<Long> merchantIdsByName(String keyword) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .like(Merchant::getName, keyword);
        List<Merchant> merchants = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> merchantMapper.selectList(wrapper))
                : merchantMapper.selectList(wrapper);
        return merchants.stream().map(Merchant::getId).collect(Collectors.toList());
    }

    private List<Device> scopedDevices(LambdaQueryWrapper<Device> wrapper) {
        if (!dataScopeService.isPlatform()) {
            return deviceMapper.selectList(wrapper);
        }
        List<Long> tenantIds = dataScopeService.authorizedTenantIds();
        if (tenantIds.isEmpty()) {
            return Collections.emptyList();
        }
        wrapper.in(Device::getTenantId, tenantIds);
        return dataScopeService.withIgnoreTenant(() -> deviceMapper.selectList(wrapper));
    }

    private List<MonitorDeviceVO> enrichDevices(List<MonitorDeviceVO> list) {
        if (list.isEmpty()) {
            return list;
        }
        List<String> sns = list.stream().map(MonitorDeviceVO::getSn).distinct().collect(Collectors.toList());
        Map<String, Device> devices = devicesBySns(sns);
        Map<Long, String> merchantNames = merchantNames(devices.values());
        return list.stream().filter(vo -> {
            Device device = devices.get(vo.getSn());
            if (device == null) {
                return false;
            }
            vo.setDeviceId(device.getId());
            vo.setName(device.getName());
            vo.setPosition(device.getPosition());
            vo.setOnlineStatus(device.getOnlineStatus());
            vo.setLastTime(device.getLastTime());
            vo.setMerchantName(merchantNames.get(device.getMerchantId()));
            return true;
        }).collect(Collectors.toList());
    }

    private Map<String, Device> devicesBySns(List<String> sns) {
        if (sns.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<Device>().in(Device::getSn, sns);
        List<Device> devices;
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return Collections.emptyMap();
            }
            wrapper.in(Device::getTenantId, tenantIds);
            devices = dataScopeService.withIgnoreTenant(() -> deviceMapper.selectList(wrapper));
        } else {
            devices = deviceMapper.selectList(wrapper);
        }
        return devices.stream().collect(Collectors.toMap(Device::getSn, Function.identity(), (a, b) -> a));
    }

    private Map<Long, String> merchantNames(java.util.Collection<Device> devices) {
        List<Long> ids = devices.stream().map(Device::getMerchantId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Merchant> merchants = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> merchantMapper.selectBatchIds(ids))
                : merchantMapper.selectBatchIds(ids);
        return merchants.stream().collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
    }

    private Device accessibleDevice(Long deviceId) {
        if (deviceId == null) {
            return null;
        }
        Device device;
        if (!dataScopeService.isPlatform()) {
            device = deviceMapper.selectById(deviceId);
        } else {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return null;
            }
            device = dataScopeService.withIgnoreTenant(() -> deviceMapper.selectById(deviceId));
            if (device != null && !tenantIds.contains(device.getTenantId())) {
                return null;
            }
        }
        if (device == null) {
            return null;
        }
        List<Long> merchantIds = dataScopeService.scopedMerchantIds();
        if (merchantIds != null
                && (device.getMerchantId() == null || !merchantIds.contains(device.getMerchantId()))) {
            return null;
        }
        return device;
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }
}
