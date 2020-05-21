package com.xiangshang.extension.idempotent.aspect;

import com.xiangshang.extension.idempotent.annotation.IdempotentBizCheck;
import com.xiangshang.extension.idempotent.annotation.IdempotentCheck;
import com.xiangshang.extension.idempotent.domain.business.IdempotentValidateBusiness;
import com.xiangshang.extension.idempotent.domain.em.IdempotentStatus;
import com.xiangshang.extension.idempotent.domain.pojo.entity.IdempotentResult;
import com.xiangshang.extension.idempotent.extension.*;
import com.xiangshang360.middleware.sdk.entity.IdempotentReqBody;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.reflect.Method;


/**
@description 幂等aop处理：controller层使用，以后会删掉
@author tanyuanpeng
@date 2018/12/18
**/
@Aspect
@Component
@Deprecated
public class IdempotentAspect {

	private static final Logger LOG = LoggerFactory.getLogger(IdempotentAspect.class);

	@Autowired
	private IdempotentValidateBusiness idempotentValidateBusiness;

	@Value("${spring.application.name}")
	private String appName;

	@Pointcut("execution(public * *(..)) && @annotation(com.xiangshang.extension.idempotent.annotation.IdempotentCheck)")
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
		// 流水号
		String serialNumber = getSerialNumber(apiClass,method,apiArgs);
		IdempotentContextHolder.setSerialNumber(serialNumber);
		if (method.isAnnotationPresent(IdempotentBizCheck.class) && method.isAnnotationPresent(IdempotentCheck.class)){
			throw new IllegalIdempotentArgumentException("method cannot be annotated with IdempotentBizCheck and IdempotentCheck annotations at same time.");
		}
		IdempotentResult result = idempotentValidateBusiness.probeIdempotent(appName, serialNumber, pjp, apiClass, apiArgs, method, false);
		if (result.isInterrupt()){
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
			IdempotentContextHolder.removeSerialNumber();
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
				LOG.error("catch idempotent exception.",ex);
				return;
			}
			String serialNumber = getSerialNumberFromContext();
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Method method = signature.getMethod();
			//返回类型
			Class returnType = method.getReturnType();
			//返回数据
			Class<? extends Throwable>[] retryFor =  getRetryForExceptionClass(method);
			if (ArrayUtils.contains(retryFor, ex.getClass())) {
				this.idempotentValidateBusiness.updateFromWaiting(serialNumber,returnType,ex,IdempotentStatus.RETRYING);
			}else {
				this.idempotentValidateBusiness.updateFromWaiting(serialNumber,returnType,ex,IdempotentStatus.SUCCESS);
			}
		}catch (Throwable e){
			LOG.error("afterThrowing is error.",e);
			throw new IllegalIdempotentArgumentException(e.getMessage());
		}finally{
			IdempotentContextHolder.removeSerialNumber();
		}
	}

	/**
	 * 获取retry异常数组
	 * @param method
	 * @return
	 */
	private Class<? extends Throwable>[] getRetryForExceptionClass(Method method) {
		IdempotentCheck idempotentCheck = method.getAnnotation(IdempotentCheck.class);
		return idempotentCheck.retryFor();
	}

	/**
	 * 从上下文获取流水号
	 * @return
	 */
	private String getSerialNumberFromContext() {
		String serialNumber = IdempotentContextHolder.getSerialNumber();
		Assert.isTrue(StringUtils.isNotBlank(serialNumber), "can not find serialNumber in the result");
		return serialNumber;
	}

	/**
	 * 获取参数对象
	 * @param apiClass
	 * @param method
	 * @param args
	 * @return
	 */
	private String getSerialNumber(String apiClass, Method method, Object[] args) {
		String apiMethod = method.getName();
		IdempotentCheck idempotentCheck = method.getAnnotation(IdempotentCheck.class);
		int bodyIndex = idempotentCheck.bodyIndex();
		if (args == null || args.length==0 || args.length < bodyIndex+1){
			throw new IllegalIdempotentArgumentException(String.format("The interface [%1s.%2s] can not find idempotent body.",apiClass,apiMethod));
		}
		Object reqBody = args[bodyIndex];
		if (reqBody == null) {
			throw new IllegalIdempotentArgumentException(String.format("The interface [%1s.%2s] parameter value can't be null, index [%3s].", apiClass, apiMethod, bodyIndex));
		}
		boolean isIdempotentBody = reqBody instanceof IdempotentReqBody;
		if (!isIdempotentBody){
			throw new IllegalIdempotentArgumentException(String.format("The interface [%1s.%2s] parameter value is not IdempotentReqBody object, index [%3s].", apiClass, apiMethod, bodyIndex));
		}
		IdempotentReqBody idempotentReqBody = (IdempotentReqBody) reqBody;
		String serialNumber = idempotentReqBody.getSerialNumber();
		if(StringUtils.isBlank(serialNumber)){
			throw new IllegalIdempotentArgumentException("idempotent interceptor can not find serialNumber.");
		}
		return serialNumber;
	}



}
