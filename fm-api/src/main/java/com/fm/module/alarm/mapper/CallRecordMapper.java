package com.fm.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.alarm.entity.CallRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通话记录 Mapper。
 * 租户隔离统一交由 MyBatis-Plus 租户插件处理；外呼回调无登录态，
 * 由 CallServiceImpl.handleCallback 显式设置 ignoreTenant 后按 action_id 定位记录。
 */
@Mapper
public interface CallRecordMapper extends BaseMapper<CallRecord> {
}
