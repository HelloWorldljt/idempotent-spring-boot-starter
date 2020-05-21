package com.xiangshang.extension.idempotent.annotation;

import java.lang.annotation.*;

/**
@description 接口层幂等性检查注解,基于流水号做校验,不依赖GlobalResponse对象 0.0.3会弃用
@author tanyuanpeng
@date 2018/12/19
**/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Deprecated
public @interface IdempotentCheck {

    /**
     * 默认idempotentBody参数位置，以0开始
     * @return
     */
    int bodyIndex() default 0;

    /**
     * 指定哪些异常可以重试，如果发生异常，幂等记录将会标记为处理中，等待调用方重试
     * @return
     */
    Class<? extends Throwable>[] retryFor() default {};

}