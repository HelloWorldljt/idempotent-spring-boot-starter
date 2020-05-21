package com.xiangshang.extension.idempotent.domain.em;

/**
 * 幂等性状态
 * @author  tanyuanpeng
 */
public enum IdempotentStatus {
    /**
     * 初始状态
     */
    WAITING("处理中状态"),
    /**
     * 表示请求处理成功
     */
    SUCCESS("成功状态"),
    /**
     * 未知状态
     */
    UNKNOWN("未知状态"),
    /**
     *　可重试状态
     */
    RETRYING("可重试状态");
    /**
     * 返回描述
     */
    private String desc;

    IdempotentStatus(String desc){
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }


}