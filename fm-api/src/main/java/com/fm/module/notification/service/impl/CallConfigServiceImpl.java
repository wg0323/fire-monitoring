package com.fm.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.notification.entity.SevenMoorConfig;
import com.fm.module.notification.entity.TenantCallConfig;
import com.fm.module.notification.mapper.TenantCallConfigMapper;
import com.fm.module.notification.service.CallConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外呼配置服务实现：参数存 config JSON，进程内缓存，保存后按租户失效。
 */
@Service
@RequiredArgsConstructor
public class CallConfigServiceImpl implements CallConfigService {

    private final TenantCallConfigMapper configMapper;
    private final ObjectMapper objectMapper;
    private final Map<Long, SevenMoorConfig> cache = new ConcurrentHashMap<>();

    @Override
    public SevenMoorConfig getByTenant(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        SevenMoorConfig cached = cache.get(tenantId);
        if (cached != null) {
            return cached;
        }
        TenantCallConfig entity = configMapper.selectOne(new LambdaQueryWrapper<TenantCallConfig>()
                .eq(TenantCallConfig::getTenantId, tenantId)
                .eq(TenantCallConfig::getProvider, TenantCallConfig.PROVIDER_SEVEN_MOOR));
        if (entity == null || entity.getEnabled() == null || entity.getEnabled() != 1) {
            return null;
        }
        SevenMoorConfig config = parse(entity.getConfig());
        if (config != null) {
            cache.put(tenantId, config);
        }
        return config;
    }

    @Override
    public SevenMoorConfig current() {
        return getByTenant(TenantContext.getTenantId());
    }

    @Override
    @Transactional
    public void save(SevenMoorConfig config) {
        Long tenantId = TenantContext.getTenantId();
        String json;
        try {
            json = objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException ex) {
            throw new BizException("外呼配置序列化失败");
        }
        TenantCallConfig existing = configMapper.selectOne(new LambdaQueryWrapper<TenantCallConfig>()
                .eq(TenantCallConfig::getTenantId, tenantId)
                .eq(TenantCallConfig::getProvider, TenantCallConfig.PROVIDER_SEVEN_MOOR));
        if (existing == null) {
            TenantCallConfig entity = new TenantCallConfig();
            entity.setTenantId(tenantId);
            entity.setProvider(TenantCallConfig.PROVIDER_SEVEN_MOOR);
            entity.setConfig(json);
            entity.setEnabled(1);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            configMapper.insert(entity);
        } else {
            existing.setConfig(json);
            existing.setEnabled(1);
            existing.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(existing);
        }
        cache.remove(tenantId);
    }

    private SevenMoorConfig parse(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SevenMoorConfig.class);
        } catch (JsonProcessingException ex) {
            throw new BizException("外呼配置格式错误");
        }
    }
}
