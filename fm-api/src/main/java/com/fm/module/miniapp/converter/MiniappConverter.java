package com.fm.module.miniapp.converter;

import com.fm.module.alarm.vo.AlarmVO;
import com.fm.module.device.entity.Merchant;
import com.fm.module.miniapp.vo.MerchantOptionVO;
import com.fm.module.miniapp.vo.MpHomeSummaryVO;
import com.fm.module.miniapp.vo.UploadVO;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * 小程序对象转换。
 */
public final class MiniappConverter {

    private MiniappConverter() {
    }

    public static MpHomeSummaryVO toSummaryVO(Long alarmCount, Long dangerCount, Long deviceCount,
                                              Long offlineCount, Long workOrderCount, Long inspectionCount,
                                              List<AlarmVO> latestAlarms,
                                              List<MpHomeSummaryVO.DangerBrief> latestDangers) {
        MpHomeSummaryVO vo = new MpHomeSummaryVO();
        vo.setAlarmCount(alarmCount);
        vo.setDangerCount(dangerCount);
        vo.setDeviceCount(deviceCount);
        vo.setOfflineCount(offlineCount);
        vo.setWorkOrderCount(workOrderCount);
        vo.setInspectionCount(inspectionCount);
        vo.setLatestAlarms(latestAlarms);
        vo.setLatestDangers(latestDangers);
        return vo;
    }

    public static MerchantOptionVO toMerchantOptionVO(Merchant merchant) {
        MerchantOptionVO vo = new MerchantOptionVO();
        BeanUtils.copyProperties(merchant, vo);
        return vo;
    }

    public static UploadVO toUploadVO(String url) {
        UploadVO vo = new UploadVO();
        vo.setUrl(url);
        return vo;
    }
}
