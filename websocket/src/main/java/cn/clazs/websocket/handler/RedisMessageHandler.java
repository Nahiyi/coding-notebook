package cn.clazs.websocket.handler;

import cn.clazs.websocket.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Redis消息处理器实现
 *
 * 基于Redis的Pub/Sub机制实现跨服务器消息转发
 * 配置：message.broker.type=redis 时启用
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "message.broker.type", havingValue = "redis")
public class RedisMessageHandler implements MessageHandler {

    private static final String TOPIC_NAME = "MY_MESSAGE_TOPIC";

    private final RedissonClient redissonClient;
    private final WebSocketHandler webSocketHandler;

    /**
     * 应用启动时自动订阅Redis主题
     */
    @PostConstruct
    @Override
    public void subscribe() {
        log.info("正在订阅Redis主题: {}", TOPIC_NAME);

        RTopic topic = redissonClient.getTopic(TOPIC_NAME, new TypedJsonJacksonCodec(ChatMessage.class));

        topic.addListener(ChatMessage.class, (channel, msg) -> {
            log.info("收到Redis消息 - 发送者: {}, 接收者: {}, 内容: {}",
                    msg.getSenderSessionId(), msg.getReceiverSessionId(), msg.getContent());

            // 尝试在本地发送消息
            boolean sent = webSocketHandler.sendMessageToUser(
                msg.getReceiverSessionId(),
                msg.getContent()
            );

            if (sent) {
                log.info("本服务器成功向用户 [{}] 发送了消息", msg.getReceiverSessionId());
            } else {
                log.info("本服务器未找到用户 [{}]，忽略此消息", msg.getReceiverSessionId());
            }
        });

        log.info("Redis主题订阅成功: {}", TOPIC_NAME);
    }

    @Override
    public void publish(String senderSessionId, String receiverSessionId, String content) {
        ChatMessage chatMessage = new ChatMessage(senderSessionId, receiverSessionId, content);

        RTopic topic = redissonClient.getTopic(TOPIC_NAME, new TypedJsonJacksonCodec(ChatMessage.class));
        topic.publish(chatMessage);

        log.info("已发布消息到Redis - 发送者: {}, 接收者: {}, 内容: {}",
                senderSessionId, receiverSessionId, content);
    }

    @Override
    public String getType() {
        return "redis";
    }
}
