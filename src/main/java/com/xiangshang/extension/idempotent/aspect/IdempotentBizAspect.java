package com.xiangshang.extension.idempotent.aspect;

import com.xiangshang.extension.idempotent.annotation.IdempotentBizCheck;
import com.xiangshang.extension.idempotent.domain.business.IdempotentValidateBusiness;
import com.xiangshang.extension.idempotent.domain.em.IdempotentStatus;
import com.xiangshang.extension.idempotent.domain.pojo.entity.IdempotentResult;
import com.xiangshang.extension.idempotent.extension.ExceptionHandler;
import com.xiangshang.extension.idempotent.extension.IdempotentContextHolder;
import com.xiangshang.extension.idempotent.extension.IllegalIdempotentArgumentException;
import com.xs.micro.common.idempotent.IdempotentMethodRegister;
import com.xs.micro.common.middleware.parser.BizKeyParser;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

/**
 * Order 为什么是1？@Transaction标签默认order是 LOWEST_PRECEDENCE；
 * 此时
 * 幂等性业务校验
 * @author  tanyuanpeng
 */
@Aspect
@Component
@Order(1)
public class IdempotentBizAspect{

    private static final Logger LOG = LoggerFactory.getLogger(IdempotentBizAspect.class);

    @Autowired
    private IdempotentValidateBusiness idempotentValidateBusiness;

    @Value("${spring.application.name}")
    private String appName;


    @Pointcut("execution(public * *(..)) && @annotation(com.xiangshang.extension.idempotent.annotation.IdempotentBizCheck)")
    public void idempotentPointcut() {

    }

    /**
     * 环绕拦截
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("idempotentPointcut()")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable{
        // 目标类名
        String apiClass = pjp.getTarget().getClass().getName();
        // 方法参数值列表
        Object[] apiArgs = pjp.getArgs();
        // 目标方法的签名
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        // 校验check方法是否存在，如果配置了
        check(pjp,method);
        // 流水号
        String idempotentKey = BizKeyParser.parse(method.getParameterAnnotations(), apiArgs);
        if (StringUtils.isBlank(idempotentKey)){
            throw new IllegalIdempotentArgumentException("can not find idempotentKey from parameters.");
        }
        if(TransactionSynchronizationManager.isActualTransactionActive()){
            LOG.error("Active transactions existed before idempotent method,class:{},method:{}",apiClass,method.getName());
            throw new IllegalIdempotentArgumentException("Active transactions existed before idempotent method.");
        }
        IdempotentContextHolder.setIdempotentKey(idempotentKey);
        IdempotentResult result = idempotentValidateBusiness.probeIdempotent(appName,idempotentKey,pjp, apiClass, apiArgs, method,true);
        if (result.isInterrupt()){
            //返回NULL 表示不执行拦截的方法；
            if(result.getResult() instanceof Throwable){
                throw (Throwable) result.getResult();
            }else{
                return result.getResult();
            }
        }
        return pjp.proceed();
    }


    /**
     * 方法执行没有异常，尝试获取方法返回结果
     * @param joinPoint
     * @param result
     */
    @AfterReturning(value="idempotentPointcut()", returning="result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        try {
            // 获取到serialNumber，根据返回值判断状态
            String serialNumber = getSerialNumberFromContext();
            //没有异常则通过，同时需要保存被调方返回参数
            MethodSignature method = (MethodSignature)joinPoint.getSignature();
            //返回类型
            Class returnType = method.getReturnType();
            //返回数据
            this.idempotentValidateBusiness.updateFromWaiting(serialNumber,returnType,result, IdempotentStatus.SUCCESS);
        }catch (Throwable e){
            LOG.error("afterReturning is error.",e);
            throw new IllegalIdempotentArgumentException(e.getMessage());
        }finally {
            IdempotentContextHolder.removeIdempotentKey();
        }
    }

    /**
     * 方法抛出异常，需要根据情况决定是否处理
     * @param joinPoint
     * @param ex
     */
    @AfterThrowing(value="idempotentPointcut()", throwing="ex")
    public void afterThrowing(JoinPoint joinPoint, Exception ex) {
        try {
            if (ExceptionHandler.isIdempotentException(ex)){
                //不执行业务逻辑代码，如果异常是中间件异常
                LOG.error("catch idempotent exception.",ex);
                return;
            }
            // 获取serialNumber ，修改为失败
            String serialNumber = getSerialNumberFromContext();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            //返回类型
            Class returnType = method.getReturnType();
            //返回数据
            IdempotentBizCheck idempotentCheck = method.getAnnotation(IdempotentBizCheck.class);
            Class<? extends Throwable>[] unKnownExceptions = idempotentCheck.unKnownException();
            if (ArrayUtils.contains(unKnownExceptions, ex.getClass())) {
                this.idempotentValidateBusiness.updateFromWaiting(serialNumber,returnType,ex,IdempotentStatus.UNKNOWN);
            }else {
                //如果拦截的是业务方法，则直接修改为retrying状态
                this.idempotentValidateBusiness.updateFromWaiting(serialNumber,returnType,ex,IdempotentStatus.RETRYING);
            }
        }catch (Throwable e){
            LOG.error("afterThrowing is error.",e);
            throw new IllegalIdempotentArgumentException(e.getMessage());
        }finally {
            IdempotentContextHolder.removeIdempotentKey();
        }
    }


    private void check(ProceedingJoinPoint pjp, Method method) {
        validateCheckMethod(pjp, method);
        String targetMethodName = method.getName();
        String targetClassName = pjp.getTarget().getClass().getName();
        validateRegister(targetClassName,targetMethodName);
    }


    private void validateRegister(String targetClassName, String targetMethodName) {
        if(!IdempotentMethodRegister.exist(targetClassName,targetMethodName)){
            throw new IllegalIdempotentArgumentException("can not find register method");
        }
    }

    private void validateCheckMethod(ProceedingJoinPoint pjp, Method method) {
        IdempotentBizCheck idempotentCheck = method.getAnnotation(IdempotentBizCheck.class);
        String checkMethodName = idempotentCheck.checkMethod();
        if (StringUtils.isBlank(checkMethodName)){
            return;
        }
        Method checkMethod = MethodUtils.getMatchingAccessibleMethod(pjp.getTarget().getClass(), checkMethodName, method.getParameterTypes());
        if (checkMethod == null ){
            LOG.error("can not find check method from class:[{}],checkMethodName:[{}],params:[{}]",pjp.getTarget().getClass().getName(),checkMethodName, ArrayUtils.toString(method.getParameterTypes()));
            throw new IllegalIdempotentArgumentException("can not find check method");
        }
    }

    private String getSerialNumberFromContext() {
        String idempotentKey = IdempotentContextHolder.getIdempotentKey();
        if (StringUtils.isBlank(idempotentKey)){
            throw new IllegalIdempotentArgumentException("can not find idempotent key in the context.");
        }
        return idempotentKey;
    }

}
