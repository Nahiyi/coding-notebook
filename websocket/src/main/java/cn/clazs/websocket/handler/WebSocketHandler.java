package cn.clazs.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket处理器
 * 处理WebSocket连接的建立、消息接收、发送和关闭
 */
@Slf4j
@Component
public class WebSocketHandler implements org.springframework.web.socket.WebSocketHandler {

    // 存储所有连接的WebSocket会话
    private static final CopyOnWriteArraySet<WebSocketSession> sessionSet = new CopyOnWriteArraySet<>();

    /**
     * 连接建立后调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionSet.add(session);
        log.info("WebSocket连接建立: {}", session.getId());
        log.info("当前连接数: {}", sessionSet.size());

        // 给新连接的客户端发送欢迎消息
        session.sendMessage(new TextMessage("欢迎连接到WebSocket服务器！你的ID: " + session.getId()));
    }

    /**
     * 接收到客户端消息时调用
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        log.info("收到来自 [{}] 的消息: {}", session.getId(), payload);

        // 处理客户端消息
        String response = "服务器回复: " + payload;

        // 广播消息给所有连接的客户端
        broadcastMessage(response);
    }

    /**
     * 传输错误时调用
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误 [{}]: {}", session.getId(), exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
        sessionSet.remove(session);
    }

    /**
     * 连接关闭后调用
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sessionSet.remove(session);
        log.info("WebSocket连接关闭: {}, 状态: {}", session.getId(), closeStatus);
        log.info("剩余连接数: {}", sessionSet.size());
    }

    /**
     * 是否支持部分消息
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 广播消息给所有连接的客户端（公共方法）
     */
    public void broadcastMessage(String message) {
        for (WebSocketSession session : sessionSet) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    log.info("发送消息给 [{}]: {}", session.getId(), message);
                }
            } catch (IOException e) {
                log.error("发送消息失败 [{}]: {}", session.getId(), e.getMessage());
            }
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessionSet.size();
    }

    /**
     * 获取所有会话ID列表
     *
     * @return 所有在线用户的session ID集合
     */
    public Set<String> getAllSessionIds() {
        Set<String> sessionIds = new HashSet<>();
        for (WebSocketSession session : sessionSet) {
            sessionIds.add(session.getId());
        }
        return sessionIds;
    }

    /**
     * 发送消息到指定用户（通过sessionId）
     *
     * @param receiverSessionId 接收者会话ID
     * @param message 消息内容
     * @return 是否发送成功（true=找到用户并发送成功，false=未找到用户）
     */
    public boolean sendMessageToUser(String receiverSessionId, String message) {
        for (WebSocketSession session : sessionSet) {
            if (session.getId().equals(receiverSessionId)) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                        log.info("成功发送消息给用户 [{}]: {}", receiverSessionId, message);
                        return true;
                    } else {
                        log.error("用户 [{}] 的会话已关闭", receiverSessionId);
                        return false;
                    }
                } catch (IOException e) {
                    log.error("发送消息给用户 [{}] 失败: {}", receiverSessionId, e.getMessage());
                    return false;
                }
            }
        }
        log.info("未找到目标用户 [{}]，该用户不在此服务器上", receiverSessionId);
        return false;
    }
}
