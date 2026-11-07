package com.fm.module.danger.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 隐患批量指派结果：逐条独立执行，返回成功数与被跳过的明细。
 */
@Schema(description = "隐患批量指派结果")
@Data
public class DangerBatchAssignResultVO {

    @Schema(description = "提交条数")
    private Integer total;
    @Schema(description = "成功条数")
    private Integer successCount;
    @Schema(description = "失败条数")
    private Integer failedCount;
    @Schema(description = "失败明细")
    private List<Failure> failures = new ArrayList<>();

    /**
     * 单条失败明细：隐患 ID 与可展示的原因。
     */
    @Data
    @Schema(description = "批量指派失败明细")
    public static class Failure {

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "隐患 ID")
        private Long id;
        @Schema(description = "失败原因")
        private String reason;

        public Failure() {
        }

        public Failure(Long id, String reason) {
            this.id = id;
            this.reason = reason;
        }
    }
}
