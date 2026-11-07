package com.fm.module.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.monitor.entity.ElectricRealtime;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电气火灾实时数据 Mapper。
 */
@Mapper
public interface ElectricRealtimeMapper extends BaseMapper<ElectricRealtime> {
}
