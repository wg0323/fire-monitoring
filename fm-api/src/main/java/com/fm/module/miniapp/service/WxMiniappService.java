package com.fm.module.miniapp.service;

import com.fm.module.miniapp.dto.PhoneLoginDTO;
import com.fm.module.system.vo.LoginVO;

/**
 * 微信小程序登录：手机号白名单或已绑定 openid。
 */
public interface WxMiniappService {

    LoginVO login(String code, String ip);

    LoginVO phoneLogin(PhoneLoginDTO request, String ip);
}
