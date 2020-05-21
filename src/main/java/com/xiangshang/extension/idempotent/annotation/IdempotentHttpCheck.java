package com.xiangshang.extension.idempotent.annotation;


import java.lang.annotation.*;


/**
 @description 接口层幂等性检查注解,基于流水号做校验,不依赖GlobalResponse对象
              不支持重试，如果出现错误，只能够换流水号进行重试
 @author tanyuanpeng
 @date 2018/12/19
 **/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface IdempotentHttpCheck {

}
