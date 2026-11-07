package com.fm.module.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.report.dto.SafetyReportQueryDTO;
import com.fm.module.report.dto.SafetyReportSaveDTO;
import com.fm.module.report.entity.SafetyReport;
import com.fm.module.report.vo.SafetyReportVO;

/**
 * 安全报告服务。
 */
public interface SafetyReportService extends IService<SafetyReport> {

    Page<SafetyReportVO> page(SafetyReportQueryDTO query);

    Long create(SafetyReportSaveDTO request);

    void delete(Long id);
}
