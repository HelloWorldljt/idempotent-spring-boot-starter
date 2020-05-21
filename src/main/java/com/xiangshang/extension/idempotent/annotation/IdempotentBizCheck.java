package com.xiangshang.extension.idempotent.annotation;


import java.lang.annotation.*;


/**
 * 业务层幂等性检查校验,暂时不支持嵌套幂等性校验
 * @author  tanyuanpeng
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface IdempotentBizCheck {

    /**
     * 指定哪些异常可以重试，如果发生异常，幂等记录将会标记为未知异常，等待重试
     * @return
     */
    Class<? extends Throwable>[] unKnownException() default {};

    /**
     * 需要重试的方法
     * @return
     */
    String checkMethod() default "";

}
