package com.xiangshang.extension.idempotent.domain.pojo.entity;

/**
 * 幂等性探测结果
 * @author  tanyuanpeng
 */
public class IdempotentResult {

    /**
     * 是否被打断:false继续执行，true 被拦截
     */
    private boolean interrupt;

    private Object result;

    public IdempotentResult(boolean interrupt, Object result) {
        this.interrupt = interrupt;
        this.result = result;
    }

    public boolean isInterrupt() {
        return interrupt;
    }

    public void setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
