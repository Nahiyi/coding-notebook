package cn.clazs.websocket.handler;

import cn.clazs.websocket.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Redis消息处理器
 *
 * 核心功能：通过Redis的发布/订阅机制实现跨服务器的WebSocket消息转发
 *
 * 工作原理：
 * 1. 每台WebSocket服务器启动时自动订阅Redis主题（MY_MESSAGE_TOPIC）
 * 2. 当需要发送点对点消息时，将消息发布到Redis主题
 * 3. 所有订阅了该主题的服务器都会收到消息
 * 4. 每台服务器都在本地查找目标用户
 * 5. 只有目标用户所在的服务器才能真正发送消息，其他服务器忽略
 *
 * @author clazs
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageHandler {

    /**
     * Redis主题名称
     * 所有WebSocket服务器都订阅这个主题，实现消息广播
     */
    public static final String TOPIC_NAME = "MY_MESSAGE_TOPIC";

    /**
     * Redisson客户端，用于操作Redis
     * 提供发布/订阅、分布式对象等功能
     */
    private final RedissonClient redissonClient;

    /**
     * WebSocket处理器
     * 用于在本地查找目标用户并发送消息
     */
    private final WebSocketHandler webSocketHandler;

    /**
     * 应用启动时自动订阅Redis主题
     * <p>
     * &#064;PostConstruct注解说明：
     * - 该注解由Java提供（javax.annotation.PostConstruct）
     * - 在Spring容器完成Bean的依赖注入后自动调用
     * - 在构造方法之后、初始化方法之前执行
     * - 保证所有@Autowired的字段都已注入完成
     * <p>
     * 执行时机：
     * 1. Spring启动
     * 2. 创建RedisMessageHandler单例Bean
     * 3. 注入redissonClient和webSocketHandler
     * 4. 调用@PostConstruct标注的init()方法
     * 5. 开始监听Redis主题
     */
    @PostConstruct
    public void init() {
        listenTopic();
        log.info("Redis消息订阅已启动，正在监听主题: {}", TOPIC_NAME);
    }

    /**
     * 监听Redis主题，处理收到的消息
     *
     * 关键技术点：
     * 1. RTopic：Redisson提供的发布/订阅接口
     * 2. TypedJsonJacksonCodec：JSON序列化编解码器
     * 3. addListener()：添加消息监听器
     */
    private void listenTopic() {
        /*
         * 获取Redis主题对象
         *
         * 参数说明：
         * - TOPIC_NAME：主题名称，字符串类型
         * - new TypedJsonJacksonCodec(ChatMessage.class)：序列化编解码器
         *
         * TypedJsonJacksonCodec作用：
         * 1. 发布消息时：将ChatMessage对象序列化为JSON字符串
         * 2. 接收消息时：将JSON字符串反序列化为ChatMessage对象
         *
         * 为什么需要Codec？
         * Redis只能存储字符串和字节，不能直接存储Java对象
         * 必须将对象序列化为JSON才能在Redis中传输
         */
        RTopic topic = redissonClient.getTopic(TOPIC_NAME, new TypedJsonJacksonCodec(ChatMessage.class));

        /*
         * 添加消息监听器
         *
         * 方法签名：topic.addListener(type, listener)
         *
         * 参数1：Class<T> type - 消息的类型
         *   - ChatMessage.class：表示我们监听的是ChatMessage类型的消息
         *   - Redisson收到消息后，会自动将JSON反序列化为ChatMessage对象
         *   - 如果类型不匹配，监听器不会收到消息
         *
         * 参数2：MessageListener<T> listener - 消息监听器
         *   - 这是一个函数式接口（@FunctionalInterface）
         *   - 只有一个抽象方法：void onMessage(CharSequence channel, T msg)
         *   - 可以使用Lambda表达式或匿名内部类实现
         *
         * Lambda表达式：(channel, msg) -> { ... }
         *   - channel：Redis主题的名称（CharSequence类型）
         *   - msg：接收到的消息对象（ChatMessage类型）
         *   - Lambda体：处理消息的逻辑
         *
         * 执行流程：
         * 1. 有消息发布到MY_MESSAGE_TOPIC主题
         * 2. Redisson收到消息，从Redis读取JSON字符串
         * 3. TypedJsonJacksonCodec将JSON反序列化为ChatMessage对象
         * 4. 调用监听器的onMessage方法，传入channel和msg
         * 5. 执行Lambda表达式中的代码
         */
        topic.addListener(ChatMessage.class, (channel, msg) -> {
            /*
             * 处理接收到的消息
             *
             * 参数说明：
             * - channel：Redis主题名称（CharSequence）
             *   - 实际值就是上面的"MY_MESSAGE_TOPIC"
             *   - 可以用来验证消息来源
             *
             * - msg：消息对象（ChatMessage）
             *   - 已经被自动反序列化
             *   - 可以直接调用getter方法获取字段值
             */
            log.info("收到Redis消息 - 发送者: {}, 接收者: {}, 内容: {}",
                    msg.getSenderSessionId(), msg.getReceiverSessionId(), msg.getContent());

            /*
             * 尝试在本地服务器查找目标用户并发送消息
             *
             * 关键点：
             * - 每台服务器都会执行这个方法
             * - 只有目标用户所在的服务器才能找到并发送成功
             * - 其他服务器找不到目标用户，返回false，忽略消息
             *
             * 例如：
             * 服务器1（9058端口）：用户A
             * 服务器2（9090端口）：用户B
             * 用户A发送消息给用户B：
             *   1. 消息发布到Redis
             *   2. 服务器1收到消息，在本地查找用户B -> 找不到 -> 返回false
             *   3. 服务器2收到消息，在本地查找用户B -> 找到了 -> 发送成功 -> 返回true
             */
            boolean sent = webSocketHandler.sendMessageToUser(msg.getReceiverSessionId(), msg.getContent());

            if (sent) {
                log.info("本服务器成功向用户 [{}] 发送了消息", msg.getReceiverSessionId());
            } else {
                log.info("本服务器未找到用户 [{}]，忽略此消息", msg.getReceiverSessionId());
            }
        });

        /*
         * 如果使用匿名内部类（不推荐，代码更冗长）：
         *
         * topic.addListener(ChatMessage.class, new MessageListener<ChatMessage>() {
         *     @Override
         *     public void onMessage(CharSequence channel, ChatMessage msg) {
         *         // 处理消息的逻辑
         *         log.info("收到Redis消息: {}", msg.getContent());
         *         boolean sent = webSocketHandler.sendMessageToUser(
         *             msg.getReceiverSessionId(),
         *             msg.getContent()
         *         );
         *         if (sent) {
         *             log.info("发送成功");
         *         } else {
         *             log.info("未找到用户");
         *         }
         *     }
         * });
         *
         * Lambda表达式 vs 匿名内部类：
         * - Lambda：更简洁，推荐使用（代码从3行减少到1行）
         * - 匿名内部类：更冗长，但可以包含更复杂的逻辑
         * - 对于简单的监听器，Lambda表达式是首选
         */
    }

    /**
     * 发布消息到Redis主题
     *
     * 使用场景：
     * 当用户A需要向用户B发送点对点消息时：
     * 1. 前端调用HTTP接口：POST /api/ws/send-to-user
     * 2. AdminController接收请求
     * 3. 调用此方法publishMessage()
     * 4. 消息被发布到Redis主题
     * 5. 所有服务器收到消息，尝试在本地发送
     * 6. 目标用户所在的服务器成功发送消息
     *
     * @param senderSessionId 发送者会话ID（当前WebSocket连接的session ID）
     * @param receiverSessionId 接收者会话ID（目标用户的session ID）
     * @param content 消息内容（要发送的文本）
     */
    public void publishMessage(String senderSessionId, String receiverSessionId, String content) {
        // 创建消息对象
        ChatMessage chatMessage = new ChatMessage(senderSessionId, receiverSessionId, content);

        /*
         * 获取主题对象并发布消息
         *
         * 注意：这里重新获取了一次topic对象
         * 为什么不缓存topic对象？
         * - Redisson的RTopic对象是轻量级的
         * - 每次获取开销很小
         * - 避免并发问题（虽然在这个场景下不太可能）
         */
        RTopic topic = redissonClient.getTopic(TOPIC_NAME, new TypedJsonJacksonCodec(ChatMessage.class));

        /*
         * 发布消息到Redis
         *
         * publish()方法：
         * - 参数：要发布的消息对象（ChatMessage）
         * - 返回值：订阅者数量（int）
         * - 同步方法：会阻塞直到消息发布完成
         *
         * 底层流程：
         * 1. TypedJsonJacksonCodec将ChatMessage序列化为JSON字符串
         * 2. 将JSON字符串发送到Redis服务器
         * 3. Redis将消息转发给所有订阅了MY_MESSAGE_TOPIC的客户端
         * 4. 每台服务器的监听器都会收到消息
         *
         * 关于数据存储：
         * ⚠️ Redis Pub/Sub是即发即弃模式，不会在Redis中存储任何数据结构！
         * - 消息发布后立即传递给在线订阅者
         * - 没有订阅者在线，消息直接丢弃
         * - 不会保存消息历史
         * - 服务器重启后，所有未投递的消息都会丢失
         * - 使用KEYS *命令在Redis中看不到任何相关数据
         *
         * 如果需要持久化消息队列，应该使用Redis Stream而不是Pub/Sub
         */
        topic.publish(chatMessage);

        log.info("已发布消息到Redis主题 - 发送者: {}, 接收者: {}, 内容: {}",
                senderSessionId, receiverSessionId, content);
    }
}
