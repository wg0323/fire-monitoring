package com.fm.module.device.converter;

import com.fm.module.device.dto.DeviceImportRow;
import com.fm.module.device.entity.Device;
import com.fm.module.device.vo.DeviceVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备导出行转换：DeviceVO 到 DeviceImportRow。
 *
 * <p>存在非等价映射（字典名回退、定位方式码值翻译、数值与日期格式化），按对象转换分档规则
 * 集中到 converter 的静态方法，避免在 Service 中散落逐字段 setter 拷贝。</p>
 */
public final class DeviceImportRowConverter {

    private static final int STATUS_DISABLED = 0;
    private static final String LOCATION_NONE = "未设置";
    private static final String LOCATION_GPS = "室外GPS";
    private static final String LOCATION_FLOOR = "室内楼层图";

    private DeviceImportRowConverter() {
    }

    public static DeviceImportRow toImportRow(DeviceVO device) {
        DeviceImportRow row = new DeviceImportRow();
        row.setSn(device.getSn());
        row.setName(device.getName());
        row.setTypeName(StringUtils.hasText(device.getTypeName()) ? device.getTypeName() : device.getType());
        row.setMerchantName(device.getMerchantName());
        row.setMakerName(StringUtils.hasText(device.getMakerName()) ? device.getMakerName() : device.getMaker());
        row.setLocationTypeName(locationTypeName(device.getLocationType()));
        row.setLng(formatDouble(device.getLng()));
        row.setLat(formatDouble(device.getLat()));
        row.setBuildingName(device.getBuildingName());
        row.setFloorName(device.getFloorName());
        row.setFloorX(formatDecimal(device.getFloorX()));
        row.setFloorY(formatDecimal(device.getFloorY()));
        row.setPosition(device.getPosition());
        row.setSim(device.getSim());
        row.setCardNo(device.getCardNo());
        row.setChannelNum(device.getChannelNum() == null ? "1" : String.valueOf(device.getChannelNum()));
        row.setActiveDate(formatDate(device.getActiveDate()));
        row.setInstallDate(formatDate(device.getInstallDate()));
        row.setStatusName(device.getStatus() != null && device.getStatus() == STATUS_DISABLED ? "禁用" : "启用");
        return row;
    }

    private static String locationTypeName(Integer locationType) {
        if (locationType == null || locationType == Device.LOCATION_NONE) {
            return LOCATION_NONE;
        }
        if (locationType == Device.LOCATION_GPS) {
            return LOCATION_GPS;
        }
        return LOCATION_FLOOR;
    }

    private static String formatDate(LocalDateTime time) {
        return time == null ? null : time.toLocalDate().toString();
    }

    private static String formatDouble(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String formatDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
