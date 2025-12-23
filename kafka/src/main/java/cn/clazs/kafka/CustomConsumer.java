package cn.clazs.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * Kafka消费者 - 基础示例
 * 演示如何从Kafka接收消息
 */
public class CustomConsumer {
    public static void main(String[] args) {
        // 1. 创建配置
        Properties props = new Properties();
        props.put("bootstrap.servers", "172.23.146.17:9092");
        props.put("group.id", "test-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // 2. 创建消费者
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // 3. 订阅主题
        consumer.subscribe(Collections.singletonList("topic1"));

        // 4. 消费消息
        System.out.println("消费者启动，等待消息...");
        try {
            while (true) {
                // 每隔1秒拉取一批消息
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                // 遍历并打印消息
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("收到消息: " + record.value() +
                            " (分区: " + record.partition() +
                            ", 偏移量: " + record.offset() + ")");
                }
            }
        } finally {
            consumer.close();
        }
    }
}