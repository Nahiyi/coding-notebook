package cn.clazs.kafkasb.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Topic 配置
 * 用于自动创建topic并设置分区数
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * KafkaAdmin：用于管理topic（创建、删除等）
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * 创建topic1（3个分区，1个副本）
     *
     * 注意：
     * 1. 如果topic已存在，此配置不会修改现有topic的分区数
     * 2. 要增加分区数，需要使用命令：kafka-topics.sh --alter --topic topic1 --partitions 5
     * 3. 分区数只能增加，不能减少
     */
    @Bean
    public NewTopic topic1() {
        return TopicBuilder.name("topic1")
                .partitions(3)  // 设置3个分区
                .replicas(1)     // 每个分区1个副本（开发环境够用，生产环境建议3个）
                .build();
    }

    /**
     * 如果需要创建更多topic，可以添加更多的Bean方法
     * 例如：
     *
     * @Bean
     * public NewTopic topic2() {
     *     return TopicBuilder.name("topic2")
     *             .partitions(5)
     *             .replicas(1)
     *             .build();
     * }
     */
}
