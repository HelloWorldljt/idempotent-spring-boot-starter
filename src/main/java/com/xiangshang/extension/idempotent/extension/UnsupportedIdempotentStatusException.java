package com.xiangshang.extension.idempotent.extension;


/**
 * 无法识别的幂等状态异常 需要加入 ExceptionHandlerList 列表中
 * @author tanyuanpeng
 */
public class UnsupportedIdempotentStatusException extends RuntimeException {

    public UnsupportedIdempotentStatusException() {
    }

    public UnsupportedIdempotentStatusException(String message) {
        super(message);
    }

    public UnsupportedIdempotentStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedIdempotentStatusException(Throwable cause) {
        super(cause);
    }

    public UnsupportedIdempotentStatusException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
