package com.xiangshang.extension.idempotent.aspect;

import com.xiangshang.extension.idempotent.annotation.IdempotentBizCheck;
import com.xiangshang.extension.idempotent.extension.IllegalIdempotentArgumentException;
import com.xs.micro.common.idempotent.IdempotentMethodRegister;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

/**
 * 自定义bean，验证处理
 * @author tanyuanpeng
 * @Date 2019/5/13 14:06
 **/
@Service
public class IdempotentCheckBeanPostProcessor implements BeanPostProcessor {

    private final static Logger LOG = LoggerFactory.getLogger(IdempotentCheckBeanPostProcessor.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String s) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Method[] methods = targetClass.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            IdempotentBizCheck annotation = method.getAnnotation(IdempotentBizCheck.class);
            if (annotation == null ){
                continue;
            }
            //check register
            if(!IdempotentMethodRegister.exist(targetClass.getName(),method.getName())){
                throw new IllegalIdempotentArgumentException(String.format("can not find method name [%s]:[%s] from idempotent register.",targetClass.getName(),method.getName()));
            }
            //validate check method
            validateCheckMethod(targetClass,method);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String s) throws BeansException {
        return bean;
    }


    private void validateCheckMethod(Class<?> targetClass,Method method) {
        IdempotentBizCheck idempotentCheck = method.getAnnotation(IdempotentBizCheck.class);
        String checkMethodName = idempotentCheck.checkMethod();
        if (StringUtils.isBlank(checkMethodName)){
            return;
        }
        Method checkMethod = MethodUtils.getMatchingAccessibleMethod(targetClass, checkMethodName, method.getParameterTypes());
        if (checkMethod == null ){
            LOG.error("can not find check method from class:[{}],checkMethodName:[{}],params:[{}]",targetClass.getName(),checkMethodName, ArrayUtils.toString(method.getParameterTypes()));
            throw new IllegalIdempotentArgumentException("can not find check method");
        }
    }

}
