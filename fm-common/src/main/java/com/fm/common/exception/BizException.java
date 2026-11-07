package com.fm.common.exception;

/**
 * 业务异常：由全局异常处理器转换为统一返回。
 */
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务规则拒绝的默认错误码 */
    public static final int BUSINESS_ERROR = 1001;

    private final int code;

    public BizException(String message) {
        this(BUSINESS_ERROR, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
