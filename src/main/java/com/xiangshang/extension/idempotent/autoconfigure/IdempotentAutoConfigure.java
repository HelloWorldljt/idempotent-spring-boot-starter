package com.xiangshang.extension.idempotent.autoconfigure;

import com.xiangshang.extension.idempotent.aspect.IdempotentAspect;
import com.xiangshang.extension.idempotent.aspect.IdempotentBizAspect;
import com.xiangshang.extension.idempotent.aspect.IdempotentHttpAspect;
import com.xiangshang.extension.idempotent.config.IdempotentConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
@description spring boot 从此处加载 幂等检查 插件
@author tanyuanpeng
@date 2018/12/12
**/
@Configuration
@EnableConfigurationProperties({IdempotentConfig.class})
@ConditionalOnExpression("${idempotent.enable:true}")
@ComponentScan(
    basePackages = {"com.xiangshang.extension.idempotent"}
)
@MapperScan({"com.xiangshang.extension.idempotent.domain.dao"})
public class IdempotentAutoConfigure {

    @Bean
    public IdempotentAspect idempotentAspect() {
        return new IdempotentAspect();
    }

    @Bean
    public IdempotentBizAspect idempotentBizAspect() {
        return new IdempotentBizAspect();
    }

    @Bean
    public IdempotentHttpAspect idempotentHttpAspect() {
        return new IdempotentHttpAspect();
    }

}