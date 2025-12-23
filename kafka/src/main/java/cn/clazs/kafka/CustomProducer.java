package cn.clazs.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;

/**
 * Kafka生产者 - 基础示例
 * 演示如何发送消息到Kafka
 */
public class CustomProducer {
    public static void main(String[] args) {
        // 1. 创建配置
        Properties props = new Properties();
        props.put("bootstrap.servers", "172.23.146.17:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        // 2. 创建生产者
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // 3. 发送消息
        for (int i = 0; i < 1000; i++) {
            // 创建消息（指定topic和消息内容）
            ProducerRecord<String, String> record = new ProducerRecord<>("topic1", "消息-" + i);

            try {
                // 同步发送消息（等待服务器响应）
                RecordMetadata metadata = producer.send(record).get();
                System.out.println("发送成功: " + record.value() + " -> 分区: " + metadata.partition());
                Thread.sleep(500);
            } catch (Exception e) {
                System.err.println("发送失败: " + e.getMessage());
            }
        }

        // 4. 关闭生产者
        producer.close();
        System.out.println("生产者关闭");
    }
}