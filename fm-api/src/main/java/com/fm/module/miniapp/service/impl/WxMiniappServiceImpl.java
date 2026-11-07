package com.fm.module.miniapp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fm.common.config.SchedulerLock;
import com.fm.common.exception.BizException;
import com.fm.common.util.LogMask;
import com.fm.module.miniapp.MpConstants;
import com.fm.module.miniapp.dto.PhoneLoginDTO;
import com.fm.module.miniapp.service.WxMiniappService;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.service.AuthService;
import com.fm.module.system.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 微信小程序登录实现：未配置 appid/secret 时拒绝，不影响服务启动。
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class WxMiniappServiceImpl implements WxMiniappService {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Set<String> MP_ROLE_KEYS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(SysRole.TENANT_ADMIN, SysRole.TENANT_FIELD, SysRole.MERCHANT)));

    private static final String TOKEN_CACHE_KEY = "fm:wx:miniapp:login:access_token";
    private static final String TOKEN_LOCK_KEY = "fm:wx:miniapp:login:access_token:lock";
    private static final long LOCK_TTL_SECONDS = 10L;
    private static final long TOKEN_TTL_FLOOR_SECONDS = 60L;
    private static final long TOKEN_REFRESH_AHEAD_SECONDS = 300L;
    private static final int LOCK_RETRY_TIMES = 10;
    private static final long LOCK_RETRY_MILLIS = 100L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SysUserMapper userMapper;
    private final AuthService authService;
    private final StringRedisTemplate redisTemplate;
    private final SchedulerLock schedulerLock;

    /**
     * 兼容既有单元测试的构造入口（测试只覆盖手机直登，不触发令牌逻辑）。
     * 生产环境由 Spring 通过 @RequiredArgsConstructor 注入单例 RestTemplate 与 Redis 依赖。
     */
    public WxMiniappServiceImpl(RestTemplateBuilder builder, ObjectMapper objectMapper,
                                SysUserMapper userMapper, AuthService authService) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
        this.authService = authService;
        this.redisTemplate = null;
        this.schedulerLock = null;
    }

    @Value("${fm.wx.miniapp.app-id:}")
    private String appId;

    @Value("${fm.wx.miniapp.app-secret:}")
    private String appSecret;

    @Value("${fm.wx.miniapp.mock-login-enabled:false}")
    private boolean mockLoginEnabled;

    @Override
    public LoginVO login(String code, String ip) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new BizException(503, "未配置小程序登录凭据");
        }
        if (!StringUtils.hasText(code)) {
            throw new BizException("缺少微信登录 code");
        }
        String openid = exchangeOpenid(code);
        SysUser user = userMapper.selectByWxAppOpenid(openid);
        if (user == null) {
            throw new BizException(401, "微信未绑定账号，请联系管理员");
        }
        checkMiniappUser(user);
        return authService.loginByUserId(user.getId(), ip, "app", MP_ROLE_KEYS);
    }

    @Override
    public LoginVO phoneLogin(PhoneLoginDTO request, String ip) {
        String phone = resolvePhone(request);
        List<SysUser> users = userMapper.selectByPhone(phone);
        if (users.isEmpty()) {
            throw new BizException(401, "该手机号未开通账号，请联系管理员");
        }
        if (users.size() > 1) {
            throw new BizException(401, "该手机号对应多个账号，请联系管理员处理");
        }
        SysUser user = users.get(0);
        checkMiniappUser(user);
        return authService.loginByUserId(user.getId(), ip, "app", MP_ROLE_KEYS);
    }

    private String resolvePhone(PhoneLoginDTO request) {
        if (request == null) {
            throw new BizException(400, "缺少登录参数");
        }
        if (StringUtils.hasText(request.getPhone())) {
            if (!mockLoginEnabled) {
                throw new BizException(403, "开发手机号登录未开启");
            }
            if (!PHONE_PATTERN.matcher(request.getPhone()).matches()) {
                throw new BizException(400, "手机号格式不正确");
            }
            return request.getPhone();
        }
        if (StringUtils.hasText(request.getCode())) {
            return requestPhoneByCode(request.getCode());
        }
        throw new BizException(400, "缺少手机号授权 code");
    }

    private void checkMiniappUser(SysUser user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已禁用");
        }
        if (!MpConstants.isMiniappUserType(user.getUserType())) {
            throw new BizException(403, "仅租户人员与商户账号可使用小程序");
        }
    }

    private String requestPhoneByCode(String code) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new BizException(503, "未配置小程序登录凭据");
        }
        String token = accessToken();
        String url = UriComponentsBuilder.fromHttpUrl(PHONE_URL)
                .queryParam("access_token", token)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body;
        try {
            body = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode().put("code", code));
        } catch (Exception ex) {
            throw new BizException("手机号授权参数不合法");
        }
        try {
            String response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            JsonNode node = objectMapper.readTree(StringUtils.hasText(response) ? response : "{}");
            JsonNode phoneInfo = node.path("phone_info");
            String phone = phoneInfo.path("purePhoneNumber").asText("");
            if (!StringUtils.hasText(phone)) {
                phone = phoneInfo.path("phoneNumber").asText("");
            }
            if (!StringUtils.hasText(phone)) {
                log.warn("wx get phone failed, errcode={}, errmsg={}",
                        node.path("errcode").asInt(-1), node.path("errmsg").asText(""));
                throw new BizException(401, "获取手机号失败，请重试");
            }
            return phone;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("wx get phone request failed, error={}", LogMask.mask(ex));
            throw new BizException("获取手机号失败");
        }
    }

    /**
     * 令牌改为 Redis 共享缓存 + Redis 分布式锁，替代原先仅单 JVM 有效的 synchronized。
     */
    private String accessToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        Optional<String> lock = schedulerLock.tryLock(TOKEN_LOCK_KEY, LOCK_TTL_SECONDS);
        if (!lock.isPresent()) {
            return waitForToken();
        }
        try {
            String doubleChecked = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
            if (StringUtils.hasText(doubleChecked)) {
                return doubleChecked;
            }
            return refreshToken();
        } finally {
            schedulerLock.release(TOKEN_LOCK_KEY, lock.get());
        }
    }

    private String waitForToken() {
        for (int attempt = 0; attempt < LOCK_RETRY_TIMES; attempt++) {
            try {
                Thread.sleep(LOCK_RETRY_MILLIS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
            String cached = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
            if (StringUtils.hasText(cached)) {
                return cached;
            }
        }
        throw new BizException(503, "微信服务暂不可用");
    }

    private String refreshToken() {
        String url = UriComponentsBuilder.fromHttpUrl(ACCESS_TOKEN_URL)
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .toUriString();
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(StringUtils.hasText(response) ? response : "{}");
            String token = node.path("access_token").asText("");
            if (!StringUtils.hasText(token)) {
                log.warn("wx access token failed, errcode={}, errmsg={}",
                        node.path("errcode").asInt(-1), node.path("errmsg").asText(""));
                throw new BizException(503, "微信服务暂不可用");
            }
            long expiresIn = node.path("expires_in").asLong(7200);
            long ttl = Math.max(TOKEN_TTL_FLOOR_SECONDS, expiresIn - TOKEN_REFRESH_AHEAD_SECONDS);
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, ttl, TimeUnit.SECONDS);
            return token;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("wx access token request failed, error={}", LogMask.mask(ex));
            throw new BizException(503, "微信服务暂不可用");
        }
    }

    private String exchangeOpenid(String code) {
        String url = UriComponentsBuilder.fromHttpUrl(CODE2SESSION_URL)
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(StringUtils.hasText(response) ? response : "{}");
            String openid = node.path("openid").asText("");
            if (!StringUtils.hasText(openid)) {
                log.warn("wx code2session failed, errcode={}, errmsg={}",
                        node.path("errcode").asInt(-1), node.path("errmsg").asText(""));
                throw new BizException(401, "微信登录失败，请重试");
            }
            return openid;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("wx code2session request failed, error={}", LogMask.mask(ex));
            throw new BizException("微信登录失败");
        }
    }
}
