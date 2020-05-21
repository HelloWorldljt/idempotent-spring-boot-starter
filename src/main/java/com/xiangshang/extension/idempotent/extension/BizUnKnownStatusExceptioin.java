package com.xiangshang.extension.idempotent.extension;

/**
 *
 * 业务状态返回未知异常状态，需要进行等待,需要加入 ExceptionHandlerList 列表中
 * @author tanyuanpeng
 * @Date 2019/8/12 19:23
 **/
public class BizUnKnownStatusExceptioin extends RuntimeException {

    public BizUnKnownStatusExceptioin() {
    }

    public BizUnKnownStatusExceptioin(String message) {
        super(message);
    }

    public BizUnKnownStatusExceptioin(String message, Throwable cause) {
        super(message, cause);
    }

    public BizUnKnownStatusExceptioin(Throwable cause) {
        super(cause);
    }

    public BizUnKnownStatusExceptioin(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
