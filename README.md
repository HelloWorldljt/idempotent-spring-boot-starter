### 幂等性检查插件

#### 版本变化

##### 0.0.7版本
- 优化checkMethod返回值的处理
- 增加处理前事务检测机制，如果幂等标签之前存在事务，则抛出异常
- 去掉异常存储，防止序列化异常，防止存储大对象

##### 0.0.6版本 
- 去掉对参数，类名，方法签名的序列化；返回值采用JSON存储，不影响修改包名或者修改参数；

##### 0.0.5版本
- 修复由于项目配置`jdbc-type-for-null: NULL`导致SQL更新一个NULL值时报错的BUG

##### 0.0.4版本
- 修复Kryo序列化造成的大小问题，由于最小大小是4k，修改为64M,但不建议存大对象

##### 0.0.3版本
- 优化中间状态处理，比如处理中状态，使用乐观锁来处理并发问题
- 增加序列化方法，增加项目名称，修改表结构

#### 概念

幂等记录(common_idempotent)

业务KEY

幂等状态：

```java
WAITING("处理中状态"),
SUCCESS("成功状态"),
UNKNOWN("未知状态"),
RETRYING("可重试状态");
```

#### 实现原理说明

使用AOP拦截方法，做幂等性校验；

详见流程图

#### 注意事项

1. 不支持嵌套调用
2. 不支持同类方法调用（AOP机制）
3. check方法返回类型固定`IdempotentCheckResult`，参数类型需和原有方法一致
4. 上线升级接口变动的处理？从0.0.6版本开始，使用JSON格式存储返回值；类名，方法名，参数类型，参数值一概不存储。支持接口变动与参数修改，支持返回值修改
5. 停机升级，造成的处理中状态的处理？人工介入监控
6. 项目启动时 会检查`IdempotentMethodRegister`是否进行幂等方法注册，包名+方法名
7. 幂等注解与最终一致性注解不可同时加在一个方法上
8. 如果业务已经做了幂等处理，不应该使用幂等注解
9. ***幂等插件在做数据库插入，更新时会把当前事务挂起；***  
10. 加入事务检测；（如果拦截之前已经存在事务，则抛出异常）

#### IdempotentKey的生成规则

bizKey 应该是带有业务属性；

比如首购发送优惠券：String bizKey = "first_buy_order__"+order_id+"_"+user_id;

比如每月礼包优惠券:   String bizKey = "every_month_gift_"+user_id+"20190701";


如果由于业务需要，需要进行 bizKey的变更，这时候需要注意， bizKey的生成规则也需要进行变更；

```java
IdempotentKey idempotentKey = IdempotentKeyBuilder.builder().withBizType(IdempotentBizType.INVEST_ADD_INTEGRAL).withBizKey(bizKey).build();		
```

#### 典型错误使用案例

如果此时，function2发生异常，会导致整个事务回滚。由于幂等插件不会挂起当前事务，所以如果此时发生回滚，幂等记录不受影响；但是此时业务代码会回滚；

那么第二次执行test方法时，function1就会被幂等框架拦截；导致业务不会执行；

```java
@IdempotentBizCheck
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
public void test(){
        aaa.function1();
        bbb.function2();
}

@IdempotentBizCheck
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
public void function1(){}


@IdempotentBizCheck
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
public void function2(){}
```



#### 项目依赖

1. 依赖middleware-sdk
2. 依赖micro-common
3. 依赖mysql（common_idempotent）
4. idempotentType 由架构组维护

#### 版本变化

1. 去掉globalResponse依赖
2. 增加业务方法拦截
3. 使用json存储返回值，不在存储方法签名

#### 使用方法

```xml
<dependency>
            <groupId>com.xiangshang.extension</groupId>
            <artifactId>idempotent-spring-boot-starter</artifactId>
            <version>0.0.7</version>
</dependency>  
```

在`application.yml` 增加如下配置：

```yaml
idempotent:
  enable: true 
```



示例代码：

一般用于三方，严禁内部项目调用使用HTTP接口幂等注解

##### http接口幂等：

```java
    /**
     * 担保户可用增加，到账增加
     * @param reqBody
     * @return
     */
    @IdempotentHttpCheck
    @RequestMapping(value = "/plusAvailable",method = RequestMethod.POST)
    public GlobalResponseEntity plusAvailable(@RequestBody AccountTransactionDTO reqBody){
        LOG.info("assure plusAvailable params :[{}]", JSON.toJSONString(reqBody));
        accountValidateService.validateIntegrity(reqBody);
        String bieKey = getBizKey(reqBody.getData());
        accountOperationBusiness.plusAvailable(bieKey,reqBody.getData());
        return GlobalResponseEntity.success();
    }
```

##### 业务层方法幂等

```java
@IdempotentBizCheck(unKnownException = {ProcessingException.class},checkMethod = "checkPlus")
@Override
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
public void plusAvailable(@BizKey IdempotentKey serialNumber, AccountTransactionDTO accountTransactionDTO) {
    assureFundsAccountService.plusAvailable(accountTransactionDTO);
}

public IdempotentCheckResult checkPlus(@BizKey IdempotentKey serialNumber, AccountTransactionDTO accountTransactionDTO) {
    System.out.println("check plus");
    IdempotentCheckResult result = new IdempotentCheckResult(true);
    System.out.println(JSON.toJSONString(accountTransactionDTO));
    return result;
}
```



`IdempotentBizCheck`参数解释：

unKnownException:出现的未知异常集合

checkMethod:重试检查的方法，必须放在当前类中；如果抛出指定异常，表示未知状态，此时需要重试，重试的时候会先检查方法是否执行成功；（典型的使用场景有调用银行出现未知异常）

有任何问题，请及时联系；




