package com.fm.module.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fm.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 登录时用户尚未建立租户上下文，必须忽略租户插件 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE user_name = #{userName} AND deleted = 0 LIMIT 1")
    SysUser selectByUserName(@Param("userName") String userName);

    /** 小程序 openid 登录同样在建立租户上下文之前 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE wx_app_openid = #{openid} AND deleted = 0 LIMIT 1")
    SysUser selectByWxAppOpenid(@Param("openid") String openid);

    /** 手机号白名单查询：返回全部匹配行以识别重复手机号 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE phone = #{phone} AND deleted = 0")
    List<SysUser> selectByPhone(@Param("phone") String phone);

    /** 手机号唯一索引覆盖逻辑删除行，开户占用检查必须查全表 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, user_name, phone, merchant_id FROM sys_user WHERE phone = #{phone}")
    List<SysUser> selectByPhoneIncludeDeleted(@Param("phone") String phone);

    /** 登录名唯一索引同样覆盖逻辑删除行 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM sys_user WHERE user_name = #{userName}")
    long countByUserNameIncludeDeleted(@Param("userName") String userName);
}
