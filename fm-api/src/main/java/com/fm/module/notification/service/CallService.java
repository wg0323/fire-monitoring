package com.fm.module.notification.service;

import com.fm.module.notification.dto.CallRequestDTO;
import com.fm.module.notification.vo.CallResultVO;

/**
 * 外呼服务：按租户配置发起七陌外呼，回调更新通话记录。
 */
public interface CallService {

    CallResultVO dialOut(CallRequestDTO request);

    void handleCallback(String actionId, String calledNo, String state,
                        String begin, String end, String monitorFilename);
}
