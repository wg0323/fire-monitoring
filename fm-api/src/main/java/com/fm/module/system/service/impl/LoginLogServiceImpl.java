package com.fm.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.common.context.TenantContext;
import com.fm.module.system.dto.LoginLogQueryDTO;
import com.fm.module.system.entity.LogLogin;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.mapper.LogLoginMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.service.DataScopeService;
import com.fm.module.system.service.LoginLogService;
import com.fm.module.system.converter.AuthConverter;
import com.fm.module.system.vo.LoginLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 登录日志：log_login 不参与租户插件，需显式按平台/租户/监管授权范围过滤。
 */
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LogLoginMapper logLoginMapper;
    private final SysUserMapper userMapper;
    private final DataScopeService dataScopeService;

    @Override
    public Page<LoginLogVO> page(LoginLogQueryDTO query) {
        LambdaQueryWrapper<LogLogin> wrapper = new LambdaQueryWrapper<>();
        boolean multi = dataScopeService.isPlatform();
        List<Long> scopeTenantIds = multi ? dataScopeService.authorizedTenantIds() : Collections.emptyList();
        if (multi && scopeTenantIds.isEmpty()) {
            return new Page<>(query.getPageNum(), query.getPageSize());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<SysUser>()
                    .in(multi, SysUser::getTenantId, scopeTenantIds)
                    .and(w -> w.like(SysUser::getUserName, query.getKeyword())
                            .or().like(SysUser::getRealName, query.getKeyword()));
            List<SysUser> users = multi
                    ? dataScopeService.withIgnoreTenant(() -> userMapper.selectList(userWrapper))
                    : userMapper.selectList(userWrapper);
            if (users.isEmpty()) {
                return new Page<>(query.getPageNum(), query.getPageSize());
            }
            wrapper.in(LogLogin::getUserId, users.stream().map(SysUser::getId).collect(Collectors.toList()));
        }
        wrapper.eq(StringUtils.hasText(query.getUserType()), LogLogin::getUserType, query.getUserType())
                .eq(query.getLoginResult() != null, LogLogin::getLoginResult, query.getLoginResult());

        if (!"PLATFORM".equals(TenantContext.getUserType())) {
            if (multi) {
                wrapper.in(LogLogin::getTenantId, scopeTenantIds);
            } else {
                wrapper.eq(LogLogin::getTenantId, TenantContext.getTenantId());
            }
        }
        wrapper.orderByDesc(LogLogin::getLoginTime);
        Page<LogLogin> page = logLoginMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        Page<LoginLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    private List<LoginLogVO> toVOList(List<LogLogin> logs) {
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = logs.stream().map(LogLogin::getUserId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, SysUser> users;
        if (userIds.isEmpty()) {
            users = Collections.emptyMap();
        } else if (dataScopeService.isPlatform()) {
            users = dataScopeService.withIgnoreTenant(() -> userMapper.selectBatchIds(userIds)).stream()
                    .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));
        } else {
            users = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));
        }
        return logs.stream().map(log -> AuthConverter.toLoginLogVO(log, users.get(log.getUserId())))
                .collect(Collectors.toList());
    }
}
