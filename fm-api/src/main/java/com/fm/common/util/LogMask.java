package com.fm.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志脱敏工具：手机号中间 4 位掩码、URL 与自由文本中的敏感查询参数替换。
 * 只作用于日志输出，不改动配置、密钥或请求本身。
 */
public final class LogMask {

    private static final String MASK = "***";
    /** 11 位手机号：前 3 位 + 中间 4 位 + 后 4 位；前后加数字边界，避免误匹配长串。 */
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d)(\\d{4})(\\d{4})(?!\\d)");
    /** 敏感查询参数：key/secret/token/access_token 等，匹配到参数值整体替换。 */
    private static final Pattern SENSITIVE_PARAM = Pattern.compile(
            "([?&](?:access_token|appsecret|key|secret|token|sig|password|pwd|mobile|phone)=)[^&#\\s]*",
            Pattern.CASE_INSENSITIVE);

    private LogMask() {
    }

    /**
     * 单个手机号掩码，如 13812345678 → 138****5678；非标准手机号统一返回 ***。
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        Matcher matcher = PHONE.matcher(phone);
        return matcher.matches() ? matcher.group(1) + "****" + matcher.group(3) : MASK;
    }

    /**
     * 脱敏 URL 中 key/secret/token/access_token 等查询参数的值。
     */
    public static String maskUrl(String url) {
        if (url == null) {
            return null;
        }
        return SENSITIVE_PARAM.matcher(url).replaceAll("$1" + MASK);
    }

    /**
     * 自由文本综合脱敏：先替换敏感查询参数，再掩码文本中出现的手机号。
     * 适用于异常消息等可能同时含 URL 与手机号的内容。
     */
    public static String mask(String text) {
        if (text == null) {
            return "";
        }
        String masked = SENSITIVE_PARAM.matcher(text).replaceAll("$1" + MASK);
        return PHONE.matcher(masked).replaceAll("$1****$3");
    }

    /**
     * 异常脱敏：返回「异常类型: 脱敏后消息」。
     * HTTP 客户端异常的栈中通常含完整请求 URL（带密钥/令牌），故不直接输出原始 Throwable。
     */
    public static String mask(Throwable ex) {
        if (ex == null) {
            return "";
        }
        return ex.getClass().getSimpleName() + ": " + mask(ex.getMessage());
    }
}
