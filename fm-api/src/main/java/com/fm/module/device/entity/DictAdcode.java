package com.fm.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 字典：行政区划。
 */
@Data
@TableName("dict_adcode")
public class DictAdcode {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String parentId;
    private String parentCode;
    private String shortName;
    private String pinyin;
    private Double lat;
    private Double lng;
    private Integer levelType;
}
