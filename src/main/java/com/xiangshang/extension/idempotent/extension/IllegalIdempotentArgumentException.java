package com.xiangshang.extension.idempotent.extension;

/**
 * 非法幂等参数异常 需要加入 ExceptionHandlerList 列表中
 * @author tanyuanpeng
 */
public class IllegalIdempotentArgumentException extends RuntimeException {

    public IllegalIdempotentArgumentException() {
    }

    public IllegalIdempotentArgumentException(String message) {
        super(message);
    }

    public IllegalIdempotentArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalIdempotentArgumentException(Throwable cause) {
        super(cause);
    }

    public IllegalIdempotentArgumentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
