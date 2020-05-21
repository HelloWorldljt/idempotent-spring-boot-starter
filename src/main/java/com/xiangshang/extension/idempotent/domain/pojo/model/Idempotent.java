package com.xiangshang.extension.idempotent.domain.pojo.model;

import com.xiangshang.extension.idempotent.domain.em.IdempotentStatus;
import com.xs.micro.common.middleware.entity.MethodInvocation;

import java.util.Date;

/**
@description 幂等记录
@author tanyuanpeng
@date 2018/12/12
**/
public class Idempotent {

    private Long id;

    private String serialNumber;
    /**
     * 项目名称
     */
    private String appName;
    /**
     * 调用类名
     */
    private String apiClass;
    /**
     * 调用方法名
     */
    private String apiMethod;
    /**
     * 状态
     */
    private IdempotentStatus status;

    /**
     * 调用签名封装
     */
    private MethodInvocation methodInvocation;
    /**
     * 备注
     */
    private String remark;

    private Date createTime;
    private Date updateTime;
    private Integer version;

    public Idempotent() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSerialNumber() {
        return this.serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public IdempotentStatus getStatus() {
        return this.status;
    }

    public void setStatus(IdempotentStatus status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getApiClass() {
        return apiClass;
    }

    public void setApiClass(String apiClass) {
        this.apiClass = apiClass;
    }

    public String getApiMethod() {
        return apiMethod;
    }

    public void setApiMethod(String apiMethod) {
        this.apiMethod = apiMethod;
    }

    public MethodInvocation getMethodInvocation() {
        return methodInvocation;
    }

    public void setMethodInvocation(MethodInvocation methodInvocation) {
        this.methodInvocation = methodInvocation;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "Idempotent{" +
                "id=" + id +
                ", serialNumber='" + serialNumber + '\'' +
                ", appName='" + appName + '\'' +
                ", apiClass='" + apiClass + '\'' +
                ", apiMethod='" + apiMethod + '\'' +
                ", status=" + status +
                ", methodInvocation=" + methodInvocation +
                ", remark='" + remark + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", version=" + version +
                '}';
    }
}