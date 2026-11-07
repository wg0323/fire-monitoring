package com.fm.module.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.device.entity.Device;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
