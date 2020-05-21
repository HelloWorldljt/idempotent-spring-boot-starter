package com.xiangshang.extension.idempotent.domain.service;

import com.xiangshang.extension.idempotent.domain.em.IdempotentStatus;
import com.xiangshang.extension.idempotent.domain.pojo.model.Idempotent;
import com.xs.micro.common.middleware.entity.MethodInvocation;

/**
@description 幂等服务类
@author tanyuanpeng
@date 2018/12/12
**/
public interface IdempotentService {

    /**
     查询幂等记录
    @param serialNumber
    @return Idempotent
    @author tanyuanpeng
    @date 2018/12/12
    **/
    Idempotent queryBySerialNumber(String serialNumber);

    /**
     增加记录
     @return
     @author tanyuanpeng
     @date 2018/12/12
     *@param idempotent
     **/
    void addRecord(Idempotent idempotent);

    /**
     @description 更新状态
     @author tanyuanpeng
     @date 2018/12/12
     **/
    void updateStatus(String serialNumber, IdempotentStatus newStatus, IdempotentStatus oldStatus, MethodInvocation methodInvocation, String remark, Integer version);

    /**
     * 更新幂等性记录为待处理状态
     * @param idempotent
     */
    void updateStatus2Waiting(Idempotent idempotent,IdempotentStatus oldStatus);
}