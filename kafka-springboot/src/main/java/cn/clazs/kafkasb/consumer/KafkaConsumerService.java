package cn.clazs.kafkasb.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费者服务
 */
@Slf4j
@Component
public class KafkaConsumerService {

    /**
     * 监听主题消费消息
     * topics: 要监听的Kafka主题
     * groupId: 消费者组ID（可选，如果不指定则使用配置文件中的默认值）
     *
     * @param record 消费的消息记录
     */
    @KafkaListener(topics = "${kafka.topic.default}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMessage(ConsumerRecord<String, String> record) {
        log.info("========== 收到消息 ==========");
        log.info("主题: {}", record.topic());
        log.info("分区: {}", record.partition());
        log.info("偏移量: {}", record.offset());
        log.info("Key: {}", record.key());
        log.info("Value: {}", record.value());
        log.info("时间戳: {}", record.timestamp());
        log.info("==============================");

        // 具体业务处理逻辑
        processMessage(record.value());
    }

    /**
     * 处理消息的业务逻辑
     *
     * @param message 消息内容
     */
    private void processMessage(String message) {
        // 模拟业务处理
        try {
            // 这里可以实现你的具体业务逻辑
            log.info("【模拟】正在处理消息: {}", message);

            // 模拟处理耗时
            Thread.sleep(100);

            log.info("【模拟】消息处理完成: {}", message);
        } catch (InterruptedException e) {
            log.error("消息处理异常: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("消息处理失败: {}", e.getMessage());
        }
    }
}
