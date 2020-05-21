package com.xiangshang.extension.idempotent.domain.service.impl;

import com.xiangshang.extension.idempotent.domain.dao.IdempotentMapper;
import com.xiangshang.extension.idempotent.domain.em.IdempotentStatus;
import com.xiangshang.extension.idempotent.domain.pojo.model.Idempotent;
import com.xiangshang.extension.idempotent.domain.service.IdempotentService;
import com.xiangshang.extension.idempotent.extension.IllegalIdempotentArgumentException;
import com.xiangshang360.middleware.sdk.util.CalendarUtil;
import com.xs.micro.common.middleware.entity.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
@description 幂等服务类
@author tanyuanpeng
@date 2018/12/12
**/
@Service
public class IdempotentServiceImpl implements IdempotentService {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotentServiceImpl.class);

    @Autowired
    private IdempotentMapper idempotentMapper;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public Idempotent queryBySerialNumber(String serialNumber) {
        return this.idempotentMapper.queryBySerialNumber(serialNumber);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void addRecord(Idempotent idempotent) {
        this.idempotentMapper.insert(idempotent);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void updateStatus(String serialNumber, IdempotentStatus newStatus, IdempotentStatus oldStatus, MethodInvocation methodInvocation, String remark, Integer version) {
        int effectCount = this.idempotentMapper.updateStatusAndReturn(serialNumber, newStatus, oldStatus,methodInvocation,remark,version,CalendarUtil.getCurrentDate());
        if (effectCount != 1) {
            LOG.error("update idempotent error~!serialNumber:{},oldStatus:{},newStatus:{}", serialNumber, oldStatus, newStatus);
            throw new IllegalIdempotentArgumentException(String.format("update idempotent error.info:[%s]",serialNumber));
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void updateStatus2Waiting(Idempotent idempotent,IdempotentStatus oldStatus) {
        int effectCount = this.idempotentMapper.updateStatusByVersion(idempotent.getSerialNumber(), IdempotentStatus.WAITING, oldStatus, CalendarUtil.getCurrentDate(),idempotent.getVersion());
        if (effectCount != 1){
            LOG.error("update updateStatus2Waiting error~!serialNumber:{}",idempotent.getSerialNumber());
            throw new IllegalIdempotentArgumentException(String.format("update idempotent error.info:[%s]",idempotent.getSerialNumber()));
        }
    }
}