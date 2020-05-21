-- -------------------------------------------------
-- 通用幂等性表(该表不能做业务，会定期删除)
-- -------------------------------------------------
CREATE TABLE common_idempotent
(
    id int PRIMARY KEY NOT NULL AUTO_INCREMENT,
    serial_number varchar(64) NOT NULL COMMENT '请求流水号',
    status varchar(20) NOT NULL COMMENT '处理状态',
    create_time datetime NOT NULL COMMENT '创建时间',
    update_time datetime NOT NULL COMMENT '更新时间',
    version int NOT NULL COMMENT '版本号'
)  ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT = '订单中心幂等性';
CREATE UNIQUE INDEX serial_number_index ON common_idempotent(serial_number);

-- -------------------------------------------------
-- 通用幂等性表
-- -------------------------------------------------
CREATE TABLE common_idempotent_detail
(
    id int PRIMARY KEY NOT NULL AUTO_INCREMENT,
    serial_number varchar(64) NOT NULL COMMENT '业务流水号',
    content varchar(2048) COMMENT '业务快照'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT = '订单中心幂等性详情';
CREATE UNIQUE INDEX serial_number_index ON common_idempotent_detail (serial_number);


---增加调用类名与方法名
alter table common_idempotent
add app_name varchar(30) default null comment '项目名称' after id,
add api_class varchar(255) default null comment '调用类' after serial_number,
add api_method varchar(255) default null comment '调用方法' after api_class,
add method_invocation longblob default null comment '方法签名序列化' after api_method,
add remark varchar(150) default null comment '备注';


