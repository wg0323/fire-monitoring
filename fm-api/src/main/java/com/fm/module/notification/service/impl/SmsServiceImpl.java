package com.fm.module.notification.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.util.LogMask;
import com.fm.module.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 聚合数据短信实现：GET v.juhe.cn/sms/send，error_code=0 视为成功。
 * key/tpl-id 未配置时直接返回 false，不阻断启动与报警落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fm.sms.key:}")
    private String key;

    @Value("${fm.sms.tpl-id:}")
    private String tplId;

    @Value("${fm.sms.url:http://v.juhe.cn/sms/send}")
    private String url;

    @Override
    public boolean sendAlarm(String phone, String alarmTime, String alarmType, String alarmAddress) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(tplId)) {
            log.warn("sms channel disabled: fm.sms.key or fm.sms.tpl-id not configured");
            return false;
        }
        if (!StringUtils.hasText(phone)) {
            log.warn("sms skipped: empty phone");
            return false;
        }
        String tplValue = "#alarmTime#=" + alarmTime
                + "&#alarmType#=" + alarmType
                + "&#alarmAddress#=" + alarmAddress;
        String requestUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("key", key)
                .queryParam("mobile", phone)
                .queryParam("tpl_id", tplId)
                .queryParam("tpl_value", tplValue)
                .build()
                .toUriString();
        try {
            String response = restTemplate.getForObject(requestUrl, String.class);
            JsonNode node = objectMapper.readTree(StringUtils.hasText(response) ? response : "{}");
            int errorCode = node.path("error_code").asInt(-1);
            if (errorCode != 0) {
                log.error("sms send failed, phone={}, errorCode={}, reason={}",
                        LogMask.maskPhone(phone), errorCode, node.path("reason").asText(""));
                return false;
            }
            return true;
        } catch (RestClientException ex) {
            log.error("sms request failed, phone={}, url={}, error={}",
                    LogMask.maskPhone(phone), LogMask.maskUrl(requestUrl), LogMask.mask(ex));
            return false;
        } catch (Exception ex) {
            log.error("sms response parse failed, phone={}, error={}",
                    LogMask.maskPhone(phone), LogMask.mask(ex));
            return false;
        }
    }
}
