package cn.clazs.websocket.handler;

import cn.clazs.websocket.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * RabbitMQ消息处理器实现
 *
 * 基于RabbitMQ的Fanout交换机实现跨服务器消息转发
 * 配置：message.broker.type=rabbitmq 时启用
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "message.broker.type", havingValue = "rabbitmq")
public class RabbitMQMessageHandler implements MessageHandler {

    private static final String EXCHANGE_NAME = "websocket.fanout.exchange";

    private final RabbitTemplate rabbitTemplate;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * 应用启动时订阅消息
     */
    @PostConstruct
    @Override
    public void subscribe() {
        log.info("正在订阅RabbitMQ交换机: {}", EXCHANGE_NAME);

        try {
            // 创建Fanout交换机（直接创建，不注入，避免循环依赖）
            FanoutExchange fanoutExchange = new FanoutExchange(EXCHANGE_NAME, true, false);

            // 创建AMQP管理器
            AmqpAdmin amqpAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());

            // 先声明交换机（重要：必须先声明才能绑定队列）
            amqpAdmin.declareExchange(fanoutExchange);
            log.info("RabbitMQ交换机已声明: {}", EXCHANGE_NAME);

            // 创建临时队列
            Queue queue = amqpAdmin.declareQueue();

            // 绑定队列到Fanout交换机
            amqpAdmin.declareBinding(BindingBuilder
                    .bind(queue)
                    .to(fanoutExchange)
            );

            log.info("RabbitMQ临时队列已创建并绑定: {} -> {}", queue.getName(), EXCHANGE_NAME);

            // 创建消息监听容器
            SimpleMessageListenerContainer container =
                    new SimpleMessageListenerContainer(rabbitTemplate.getConnectionFactory());

            container.setQueueNames(queue.getName());
            container.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        // 反序列化消息
                        byte[] body = message.getBody();
                        String json = new String(body);
                        ChatMessage chatMessage = objectMapper.readValue(json, ChatMessage.class);

                        log.info("收到RabbitMQ消息 - 发送者: {}, 接收者: {}, 内容: {}",
                                chatMessage.getSenderSessionId(),
                                chatMessage.getReceiverSessionId(),
                                chatMessage.getContent());

                        // 尝试在本地发送消息
                        boolean sent = webSocketHandler.sendMessageToUser(
                                chatMessage.getReceiverSessionId(),
                                chatMessage.getContent()
                        );

                        if (sent) {
                            log.info("本服务器成功向用户 [{}] 发送了消息",
                                    chatMessage.getReceiverSessionId());
                        } else {
                            log.info("本服务器未找到用户 [{}]，忽略此消息",
                                    chatMessage.getReceiverSessionId());
                        }

                    } catch (Exception e) {
                        log.error("处理RabbitMQ消息失败", e);
                    }
                }
            });

            container.start();

            log.info("RabbitMQ消息订阅成功: {}", EXCHANGE_NAME);

        } catch (Exception e) {
            log.error("订阅RabbitMQ失败，完整错误信息：", e);
            log.error("错误类型: {}", e.getClass().getName());
            log.error("错误消息: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("根本原因: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("订阅RabbitMQ失败，请检查RabbitMQ配置和服务状态。错误: " + e.getMessage(), e);
        }
    }

    @Override
    public void publish(String senderSessionId, String receiverSessionId, String content) {
        ChatMessage chatMessage = new ChatMessage(senderSessionId, receiverSessionId, content);

        // 发布到Fanout交换机（Spring AMQP会自动序列化）
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, "", chatMessage);

        log.info("已发布消息到RabbitMQ - 发送者: {}, 接收者: {}, 内容: {}",
                senderSessionId, receiverSessionId, content);
    }

    @Override
    public String getType() {
        return "rabbitmq";
    }
}
