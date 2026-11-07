package com.fm.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.exception.BizException;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.device.constant.DeviceTypeCodes;
import com.fm.module.device.converter.DeviceConverter;
import com.fm.module.device.entity.DeviceRealtime;
import com.fm.module.device.entity.DictAlarmType;
import com.fm.module.device.mapper.DeviceRealtimeMapper;
import com.fm.module.device.mapper.DictAlarmTypeMapper;
import com.fm.module.device.service.DeviceDetailService;
import com.fm.module.device.service.DeviceService;
import com.fm.module.device.vo.DeviceAlarmBriefVO;
import com.fm.module.device.vo.DeviceDetailVO;
import com.fm.module.device.converter.DeviceConverter;
import com.fm.module.device.vo.DeviceSnapshotItemVO;
import com.fm.module.device.vo.DeviceVO;
import com.fm.module.monitor.entity.ElectricRealtime;
import com.fm.module.monitor.entity.WaterRealtime;
import com.fm.module.monitor.mapper.ElectricRealtimeMapper;
import com.fm.module.monitor.mapper.WaterRealtimeMapper;
import com.fm.module.monitor.service.MonitorService;
import com.fm.module.monitor.vo.MonitorTrendVO;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备详情聚合：水/电走独立实时表，其余设备展示 JSON 快照。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDetailServiceImpl implements DeviceDetailService {

    private static final int RECENT_ALARM_LIMIT = 10;
    private static final BigDecimal FACTOR_PRESSURE = new BigDecimal("0.01");
    private static final BigDecimal FACTOR_TENTH = new BigDecimal("0.1");
    private static final String UNIT_MPA = "MPa";
    private static final String UNIT_CM = "cm";
    private static final String UNIT_VOLT = "V";
    private static final String UNIT_AMPERE = "A";
    private static final String UNIT_CELSIUS = "℃";
    private static final int SCALE_ZERO = 0;
    private static final int SCALE_ONE = 1;
    private static final int SCALE_TWO = 2;

    private final DeviceService deviceService;
    private final MonitorService monitorService;
    private final WaterRealtimeMapper waterRealtimeMapper;
    private final ElectricRealtimeMapper electricRealtimeMapper;
    private final DeviceRealtimeMapper deviceRealtimeMapper;
    private final FireAlarmMapper fireAlarmMapper;
    private final DictAlarmTypeMapper dictAlarmTypeMapper;
    private final DataScopeService dataScopeService;
    private final ObjectMapper objectMapper;

    @Override
    public DeviceDetailVO detail(Long id, String range) {
        DeviceVO base = deviceService.getVOById(id);
        if (base == null) {
            throw new BizException("设备不存在或无权操作");
        }
        DeviceDetailVO vo = DeviceConverter.toDetailVO(base);
        vo.setSnapshot(snapshot(base));
        vo.setTrends(monitorService.trend(id, range));
        vo.setRecentAlarms(recentAlarms(base.getSn()));
        return vo;
    }

    @Override
    public List<MonitorTrendVO> trend(Long id, String range) {
        return monitorService.trend(id, range);
    }

    private List<DeviceSnapshotItemVO> snapshot(DeviceVO device) {
        if (DeviceTypeCodes.isWater(device.getType())) {
            return waterSnapshot(device.getSn());
        }
        if (DeviceTypeCodes.isElectric(device.getType())) {
            return electricSnapshot(device.getSn());
        }
        return realtimeSnapshot(device.getSn());
    }

    private List<DeviceSnapshotItemVO> waterSnapshot(String sn) {
        WaterRealtime row = waterRealtimeMapper.selectOne(new LambdaQueryWrapper<WaterRealtime>()
                .eq(WaterRealtime::getSn, sn));
        if (row == null) {
            return Collections.emptyList();
        }
        boolean pressure = row.getMeasureType() == null || row.getMeasureType() == 1;
        List<DeviceSnapshotItemVO> items = new ArrayList<>();
        addItem(items, item(pressure ? "水压" : "液位", row.getMeasureValue(),
                pressure ? UNIT_MPA : UNIT_CM, pressure ? FACTOR_PRESSURE : BigDecimal.ONE,
                pressure ? SCALE_TWO : SCALE_ZERO, row.getMeasureTime()));
        return items;
    }

    private List<DeviceSnapshotItemVO> electricSnapshot(String sn) {
        ElectricRealtime row = electricRealtimeMapper.selectOne(new LambdaQueryWrapper<ElectricRealtime>()
                .eq(ElectricRealtime::getSn, sn));
        if (row == null) {
            return Collections.emptyList();
        }
        LocalDateTime time = row.getMeasureTime();
        List<DeviceSnapshotItemVO> items = new ArrayList<>();
        addItem(items, item("电压 A", row.getVoltageA(), UNIT_VOLT, BigDecimal.ONE, SCALE_ZERO, time));
        addItem(items, item("电压 B", row.getVoltageB(), UNIT_VOLT, BigDecimal.ONE, SCALE_ZERO, time));
        addItem(items, item("电压 C", row.getVoltageC(), UNIT_VOLT, BigDecimal.ONE, SCALE_ZERO, time));
        addItem(items, item("电流 A", row.getCurrentA(), UNIT_AMPERE, FACTOR_TENTH, SCALE_ONE, time));
        addItem(items, item("电流 B", row.getCurrentB(), UNIT_AMPERE, FACTOR_TENTH, SCALE_ONE, time));
        addItem(items, item("电流 C", row.getCurrentC(), UNIT_AMPERE, FACTOR_TENTH, SCALE_ONE, time));
        addItem(items, item("剩余电流", row.getResidualCurrent(), UNIT_AMPERE, FACTOR_TENTH, SCALE_ONE, time));
        addItem(items, item("温度", row.getTemperature(), UNIT_CELSIUS, FACTOR_TENTH, SCALE_ONE, time));
        return items;
    }

    private List<DeviceSnapshotItemVO> realtimeSnapshot(String sn) {
        DeviceRealtime row = deviceRealtimeMapper.selectOne(new LambdaQueryWrapper<DeviceRealtime>()
                .eq(DeviceRealtime::getSn, sn));
        if (row == null || !StringUtils.hasText(row.getData())) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(row.getData());
            if (!root.isObject()) {
                return Collections.singletonList(DeviceConverter.toSnapshotItem("数据", row.getData(), row.getReportTime()));
            }
            List<DeviceSnapshotItemVO> items = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getValue().isValueNode()) {
                    items.add(DeviceConverter.toSnapshotItem(field.getKey(), field.getValue().asText(),
                            row.getReportTime()));
                }
            }
            return items;
        } catch (JsonProcessingException ex) {
            log.warn("设备快照解析失败，sn={}", sn, ex);
            return Collections.emptyList();
        }
    }

    private DeviceSnapshotItemVO item(String label, Integer raw, String unit, BigDecimal factor, int scale,
                                      LocalDateTime time) {
        if (raw == null) {
            return null;
        }
        DeviceSnapshotItemVO item = new DeviceSnapshotItemVO();
        item.setLabel(label);
        item.setValue(BigDecimal.valueOf(raw).multiply(factor).setScale(scale, RoundingMode.HALF_UP).toPlainString());
        item.setUnit(unit);
        item.setTime(time);
        return item;
    }

    private void addItem(List<DeviceSnapshotItemVO> items, DeviceSnapshotItemVO item) {
        if (item != null) {
            items.add(item);
        }
    }

    private List<DeviceAlarmBriefVO> recentAlarms(String sn) {
        LambdaQueryWrapper<FireAlarm> wrapper = new LambdaQueryWrapper<FireAlarm>()
                .eq(FireAlarm::getSn, sn)
                .orderByDesc(FireAlarm::getAlarmTime)
                .last("LIMIT " + RECENT_ALARM_LIMIT);
        List<FireAlarm> alarms = dataScopeService.isPlatform()
                ? dataScopeService.withIgnoreTenant(() -> fireAlarmMapper.selectList(wrapper))
                : fireAlarmMapper.selectList(wrapper);
        if (alarms.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> codes = alarms.stream().map(FireAlarm::getAlarmType)
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        Map<String, String> typeNames = codes.isEmpty() ? Collections.emptyMap()
                : dictAlarmTypeMapper.selectList(new LambdaQueryWrapper<DictAlarmType>()
                        .in(DictAlarmType::getCode, codes))
                .stream().collect(Collectors.toMap(DictAlarmType::getCode, DictAlarmType::getName, (a, b) -> a));
        return alarms.stream()
                .map(alarm -> DeviceConverter.toAlarmBriefVO(alarm, typeNames.get(alarm.getAlarmType())))
                .collect(Collectors.toList());
    }
}
