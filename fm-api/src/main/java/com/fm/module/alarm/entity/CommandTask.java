package com.fm.module.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 下行指令任务：publish 后异步等待采集器回执，前端可查状态。
 */
@Data
@TableName("b_command_task")
public class CommandTask implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";
    public static final String STATUS_TIMEOUT = "TIMEOUT";

    public static final String TYPE_TIME_SYNC = "TIME_SYNC";
    public static final String TYPE_PARAM_SET = "PARAM_SET";
    public static final String TYPE_PARAM_QUERY = "PARAM_QUERY";
    public static final String TYPE_SILENCE = "SILENCE";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String sn;
    private String commandType;
    private String payload;
    private String status;
    private LocalDateTime reqTime;
    private LocalDateTime respTime;
    private String result;
    private Integer retryCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 
