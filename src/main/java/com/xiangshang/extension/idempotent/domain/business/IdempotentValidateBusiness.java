package com.xiangshang.extension.idempotent.domain.business;

import com.alibaba.fastjson.JSON;
import com.xiangshang.extension.idempotent.annotation.IdempotentBizCheck;
import com.xiangshang.extension.idempotent.domain.em.CheckStatus;
import com.xiangshang.extension.idempotent.domain.em.IdempotentStatus;
import com.xiangshang.extension.idempotent.domain.pojo.entity.IdempotentCheckResult;
import com.xiangshang.extension.idempotent.domain.pojo.entity.IdempotentResult;
import com.xiangshang.extension.idempotent.domain.pojo.model.Idempotent;
import com.xiangshang.extension.idempotent.domain.service.IdempotentService;
import com.xiangshang.extension.idempotent.extension.BizUnKnownStatusExceptioin;
import com.xiangshang.extension.idempotent.extension.IllegalIdempotentArgumentException;
import com.xiangshang.extension.idempotent.extension.ProcessingIdempotentStatusException;
import com.xiangshang.extension.idempotent.extension.UnsupportedIdempotentStatusException;
import com.xiangshang360.middleware.sdk.util.CalendarUtil;
import com.xs.micro.common.middleware.entity.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
@description 幂等性业务处理
@author tanyuanpeng
@date 2018/12/12
**/
@Service
public class IdempotentValidateBusiness {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotentValidateBusiness.class);

    @Autowired
    private IdempotentService idempotentService;

    public IdempotentValidateBusiness() {
    }

    /**
     * 探测是否可以执行
     * @param pjp
     * @param apiClass
     * @param apiArgs
     * @param method
     * @param serialNumber
     * @return
     */
    public IdempotentResult probeIdempotent(String appName, String serialNumber, ProceedingJoinPoint pjp, String apiClass, Object[] apiArgs, Method method, Boolean isBizCheck) throws Throwable {

        Idempotent idempotent = queryBySerialNumber(serialNumber);
        if (idempotent == null ){
            //并发时，此方法会抛异常
            try{
                buildIdempotent(appName,serialNumber,pjp.getTarget().getClass(),apiClass,method,apiArgs);
                return new IdempotentResult(false,null);
            }catch (DuplicateKeyException e){
                LOG.error("insert idempotent record error.maybe repeat insert",e);
                throw  new ProcessingIdempotentStatusException("the server is processing..");
            }
        }
        //如果存在
        if (idempotent.getStatus() == IdempotentStatus.RETRYING){
            update2Waiting(idempotent,IdempotentStatus.RETRYING);
            return new IdempotentResult(false,null);
        }else if(idempotent.getStatus() == IdempotentStatus.UNKNOWN){
            if (isBizCheck){
                //如果是 UNKNOWN 状态，需要调用check方法验证是否需要执行
                return bizCheckHandle(serialNumber, pjp, apiArgs, method, idempotent);
            }
            return new IdempotentResult(true,new UnsupportedIdempotentStatusException("unsupported idempotent status."));
        }else if(idempotent.getStatus() == IdempotentStatus.WAITING){
            LOG.info("the server is processing..");
            throw  new ProcessingIdempotentStatusException("the server is processing..");
        }else if(idempotent.getStatus() == IdempotentStatus.SUCCESS){
            //返回值重放
            MethodInvocation methodInvocation = idempotent.getMethodInvocation();
            Object result = methodInvocation.getReturnValue();
            if (methodInvocation.getReturnValue() instanceof String){
                try{
                    result = JSON.parseObject((String) methodInvocation.getReturnValue(), methodInvocation.getReturnType());
                }catch (Exception e){
                    //ignore it
                    LOG.info("parse json error. maybe it is a simple string. value:{},type:{}",methodInvocation.getReturnValue(),methodInvocation.getReturnType());
                }
            }
            return new IdempotentResult(true,result);
        }else{
            return new IdempotentResult(true,new UnsupportedIdempotentStatusException("unsupported idempotent status."));
        }

    }

    /**
     * 是否是成功处理过该请求
     * 返回请求的状态；
     * @param appName
     * @param serialNumber
     * @param method
     * @return
     */
    public void buildIdempotent(String appName,String serialNumber,Class<?> target,String apiClass, Method method,Object[] args) {
        Idempotent idempotent = new Idempotent();
        idempotent.setSerialNumber(serialNumber);
        idempotent.setStatus(IdempotentStatus.WAITING);
        idempotent.setApiClass(apiClass);
        idempotent.setApiMethod(method.getName());
        idempotent.setAppName(appName);
        MethodInvocation apiInvocation = new MethodInvocation();
        idempotent.setMethodInvocation(apiInvocation);
        idempotent.setCreateTime(CalendarUtil.getCurrentDate());
        idempotent.setUpdateTime(CalendarUtil.getCurrentDate());
        idempotent.setVersion(0);
        this.idempotentService.addRecord(idempotent);
    }

    /**
     *
     * 业务方法检查
     * @param serialNumber
     * @param pjp
     * @param apiArgs
     * @param method
     * @param idempotent
     * @return
     */
    private IdempotentResult bizCheckHandle(String serialNumber, ProceedingJoinPoint pjp, Object[] apiArgs, Method method, Idempotent idempotent) {
        Class[] parameterTypes = method.getParameterTypes();
        String checkMethodName = getCheckMethodName(method);
        //在当前类中调用check方法进行check
        IdempotentCheckResult checkResult = null;
        try{
            checkResult = (IdempotentCheckResult) MethodUtils.invokeMethod(pjp.getTarget(), checkMethodName, apiArgs, parameterTypes);
        }catch (Throwable e){
            LOG.error("call check method [{}] error",checkMethodName,e);
            throw new IllegalIdempotentArgumentException("call check method error");
        }
        if (checkResult.getStatus() == CheckStatus.SUCCESS){
            //已经执行成功，修改幂等性表结果
            updateFromOldStatus(serialNumber,method.getReturnType(),checkResult.getResult(), IdempotentStatus.SUCCESS,IdempotentStatus.UNKNOWN);
            return new IdempotentResult(true,checkResult.getResult());
        }else if(checkResult.getStatus() == CheckStatus.UNKNOWN){
            LOG.info("call check method return unknown status ,bizKey:{}",serialNumber);
            throw new BizUnKnownStatusExceptioin("call check method return unknown status");
        }else if(checkResult.getStatus() == CheckStatus.RETRYING){
            //继续执行，同时修改为处理中状态
            update2Waiting(idempotent,IdempotentStatus.UNKNOWN);
            return new IdempotentResult(false,null);
        }else {
            LOG.error("unsupported check result status ,bizKey:{},status:{}",serialNumber,checkResult.getStatus());
            throw new IllegalIdempotentArgumentException("unsupported check result status");
        }
    }

    private String getCheckMethodName(Method method) {
        IdempotentBizCheck idempotentBizCheck = method.getAnnotation(IdempotentBizCheck.class);
        String checkMethodName = idempotentBizCheck.checkMethod();
        Assert.isTrue(StringUtils.isNotBlank(checkMethodName),String.format("cannot find check method ,but the status is UNKNOWN"));
        return checkMethodName;
    }

    private String getMaxLengthRemark(String message) {
        if (StringUtils.isBlank(message)){
            return "";
        }
        return StringUtils.left(message, 100);
    }

    public Idempotent queryBySerialNumber(String serialNumber) {
        return idempotentService.queryBySerialNumber(serialNumber);
    }

    public void update2Waiting(Idempotent idempotent,IdempotentStatus oldStatus) {
        this.idempotentService.updateStatus2Waiting(idempotent,oldStatus);
    }

    public void updateFromWaiting(String serialNumber, Class returnType,Object result,IdempotentStatus status){
        this.updateFromOldStatus(serialNumber,returnType,result,status,IdempotentStatus.WAITING);
    }

    private void updateFromOldStatus(String serialNumber, Class<?> returnType, Object result, IdempotentStatus newStatus, IdempotentStatus oldStatus) {
        Idempotent idempotent = this.idempotentService.queryBySerialNumber(serialNumber);
        if (idempotent == null ){
            throw new IllegalIdempotentArgumentException(String.format("cannot find idempotent without transaction. serialNumber:[%s]",serialNumber));
        }
        if (newStatus == idempotent.getStatus()){
            LOG.info("idempotent is [{}] ,no need modify.",newStatus);
            return;
        }
        MethodInvocation methodInvocation = idempotent.getMethodInvocation();
        methodInvocation.setReturnType(returnType);
        //修改为存储JSON
        //不在完整保存堆栈信息，只保存统一的简单的异常信息，因为完整堆栈信息内容较多，序列化不合适
        String remark = "SUCCESS";
        String jsonResult = JSON.toJSONString(null);
        if (result instanceof Throwable){
            remark = getMaxLengthRemark(((Throwable) result).getMessage());
        }else{
            jsonResult = JSON.toJSONString(result);
        }
        methodInvocation.setReturnValue(jsonResult);
        this.idempotentService.updateStatus(serialNumber, newStatus, oldStatus,methodInvocation,remark,idempotent.getVersion());
    }

}