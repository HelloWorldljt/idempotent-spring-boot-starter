package com.xiangshang.extension.idempotent.domain.pojo.entity;

import com.xiangshang.extension.idempotent.domain.em.CheckStatus;

/**
 * 幂等性 check方法检查结果
 * @author  tanyuanpeng
 */
public class IdempotentCheckResult {

    private CheckStatus status;

    private Object result;

    private IdempotentCheckResult() {}


    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public CheckStatus getStatus() {
        return status;
    }

    private IdempotentCheckResult(CheckStatus status, Object result) {
        this.status = status;
        this.result = result;
    }

    /**
     * 返回成功状态，幂等插件会拦截请求，直接返回结果
     * @param result
     * @return
     */
    public static IdempotentCheckResult success(Object result){
        return new IdempotentCheckResult(CheckStatus.SUCCESS,result);
    }

    /**
     * 返回Unknown状态，幂等插件会拦截当前请求，等下次重试
     * @return
     */
    public static IdempotentCheckResult unknown(){
        return new IdempotentCheckResult(CheckStatus.UNKNOWN,null);
    }

    /**
     * 返回需要重试状态，幂等插件不会拦截，会执行业务代码
     * @return
     */
    public static IdempotentCheckResult retry(){
        return new IdempotentCheckResult(CheckStatus.RETRYING,null);
    }



}
