package cn.clazs.websocket.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket处理器
 * 处理WebSocket连接的建立、消息接收、发送和关闭
 */
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
        System.out.println("WebSocket连接建立: " + session.getId());
        System.out.println("当前连接数: " + sessionSet.size());

        // 给新连接的客户端发送欢迎消息
        session.sendMessage(new TextMessage("欢迎连接到WebSocket服务器！你的ID: " + session.getId()));
    }

    /**
     * 接收到客户端消息时调用
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        System.out.println("收到来自 [" + session.getId() + "] 的消息: " + payload);

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
        System.err.println("WebSocket传输错误 [" + session.getId() + "]: " + exception.getMessage());
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
        System.out.println("WebSocket连接关闭: " + session.getId() + ", 状态: " + closeStatus);
        System.out.println("剩余连接数: " + sessionSet.size());
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
                    System.out.println("发送消息给 [" + session.getId() + "]: " + message);
                }
            } catch (IOException e) {
                System.err.println("发送消息失败 [" + session.getId() + "]: " + e.getMessage());
            }
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return sessionSet.size();
    }
}
