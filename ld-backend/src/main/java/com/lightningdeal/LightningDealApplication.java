package com.lightningdeal;

import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ⚡ LightningDeal 秒杀系统 - 启动入口
 */
@SpringBootApplication(exclude = {RedissonAutoConfiguration.class, RedissonAutoConfigurationV2.class})
@EnableScheduling
public class LightningDealApplication {

    public static void main(String[] args) {
        SpringApplication.run(LightningDealApplication.class, args);
    }
}
