package com.lightningdeal.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.connection.ConnectionFactory")
@ConditionalOnProperty(value = "spring.rabbitmq.listener.simple.auto-startup", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    // ===== 秒杀订单 - 交换机 & 队列 =====
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUEUE = "seckill.queue";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    // ===== 死信队列 =====
    public static final String DEAD_EXCHANGE = "seckill.dead.exchange";
    public static final String DEAD_QUEUE = "seckill.dead.queue";
    public static final String DEAD_ROUTING_KEY = "seckill.dead";

    // ===== 延迟队列（用于超时未支付取消订单） =====
    public static final String DELAY_EXCHANGE = "seckill.delay.exchange";
    public static final String DELAY_QUEUE = "seckill.delay.queue";
    public static final String DELAY_ROUTING_KEY = "seckill.delay";

    /**
     * 秒杀订单交换机 (topic)
     */
    @Bean
    public TopicExchange seckillExchange() {
        return ExchangeBuilder.topicExchange(SECKILL_EXCHANGE).durable(true).build();
    }

    /**
     * 秒杀订单队列
     */
    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定秒杀队列到交换机
     */
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadExchange() {
        return ExchangeBuilder.directExchange(DEAD_EXCHANGE).durable(true).build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    /**
     * 绑定死信队列
     */
    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(deadQueue()).to(deadExchange()).with(DEAD_ROUTING_KEY);
    }

    /**
     * 延迟交换机（使用 RabbitMQ 插件 rabbitmq_delayed_message_exchange）
     * <p>
     * 必须在 RabbitMQ 中启用插件：rabbitmq-plugins enable rabbitmq_delayed_message_exchange
     */
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(DELAY_EXCHANGE, "x-delayed-message", true, false, args);
    }

    /**
     * 延迟队列
     */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE).build();
    }

    /**
     * 绑定延迟队列
     */
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(DELAY_ROUTING_KEY).noargs();
    }

    /**
     * JSON 序列化
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 消费失败重试后转发到死信交换机
     */
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DEAD_EXCHANGE, DEAD_ROUTING_KEY);
    }
}
