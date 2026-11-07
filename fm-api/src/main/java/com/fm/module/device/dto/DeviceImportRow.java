package com.fm.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 设备导入/导出共用行模型，全部按文本读取，便于逐行给出校验提示。
 */
@Schema(description = "设备导入/导出共用行模型，全部按文本读取，便于逐行给出校验提示")
@Data
public class DeviceImportRow {

    @ExcelProperty(value = "设备编号", index = 0)
    @ColumnWidth(24)
    @Schema(description = "设备 SN")
    private String sn;

    @ExcelProperty(value = "设备名称", index = 1)
    @ColumnWidth(20)
    @Schema(description = "名称")
    private String name;

    @ExcelProperty(value = "设备类型", index = 2)
    @ColumnWidth(16)
    @Schema(description = "类型名称")
    private String typeName;

    @ExcelProperty(value = "所属商户", index = 3)
    @ColumnWidth(24)
    @Schema(description = "企业名称")
    private String merchantName;

    @ExcelProperty(value = "制造商", index = 4)
    @ColumnWidth(12)
    @Schema(description = "厂商名称")
    private String makerName;

    @ExcelProperty(value = "定位方式", index = 5)
    @ColumnWidth(14)
    @Schema(description = "位置类型名称")
    private String locationTypeName;

    @ExcelProperty(value = "经度", index = 6)
    @ColumnWidth(14)
    @Schema(description = "经度")
    private String lng;

    @ExcelProperty(value = "纬度", index = 7)
    @ColumnWidth(14)
    @Schema(description = "纬度")
    private String lat;

    @ExcelProperty(value = "楼宇", index = 8)
    @ColumnWidth(16)
    @Schema(description = "楼宇名称")
    private String buildingName;

    @ExcelProperty(value = "楼层", index = 9)
    @ColumnWidth(10)
    @Schema(description = "楼层名称")
    private String floorName;

    @ExcelProperty(value = "楼层X坐标", index = 10)
    @ColumnWidth(12)
    @Schema(description = "楼层坐标 X")
    private String floorX;

    @ExcelProperty(value = "楼层Y坐标", index = 11)
    @ColumnWidth(12)
    @Schema(description = "楼层坐标 Y")
    private String floorY;

    @ExcelProperty(value = "安装位置", index = 12)
    @ColumnWidth(24)
    @Schema(description = "具体位置")
    private String position;

    @ExcelProperty(value = "SIM卡", index = 13)
    @ColumnWidth(22)
    @Schema(description = "SIM 卡号")
    private String sim;

    @ExcelProperty(value = "4G卡号", index = 14)
    @ColumnWidth(18)
    @Schema(description = "卡号")
    private String cardNo;

    @ExcelProperty(value = "通道数", index = 15)
    @ColumnWidth(10)
    @Schema(description = "通道号")
    private String channelNum;

    @ExcelProperty(value = "激活日期", index = 16)
    @ColumnWidth(14)
    @Schema(description = "启用日期")
    private String activeDate;

    @ExcelProperty(value = "安装日期", index = 17)
    @ColumnWidth(14)
    @Schema(description = "安装日期")
    private String installDate;

    @ExcelProperty(value = "状态", index = 18)
    @ColumnWidth(10)
    @Schema(description = "状态名称")
    private String statusName;
}
