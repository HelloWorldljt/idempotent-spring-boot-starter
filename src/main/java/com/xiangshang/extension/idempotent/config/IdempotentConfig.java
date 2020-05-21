package com.xiangshang.extension.idempotent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
@description  幂等性配置
@author tanyuanpeng
@date 2018/12/12
**/
@ConfigurationProperties(
    prefix = "idempotent"
)
public class IdempotentConfig {

    /**
     * 是否开启幂等性校验
     */
    private boolean enable;

    public IdempotentConfig() {
    }

    public boolean isEnable() {
        return this.enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

}
