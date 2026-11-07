package com.fm.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.report.converter.SafetyReportConverter;
import com.fm.module.report.dto.SafetyReportQueryDTO;
import com.fm.module.report.dto.SafetyReportSaveDTO;
import com.fm.module.report.entity.SafetyReport;
import com.fm.module.report.mapper.SafetyReportMapper;
import com.fm.module.report.service.SafetyReportService;
import com.fm.module.report.vo.SafetyReportVO;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 安全报告：运营中心维护，平台/监管按范围只读。
 */
@Service
@RequiredArgsConstructor
public class SafetyReportServiceImpl extends ServiceImpl<SafetyReportMapper, SafetyReport>
        implements SafetyReportService {

    private final DataScopeService dataScopeService;

    @Override
    public Page<SafetyReportVO> page(SafetyReportQueryDTO query) {
        LambdaQueryWrapper<SafetyReport> wrapper = new LambdaQueryWrapper<SafetyReport>()
                .eq(query.getType() != null, SafetyReport::getType, query.getType())
                .like(StringUtils.hasText(query.getKeyword()), SafetyReport::getReportName, query.getKeyword())
                .orderByDesc(SafetyReport::getReportTime)
                .orderByDesc(SafetyReport::getId);
        Page<SafetyReport> page;
        if (dataScopeService.isPlatform()) {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return new Page<>(query.getPageNum(), query.getPageSize());
            }
            wrapper.in(SafetyReport::getTenantId, tenantIds);
            page = dataScopeService.withIgnoreTenant(() -> baseMapper.selectPage(
                    new Page<>(query.getPageNum(), query.getPageSize()), wrapper));
        } else {
            page = baseMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        }
        Page<SafetyReportVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(SafetyReportConverter::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional
    public Long create(SafetyReportSaveDTO request) {
        if (!"TENANT".equals(TenantContext.getUserType())) {
            throw new BizException(403, "请使用运营中心账号维护安全报告");
        }
        if (request.getType() == null
                || (request.getType() != SafetyReport.TYPE_MONTH && request.getType() != SafetyReport.TYPE_YEAR)) {
            throw new BizException("报告类型只能是月报或年报");
        }
        if (!StringUtils.hasText(request.getReportName())) {
            throw new BizException("报告名称不能为空");
        }
        SafetyReport report = new SafetyReport();
        report.setTenantId(TenantContext.getTenantId());
        report.setType(request.getType());
        report.setReportName(request.getReportName());
        report.setFilePath(request.getFilePath());
        report.setReportTime(request.getReportTime() == null ? LocalDate.now() : request.getReportTime());
        baseMapper.insert(report);
        return report.getId();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!"TENANT".equals(TenantContext.getUserType())) {
            throw new BizException(403, "请使用运营中心账号删除安全报告");
        }
        SafetyReport report = baseMapper.selectById(id);
        if (report == null) {
            throw new BizException("安全报告不存在");
        }
        baseMapper.deleteById(id);
    }
}
