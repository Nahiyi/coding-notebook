package cn.clazs.kafkasb.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Kafka 生产者服务
 */
@Slf4j
@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息（异步）
     *
     * @param topic   主题
     * @param message 消息内容
     */
    public void sendMessage(String topic, String message) {
        log.info("准备发送消息: {} 到主题: {}", message, topic);

        // 异步发送消息
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);

        // 添加回调函数处理发送结果
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("消息发送成功！主题: {}, 分区: {}, 偏移量: {}, 消息: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        message);
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("消息发送失败！主题: {}, 消息: {}, 错误: {}",
                        topic, message, ex.getMessage());
            }
        });
    }

    /**
     * 发送消息（指定key）
     * 使用key可以确保相同key的消息发送到同一个分区
     *
     * @param topic   主题
     * @param key     消息key
     * @param message 消息内容
     */
    public void sendMessageWithKey(String topic, String key, String message) {
        log.info("准备发送消息到主题 {} (key: {}): {}", topic, key, message);

        ListenableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, key, message);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("消息发送成功！主题: {}, key: {}, 分区: {}, 偏移量: {}, 消息: {}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        message);
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("消息发送失败！主题: {}, key: {}, 消息: {}, 错误: {}",
                        topic, key, message, ex.getMessage());
            }
        });
    }
}
