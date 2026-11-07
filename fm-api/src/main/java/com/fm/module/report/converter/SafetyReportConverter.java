package com.fm.module.report.converter;

import com.fm.module.report.entity.SafetyReport;
import com.fm.module.report.vo.SafetyReportVO;
import org.springframework.beans.BeanUtils;

/**
 * 安全报告对象转换。
 */
public final class SafetyReportConverter {

    private SafetyReportConverter() {
    }

    public static SafetyReportVO toVO(SafetyReport report) {
        if (report == null) {
            return null;
        }
        SafetyReportVO vo = new SafetyReportVO();
        BeanUtils.copyProperties(report, vo);
        return vo;
    }
}
