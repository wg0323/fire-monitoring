package com.fm.module.notification.converter;

import com.fm.module.notification.entity.SevenMoorConfig;
import com.fm.module.notification.vo.CallConfigVO;
import com.fm.module.notification.vo.CallResultVO;

/**
 * 外呼对象转换。
 */
public final class CallConverter {

    private CallConverter() {
    }

    public static CallResultVO result(boolean succeed, String message) {
        CallResultVO vo = new CallResultVO();
        vo.setSucceed(succeed);
        vo.setMessage(message);
        return vo;
    }

    public static CallConfigVO toVO(SevenMoorConfig config) {
        CallConfigVO vo = new CallConfigVO();
        vo.setConfigured(config != null);
        if (config != null) {
            vo.setAccountId(config.getAccountId());
            vo.setHost(config.getHost());
            vo.setFromExten(config.getFromExten());
        }
        return vo;
    }
}
