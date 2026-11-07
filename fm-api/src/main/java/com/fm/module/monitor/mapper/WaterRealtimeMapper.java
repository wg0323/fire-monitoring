package com.fm.module.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.monitor.entity.WaterRealtime;
import org.apache.ibatis.annotations.Mapper;

/**
 * 水压/液位实时数据 Mapper。
 */
@Mapper
public interface WaterRealtimeMapper extends BaseMapper<WaterRealtime> {
}
