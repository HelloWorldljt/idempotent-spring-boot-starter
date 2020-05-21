package com.xiangshang.extension.idempotent.domain.em;

/**
 * 检查结果状态
 * @author  tanyuanpeng
 */
public enum CheckStatus {
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

    CheckStatus(String desc){
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }


}