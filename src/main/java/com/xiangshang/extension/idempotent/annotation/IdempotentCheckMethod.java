package com.xiangshang.extension.idempotent.annotation;


import java.lang.annotation.*;


/**
 * 业务层幂等性检查方法 暂时只有标记作用
 * @author  tanyuanpeng
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface IdempotentCheckMethod {
    /**
     * 指向检查重试方法
     * @return
     */
    String forMethod() default "";
}
