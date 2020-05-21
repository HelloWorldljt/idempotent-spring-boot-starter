package com.xiangshang.extension.idempotent.domain.dao;

import com.xiangshang.extension.idempotent.domain.em.IdempotentStatus;
import com.xiangshang.extension.idempotent.domain.pojo.model.Idempotent;
import com.xs.micro.common.middleware.entity.MethodInvocation;
import com.xs.micro.common.middleware.serialize.SerializeTypeHandler;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Repository;

import java.util.Date;


/**
@description  幂等表
@author tanyuanpeng
@date 2018/12/12
**/
@Repository
public interface IdempotentMapper {

    String BASE_COLS = "id,app_name,serial_number,api_class,api_method,method_invocation,status,remark,create_time,update_time,version";

    String SIMPLE_COLS = "id,serial_number,method_invocation,status,create_time,update_time,version";
    /**
     * 查询记录
     * @param serialNumber
     * @return
     */
    @Select({"select "+SIMPLE_COLS+" from common_idempotent where serial_number=#{serialNumber}"})
    @Results({
            @Result(id = true, column = "id", property = "id",jdbcType = JdbcType.BIGINT),
            @Result(column = "serial_number", property = "serialNumber",jdbcType = JdbcType.VARCHAR),
            @Result(column = "method_invocation", property = "methodInvocation",jdbcType = JdbcType.LONGVARBINARY,typeHandler = SerializeTypeHandler.class),
            @Result(column = "status", property = "status",jdbcType = JdbcType.VARCHAR),
            @Result(column = "create_time", property = "createTime",jdbcType = JdbcType.DATE),
            @Result(column = "update_time", property = "updateTime",jdbcType = JdbcType.TIMESTAMP),
            @Result(column = "version", property = "version",jdbcType = JdbcType.INTEGER) })
    Idempotent queryBySerialNumber(@Param("serialNumber") String serialNumber);

    /**
     * 插入记录
     * @param record
     */
    @Insert({"insert into common_idempotent ("+BASE_COLS+") values (#{id,jdbcType=INTEGER},#{appName,jdbcType=VARCHAR},#{serialNumber,jdbcType=VARCHAR},#{apiClass,jdbcType=VARCHAR},#{apiMethod,jdbcType=VARCHAR}," +
            "#{methodInvocation,jdbcType=LONGVARBINARY, typeHandler=com.xs.micro.common.middleware.serialize.SerializeTypeHandler},"+
            "#{status,jdbcType=VARCHAR},#{remark,jdbcType=VARCHAR},#{createTime,jdbcType=TIMESTAMP},#{updateTime,jdbcType=TIMESTAMP},#{version,jdbcType=INTEGER})"})
    void insert(Idempotent record);

    /**
     * 使用乐观锁更新
     * @param serialNumber
     * @param newStatus
     * @param oldStatus
     * @param updateTime
     * @param version
     * @return
     */
    @Update({"update common_idempotent set status=#{newStatus,jdbcType=VARCHAR},update_time=#{updateTime,jdbcType=TIMESTAMP},version=version+1 where serial_number=#{serialNumber,jdbcType=VARCHAR} and status=#{oldStatus,jdbcType=VARCHAR} and version=#{version,jdbcType=INTEGER}"})
    int updateStatusByVersion(@Param("serialNumber") String serialNumber, @Param("newStatus") IdempotentStatus newStatus, @Param("oldStatus") IdempotentStatus oldStatus, @Param("updateTime") Date updateTime, @Param("version")Integer version);

    /**
     * 更新状态
     * @param serialNumber
     * @param newStatus
     * @param oldStatus
     * @param methodInvocation
     * @param version
     * @param currentDate
     * @return
     */
    @Update({"update common_idempotent set status=#{newStatus,jdbcType=VARCHAR},update_time=#{updateTime,jdbcType=TIMESTAMP},method_invocation=#{methodInvocation,jdbcType=LONGVARBINARY, typeHandler=com.xs.micro.common.middleware.serialize.SerializeTypeHandler},remark=#{remark,jdbcType=VARCHAR},version=version+1 where serial_number=#{serialNumber,jdbcType=VARCHAR} and status=#{oldStatus,jdbcType=VARCHAR} and version=#{version,jdbcType=INTEGER}"})
    int updateStatusAndReturn(@Param("serialNumber")String serialNumber, @Param("newStatus")IdempotentStatus newStatus, @Param("oldStatus")IdempotentStatus oldStatus, @Param("methodInvocation")MethodInvocation methodInvocation,@Param("remark")String remark, @Param("version")Integer version, @Param("updateTime")Date currentDate);


}