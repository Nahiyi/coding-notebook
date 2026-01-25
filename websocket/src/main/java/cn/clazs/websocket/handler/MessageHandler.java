package cn.clazs.websocket.handler;

/**
 * 消息处理器接口
 *
 * 统一的消息处理规范，支持多种实现（Redis、RabbitMQ等）
 */
public interface MessageHandler {

    /**
     * 订阅消息
     * 应用启动时自动调用，监听消息代理的消息
     */
    void subscribe();

    /**
     * 发布消息
     * 将消息发布到消息代理，广播给所有订阅的服务器
     *
     * @param senderSessionId 发送者会话ID
     * @param receiverSessionId 接收者会话ID
     * @param content 消息内容
     */
    void publish(String senderSessionId, String receiverSessionId, String content);

    /**
     * 获取处理器类型
     *
     * @return 类型标识（redis、rabbitmq等）
     */
    String getType();
}
