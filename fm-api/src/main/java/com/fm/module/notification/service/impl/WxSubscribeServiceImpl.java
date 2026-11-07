package com.fm.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.util.LogMask;
import com.fm.module.alarm.entity.AlarmLevel;
import com.fm.module.alarm.entity.FireAlarm;
import com.fm.module.danger.entity.Danger;
import com.fm.module.device.entity.Merchant;
import com.fm.module.notification.service.WxSubscribeService;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信小程序订阅消息实现。
 * 报警模板字段：thing1=位置、time2=时间、thing3=类型、thing4=商户；
 * 隐患模板字段：thing1=隐患类型、thing2=位置、time3=时间、thing4=状态说明。
 * 模板需按上述字段创建，未配置模板或凭据时自动跳过。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxSubscribeServiceImpl implements WxSubscribeService {

    // 与 WxMiniappServiceImpl 共用同一 appid 的 token 缓存 key：微信 access_token 全局唯一，
    // 两处独立刷新会互相使对方失效；只统一 key，刷新/缓存逻辑保持不变。
    private static final String TOKEN_KEY = "fm:wx:miniapp:login:access_token";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int THING_MAX_LENGTH = 20;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;

    @Value("${fm.wx.miniapp.app-id:}")
    private String appId;
    @Value("${fm.wx.miniapp.app-secret:}")
    private String appSecret;
    @Value("${fm.wx.miniapp.subscribe.alarm-template-id:}")
    private String alarmTemplateId;
    @Value("${fm.wx.miniapp.subscribe.danger-template-id:}")
    private String dangerTemplateId;
    @Value("${fm.wx.miniapp.subscribe.state:developer}")
    private String miniprogramState;

    @Override
    public String alarmTemplateId() {
        return StringUtils.hasText(alarmTemplateId) ? alarmTemplateId : "";
    }

    @Override
    public String dangerTemplateId() {
        return StringUtils.hasText(dangerTemplateId) ? dangerTemplateId : "";
    }

    @Override
    public void sendAlarm(FireAlarm alarm, Merchant merchant) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)
                || !StringUtils.hasText(alarmTemplateId)) {
            log.debug("wx subscribe skipped: template or credential not configured");
            return;
        }
        if (alarm == null || merchant == null) {
            return;
        }
        try {
            List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getTenantId, alarm.getTenantId())
                    .isNotNull(SysUser::getWxAppOpenid)
                    .ne(SysUser::getWxAppOpenid, "")
                    .eq(SysUser::getStatus, 1));
            if (users.isEmpty()) {
                return;
            }
            String address = StringUtils.hasText(alarm.getPosition())
                    ? alarm.getPosition() : merchant.getName();
            String time = (alarm.getAlarmTime() == null ? LocalDateTime.now() : alarm.getAlarmTime())
                    .format(TIME_FORMATTER);
            String type = levelText(alarm.getAlarmLevel());
            for (SysUser user : users) {
                sendOne(user.getWxAppOpenid(), alarm, address, time, type, merchant.getName());
            }
        } catch (Exception ex) {
            log.error("wx subscribe failed, alarmId={}, error={}", alarm.getId(), LogMask.mask(ex));
        }
    }

    @Override
    public void sendDanger(Danger danger, SysUser receiver, String content) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)
                || !StringUtils.hasText(dangerTemplateId)) {
            log.debug("wx danger subscribe skipped: template or credential not configured");
            return;
        }
        if (danger == null || receiver == null || !StringUtils.hasText(receiver.getWxAppOpenid())) {
            return;
        }
        try {
            String token = accessToken(false);
            JsonNode response = postDanger(token, receiver.getWxAppOpenid(), danger, content);
            int errorCode = response.path("errcode").asInt(0);
            if (errorCode == 40001 || errorCode == 42001) {
                token = accessToken(true);
                response = postDanger(token, receiver.getWxAppOpenid(), danger, content);
                errorCode = response.path("errcode").asInt(0);
            }
            if (errorCode != 0) {
                log.warn("wx danger subscribe rejected: dangerId={}, errcode={}, errmsg={}",
                        danger.getId(), errorCode, response.path("errmsg").asText());
                return;
            }
            log.info("wx danger subscribe sent: dangerId={}, userId={}", danger.getId(), receiver.getId());
        } catch (Exception ex) {
            log.error("wx danger subscribe send failed, dangerId={}, error={}",
                    danger.getId(), LogMask.mask(ex));
        }
    }

    private JsonNode postDanger(String token, String openid, Danger danger, String content) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("template_id", dangerTemplateId);
        body.put("page", "pages/danger/detail?id=" + danger.getId());
        body.put("miniprogram_state", miniprogramState);
        body.put("lang", "zh_CN");
        Map<String, Object> data = new LinkedHashMap<>();
        String type = StringUtils.hasText(danger.getDangerType()) ? danger.getDangerType() : "隐患";
        String position = StringUtils.hasText(danger.getPosition()) ? danger.getPosition() : "待整改";
        data.put("thing1", Collections.singletonMap("value", truncate(type)));
        data.put("thing2", Collections.singletonMap("value", truncate(position)));
        data.put("time3", Collections.singletonMap("value", LocalDateTime.now().format(TIME_FORMATTER)));
        data.put("thing4", Collections.singletonMap("value", truncate(content)));
        body.put("data", data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token;
        String response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        return objectMapper.readTree(StringUtils.hasText(response) ? response : "{}");
    }

    private void sendOne(String openid, FireAlarm alarm, String address, String time,
                         String type, String merchantName) {
        try {
            String token = accessToken(false);
            JsonNode response = postSend(token, openid, alarm, address, time, type, merchantName);
            int errorCode = response.path("errcode").asInt(0);
            if (errorCode == 40001 || errorCode == 42001) {
                token = accessToken(true);
                response = postSend(token, openid, alarm, address, time, type, merchantName);
                errorCode = response.path("errcode").asInt(0);
            }
            if (errorCode != 0) {
                log.warn("wx subscribe rejected: alarmId={}, errcode={}, errmsg={}",
                        alarm.getId(), errorCode, response.path("errmsg").asText());
                return;
            }
            log.info("wx subscribe sent: alarmId={}", alarm.getId());
        } catch (Exception ex) {
            log.error("wx subscribe send failed, alarmId={}, error={}", alarm.getId(), LogMask.mask(ex));
        }
    }

    private JsonNode postSend(String token, String openid, FireAlarm alarm, String address,
                              String time, String type, String merchantName) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("template_id", alarmTemplateId);
        body.put("page", "pages/alarm/detail?id=" + alarm.getId());
        body.put("miniprogram_state", miniprogramState);
        body.put("lang", "zh_CN");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("thing1", Collections.singletonMap("value", truncate(address)));
        data.put("time2", Collections.singletonMap("value", time));
        data.put("thing3", Collections.singletonMap("value", truncate(type)));
        data.put("thing4", Collections.singletonMap("value", truncate(merchantName)));
        body.put("data", data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token;
        String response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        return objectMapper.readTree(StringUtils.hasText(response) ? response : "{}");
    }

    private String accessToken(boolean force) throws Exception {
        if (!force) {
            String cached = redisTemplate.opsForValue().get(TOKEN_KEY);
            if (StringUtils.hasText(cached)) {
                return cached;
            }
        }
        synchronized (this) {
            if (!force) {
                String cached = redisTemplate.opsForValue().get(TOKEN_KEY);
                if (StringUtils.hasText(cached)) {
                    return cached;
                }
            }
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential"
                    + "&appid=" + appId + "&secret=" + appSecret;
            String body = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(StringUtils.hasText(body) ? body : "{}");
            String token = node.path("access_token").asText();
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("获取微信 access_token 失败: "
                        + node.path("errmsg").asText());
            }
            long expiresIn = node.path("expires_in").asLong(7200);
            redisTemplate.opsForValue().set(TOKEN_KEY, token,
                    Math.max(60, expiresIn - 300), TimeUnit.SECONDS);
            return token;
        }
    }

    private String levelText(Integer level) {
        if (level == null) {
            return "报警";
        }
        if (level == AlarmLevel.FAULT) {
            return "故障";
        }
        return "火警";
    }

    private String truncate(String value) {
        String text = value == null ? "" : value;
        return text.length() <= THING_MAX_LENGTH ? text : text.substring(0, THING_MAX_LENGTH);
    }
}
