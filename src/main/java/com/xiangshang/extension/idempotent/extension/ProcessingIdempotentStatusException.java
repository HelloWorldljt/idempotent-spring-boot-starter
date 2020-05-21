package com.xiangshang.extension.idempotent.extension;

/**
 * @author  tanyuanpeng 需要加入 ExceptionHandlerList 列表中
 * 幂等性拦截抛出的运行中状态异常
 */
public class ProcessingIdempotentStatusException extends RuntimeException {

    public ProcessingIdempotentStatusException() {
    }

    public ProcessingIdempotentStatusException(String message) {
        super(message);
    }

    public ProcessingIdempotentStatusException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessingIdempotentStatusException(Throwable cause) {
        super(cause);
    }

    public ProcessingIdempotentStatusException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
