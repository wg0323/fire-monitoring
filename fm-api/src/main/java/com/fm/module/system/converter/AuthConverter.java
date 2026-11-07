package com.fm.module.system.converter;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.fm.module.device.vo.MerchantOptionVO;
import com.fm.module.system.entity.LogLogin;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.vo.LoginLogVO;
import com.fm.module.system.vo.LoginVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证与登录日志对象转换。
 */
public final class AuthConverter {

    private AuthConverter() {
    }

    public static LoginVO toLoginVO(SysUser user, SaTokenInfo tokenInfo, String dataScope,
                                    List<String> roles, List<String> permissions,
                                    Long currentMerchantId, List<MerchantOptionVO> merchants) {
        LoginVO vo = new LoginVO();
        vo.setTokenName(tokenInfo.getTokenName());
        vo.setTokenValue(tokenInfo.getTokenValue());
        vo.setUserId(user.getId());
        vo.setUserName(user.getUserName());
        vo.setRealName(user.getRealName());
        vo.setUserType(user.getUserType());
        vo.setDataScope(dataScope);
        vo.setCurrentMerchantId(currentMerchantId);
        vo.setMerchants(merchants);
        vo.setRoles(new ArrayList<>(roles));
        vo.setPermissions(permissions);
        return vo;
    }

    public static LoginLogVO toLoginLogVO(LogLogin log, SysUser user) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(log.getId());
        vo.setTenantId(log.getTenantId());
        vo.setUserId(log.getUserId());
        vo.setUserType(log.getUserType());
        vo.setLoginTime(log.getLoginTime());
        vo.setLoginIp(log.getLoginIp());
        vo.setLoginResult(log.getLoginResult());
        vo.setFailureReason(log.getFailureReason());
        if (user != null) {
            vo.setUserName(user.getUserName());
            vo.setRealName(user.getRealName());
        }
        return vo;
    }
}
