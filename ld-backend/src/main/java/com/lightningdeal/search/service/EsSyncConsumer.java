package com.lightningdeal.search.service;

import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.search.entity.ActivityIndex;
import com.lightningdeal.search.model.EsSyncMessage;
import com.lightningdeal.search.repository.ActivitySearchRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ES 同步 MQ 消费者
 *
 * 异步处理 ES 同步操作，与 DB 操作解耦。
 * 如果 ES 写入失败，MQ 会自动重试（默认 3 次），
 * 重试耗尽后转发到死信队列，不丢失同步请求。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsSyncConsumer {

    private final ActivitySearchRepository repository;
    private final SeckillActivityService activityService;

    @RabbitListener(queues = "seckill.es.queue")
    public void handleEsSync(EsSyncMessage message, Channel channel, @org.springframework.messaging.handler.annotation.Header("amqp_deliveryTag") long deliveryTag) throws IOException {
        try {
            log.debug("ES 同步消息: action={}, activityId={}", message.getAction(), message.getActivityId());

            if ("delete".equals(message.getAction())) {
                repository.deleteById(message.getActivityId());
                log.info("ES 删除索引 activityId={}", message.getActivityId());
            } else {
                SeckillActivity activity = activityService.getById(message.getActivityId());
                if (activity == null) {
                    log.warn("ES 同步：活动不存在 activityId={}，删除索引", message.getActivityId());
                    repository.deleteById(message.getActivityId());
                } else {
                    ActivityIndex index = toIndex(activity);
                    repository.save(index);
                    log.info("ES 同步索引 activityId={}, name={}", activity.getId(), activity.getName());
                }
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("ES 同步失败 action={}, activityId={}", message.getAction(), message.getActivityId(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private ActivityIndex toIndex(SeckillActivity activity) {
        ActivityIndex index = new ActivityIndex();
        org.springframework.beans.BeanUtils.copyProperties(activity, index);
        return index;
    }
}
