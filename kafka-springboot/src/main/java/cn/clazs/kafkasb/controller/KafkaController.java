package cn.clazs.kafkasb.controller;

import cn.clazs.kafkasb.producer.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * Kafka 消息发送控制器
 */
@Slf4j
@RestController
@RequestMapping("/kafka")
public class KafkaController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Value("${kafka.topic.default}")
    private String defaultTopic;

    /**
     * 发送消息到默认主题
     *
     * @param message 消息内容
     * @return 发送结果
     */
    @GetMapping("/send")
    public String sendMessage(@RequestParam("message") String message) {
        log.info("接收到发送请求: {}", message);
        kafkaProducerService.sendMessage(defaultTopic, message);
        return "消息已提交发送: " + message;
    }

    /**
     * 发送消息到指定主题
     *
     * @param topic   主题名称
     * @param message 消息内容
     * @return 发送结果
     */
    @PostMapping("/send")
    public String sendMessageToTopic(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestBody String message) {
        String targetTopic = (topic != null && !topic.isEmpty()) ? topic : defaultTopic;
        log.info("接收到发送请求到主题 {}: {}", targetTopic, message);
        kafkaProducerService.sendMessage(targetTopic, message);
        return "消息已提交发送到主题 " + targetTopic + ": " + message;
    }

    /**
     * 发送带key的消息
     *
     * @param key     消息key
     * @param message 消息内容
     * @return 发送结果
     */
    @PostMapping("/send-with-key")
    public String sendMessageWithKey(
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam("key") String key,
            @RequestBody String message) {
        String targetTopic = (topic != null && !topic.isEmpty()) ? topic : defaultTopic;
        log.info("接收到发送请求到主题 {} (key: {}): {}", targetTopic, key, message);
        kafkaProducerService.sendMessageWithKey(targetTopic, key, message);
        return "带Key的消息已提交发送到主题 " + targetTopic + " (key: " + key + "): " + message;
    }

    /**
     * 批量发送消息
     *
     * @param count 消息数量
     * @return 发送结果
     */
    @GetMapping("/batch-send")
    public String batchSendMessage(@RequestParam(value = "count", defaultValue = "10") int count) {
        log.info("开始批量发送 {} 条消息", count);

        for (int i = 1; i <= count; i++) {
            String message = "批量消息-" + i;
            kafkaProducerService.sendMessage(defaultTopic, message);

            // 添加小延迟，避免发送过快
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return "已批量发送 " + count + " 条消息到主题: " + defaultTopic;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "Kafka Spring Boot 服务运行正常！";
    }
}
