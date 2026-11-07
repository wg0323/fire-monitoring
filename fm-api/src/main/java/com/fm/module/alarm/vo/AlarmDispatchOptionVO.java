package com.fm.module.alarm.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 派单选项：工作区及其外勤成员。
 */
@Data
@Schema(description = "派单选项：工作区及其外勤成员")
public class AlarmDispatchOptionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "工作区域主键ID")
    private Long workAreaId;
    @Schema(description = "工作区域名称")
    private String workAreaName;
    @Schema(description = "字段 handlers")
    private List<Handler> handlers;

    /**
     * 工作区成员。
     */
    @Data
    @Schema(description = "工作区成员")
    public static class Handler {
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "主键ID")
        private Long id;
        @Schema(description = "名称")
        private String name;
        @Schema(description = "手机号")
        private String phone;
    }
}
