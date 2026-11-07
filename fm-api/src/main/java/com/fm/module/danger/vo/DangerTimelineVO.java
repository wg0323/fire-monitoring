package com.fm.module.danger.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 隐患时间线节点：固定节点始终返回，未发生节点 occurred=false。
 */
@Data
@Schema(description = "隐患时间线节点：固定节点始终返回，未发生节点 occurred=false")
public class DangerTimelineVO {

    public static final String NODE_REPORT = "REPORT";
    public static final String NODE_ALARM = "ALARM";
    public static final String NODE_CONVERT = "CONVERT";
    public static final String NODE_ASSIGN = "ASSIGN";
    public static final String NODE_DEAL = "DEAL";
    public static final String NODE_AUDIT = "AUDIT";

    @Schema(description = "节点键")
    private String nodeKey;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "结果")
    private Integer result;
    @Schema(description = "字段 occurred")
    private Boolean occurred;
    @Schema(description = "时间")
    private LocalDateTime time;
    @Schema(description = "操作人")
    private String operator;
    @Schema(description = "内容")
    private String content;
    @Schema(description = "字段 pics")
    private List<String> pics;
    @Schema(description = "扩展信息")
    private String extra;
}
