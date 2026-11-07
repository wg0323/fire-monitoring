package com.fm.module.notification.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.common.util.LogMask;
import com.fm.module.alarm.entity.CallRecord;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.alarm.mapper.CallRecordMapper;
import com.fm.module.alarm.mapper.FireAlarmMapper;
import com.fm.module.alarm.service.AlarmDomainService;
import com.fm.module.notification.config.CallProperties;
import com.fm.module.notification.converter.CallConverter;
import com.fm.module.notification.dto.CallRequestDTO;
import com.fm.module.notification.entity.SevenMoorConfig;
import com.fm.module.notification.service.CallConfigService;
import com.fm.module.notification.service.CallService;
import com.fm.module.notification.vo.CallResultVO;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 外呼服务实现：容联七陌单实现，配置来自 sys_tenant_call_config。
 * 未配置或禁用时直接返回「未开通」，不影响应用启动与报警落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CallServiceImpl implements CallService {

    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    };
    private static final String HTTPS_SCHEME = "https";
    private static final String SEVEN_MOOR_DOMAIN = "7moor.com";
    private static final String SEVEN_MOOR_SUFFIX = "." + SEVEN_MOOR_DOMAIN;
    private static final int HTTPS_PORT = 443;
    private static final String METADATA_HOST = "169.254.169.254";
    private static final Pattern IPV4_LITERAL = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");
    private static final Pattern PRIVATE_NETWORK = Pattern.compile(
            "^(?:10\\.|192\\.168\\.|172\\.(?:1[6-9]|2\\d|3[01])\\.)");

    private final FireAlarmMapper alarmMapper;
    private final CallRecordMapper callRecordMapper;
    private final CallConfigService callConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CallProperties callProperties;
    private final DataScopeService dataScopeService;

    /**
     * 兼容既有单元测试的构造入口；生产环境由 Spring 通过 @RequiredArgsConstructor 注入单例 RestTemplate。
     */
    public CallServiceImpl(FireAlarmMapper alarmMapper, CallRecordMapper callRecordMapper,
                           CallConfigService callConfigService, RestTemplateBuilder builder,
                           ObjectMapper objectMapper, CallProperties callProperties,
                           DataScopeService dataScopeService) {
        this.alarmMapper = alarmMapper;
        this.callRecordMapper = callRecordMapper;
        this.callConfigService = callConfigService;
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
        this.callProperties = callProperties;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public CallResultVO dialOut(CallRequestDTO request) {
        FireAlarm alarm = alarmMapper.selectById(request.getAlarmId());
        if (alarm == null) {
            throw new BizException("报警不存在");
        }
        if (AlarmDomainService.isFault(alarm)) {
            throw new BizException("故障报警只能转隐患，无需电话告警");
        }
        requireDialable(alarm);
        if (!StringUtils.hasText(request.getPhone())) {
            throw new BizException("被叫号码不能为空");
        }
        if (callProperties.isMock()) {
            return mockDialOut(alarm, request);
        }
        SevenMoorConfig config = callConfigService.getByTenant(alarm.getTenantId());
        if (config == null) {
            return CallConverter.result(false, "该运营中心未开通外呼");
        }

        String actionId = UUID.randomUUID().toString();
        saveRecord(alarm, request.getPhone(), actionId);
        try {
            JsonNode response = post(config, request.getPhone(), actionId);
            boolean succeed = response.path("Succeed").asBoolean(false);
            String message = response.path("Message").asText("");
            if (!succeed) {
                markRecordFailed(actionId);
            }
            CallResultVO vo = CallConverter.result(succeed, message);
            vo.setActionId(actionId);
            return vo;
        } catch (RestClientException ex) {
            log.error("dial out failed, alarmId={}, tenantId={}, error={}",
                    alarm.getId(), alarm.getTenantId(), LogMask.mask(ex));
            markRecordFailed(actionId);
            CallResultVO vo = CallConverter.result(false, "外呼请求失败");
            vo.setActionId(actionId);
            return vo;
        }
    }

    @Override
    public void handleCallback(String actionId, String calledNo, String state,
                               String begin, String end, String monitorFilename) {
        if (!StringUtils.hasText(actionId)) {
            return;
        }
        // 回调无登录态，显式忽略租户过滤后再按 actionId 定位记录
        TenantContext.set(0L, null, null, "PLATFORM", "ALL", true);
        try {
            CallRecord record = callRecordMapper.selectOne(new LambdaQueryWrapper<CallRecord>()
                    .eq(CallRecord::getActionId, actionId));
            if (record == null) {
                log.warn("call callback matched no record, actionId={}", actionId);
                return;
            }
            CallRecord update = new CallRecord();
            update.setId(record.getId());
            if (StringUtils.hasText(calledNo)) {
                update.setCalledNo(calledNo);
            }
            update.setCallState(state);
            update.setCallBegin(parseTime(begin));
            update.setCallEnd(parseTime(end));
            update.setMonitorFileRemote(monitorFilename);
            update.setSynced(0);
            update.setUpdateTime(LocalDateTime.now());
            callRecordMapper.updateById(update);
        } finally {
            TenantContext.clear();
        }
    }

    private CallResultVO mockDialOut(FireAlarm alarm, CallRequestDTO request) {
        String actionId = UUID.randomUUID().toString();
        saveRecord(alarm, request.getPhone(), actionId);
        log.info("mock dial out, alarmId={}, phone={}, actionId={}",
                alarm.getId(), LogMask.maskPhone(request.getPhone()), actionId);
        CallResultVO vo = CallConverter.result(true, "MOCK 外呼已发起");
        vo.setActionId(actionId);
        return vo;
    }

    private JsonNode post(SevenMoorConfig config, String phone, String actionId) {
        String host = validateHost(config.getHost());
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String raw = config.getAccountId() + config.getApiSecret() + time;
        String sig = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8)).toUpperCase();
        String auth = Base64.getEncoder()
                .encodeToString((config.getAccountId() + ":" + time).getBytes(StandardCharsets.UTF_8));
        String url = host + "/v20180426/call/dialout/" + config.getAccountId() + "?sig=" + sig;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("Authorization", auth);

        Map<String, Object> body = new HashMap<>(4);
        body.put("FromExten", StringUtils.hasText(config.getFromExten()) ? config.getFromExten() : "8000");
        body.put("Exten", phone);
        body.put("ExtenType", "gateway");
        body.put("ActionID", actionId);

        String response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        try {
            return objectMapper.readTree(StringUtils.hasText(response) ? response : "{}");
        } catch (Exception ex) {
            throw new RestClientException("invalid dialout response", ex);
        }
    }

    /**
     * 仅允许 https + 七陌官方域名；拒绝 IP、localhost、内网段、元数据地址、userinfo 与自定义 scheme。
     * 校验失败只抛出业务异常，不打印 host 或任何凭据。
     */
    private String validateHost(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BizException("外呼接口地址未配置");
        }
        String host = value.trim();
        URI uri;
        try {
            uri = new URI(host);
        } catch (URISyntaxException ex) {
            throw new BizException("外呼接口地址不合法");
        }
        if (!HTTPS_SCHEME.equalsIgnoreCase(uri.getScheme())) {
            throw new BizException("外呼接口地址仅支持 https");
        }
        if (uri.getRawUserInfo() != null || host.contains("@")
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new BizException("外呼接口地址不合法");
        }
        String domain = uri.getHost();
        if (!StringUtils.hasText(domain)) {
            throw new BizException("外呼接口地址不合法");
        }
        String lower = domain.toLowerCase(Locale.ROOT);
        if (isForbiddenHost(lower)) {
            throw new BizException("外呼接口地址不合法");
        }
        if (!SEVEN_MOOR_DOMAIN.equals(lower) && !lower.endsWith(SEVEN_MOOR_SUFFIX)) {
            throw new BizException("外呼接口地址必须使用七陌官方域名");
        }
        int port = uri.getPort();
        if (port != -1 && port != HTTPS_PORT) {
            throw new BizException("外呼接口地址端口不合法");
        }
        String path = uri.getRawPath();
        if (StringUtils.hasText(path) && !"/".equals(path)) {
            throw new BizException("外呼接口地址不合法");
        }
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }

    private boolean isForbiddenHost(String host) {
        return "localhost".equals(host)
                || host.endsWith(".localhost")
                || host.startsWith("[")
                || METADATA_HOST.equals(host)
                || IPV4_LITERAL.matcher(host).matches()
                || PRIVATE_NETWORK.matcher(host).find()
                || host.endsWith(".internal");
    }

    /**
     * 标记呼叫记录失败：按 actionId 直接条件更新，单条 UPDATE 自带原子性，
     * 消除 selectOne -> updateById 的读改写窗口（并发失败标记不会互相覆盖）。
     * private 方法上的 @Transactional 在 Spring AOP 代理下不生效，同类自调用同样不经过代理，故移除。
     * 本类写操作都是单条 INSERT / UPDATE；dialOut 在两次写之间夹着外部 HTTP 调用，
     * 若在 public 方法上加事务会把长耗时 HTTP 请求纳入数据库事务并长期占用连接，
     * 因此不引入类级或 public 方法级事务（也不改变 dialOut 的调用时序）。
     */
    private void markRecordFailed(String actionId) {
        CallRecord update = new CallRecord();
        update.setCallState("failed");
        update.setUpdateTime(LocalDateTime.now());
        callRecordMapper.update(update, new LambdaUpdateWrapper<CallRecord>()
                .eq(CallRecord::getActionId, actionId));
    }

    private void saveRecord(FireAlarm alarm, String phone, String actionId) {
        CallRecord record = new CallRecord();
        record.setTenantId(alarm.getTenantId());
        record.setAlarmId(alarm.getId());
        record.setCalledNo(phone);
        record.setActionId(actionId);
        record.setCallState("dialing");
        record.setSynced(0);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        callRecordMapper.insert(record);
    }

    /** 外呼是写操作：平台只读账号禁止发起，外勤只能呼叫自己工作区商户的报警 */
    private void requireDialable(FireAlarm alarm) {
        if (dataScopeService.isPlatform()) {
            throw new BizException(403, "平台账号只读，不能发起外呼");
        }
        if (!SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            return;
        }
        List<Long> merchantIds = dataScopeService.workAreaMerchantIds(currentUserId());
        if (alarm.getMerchantId() == null || !merchantIds.contains(alarm.getMerchantId())) {
            throw new BizException("报警不存在");
        }
    }

    private Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        for (DateTimeFormatter formatter : TIME_FORMATS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种格式
            }
        }
        return null;
    }
}
