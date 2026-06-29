package com.lightningdeal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ⚡ LightningDeal 秒杀系统 - 启动入口
 */
@SpringBootApplication
@EnableScheduling
public class LightningDealApplication {

    public static void main(String[] args) {
        SpringApplication.run(LightningDealApplication.class, args);
    }
}
