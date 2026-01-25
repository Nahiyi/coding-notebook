package cn.clazs.websocket.controller;

import cn.clazs.websocket.handler.MessageHandler;
import cn.clazs.websocket.handler.WebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket REST控制器
 * 提供HTTP接口用于服务端主动推送消息
 */
@Slf4j
@RestController
@RequestMapping("/api/ws")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private MessageHandler messageHandler;

    /**
     * 广播消息给所有连接的客户端
     */
    @PostMapping("/broadcast")
    public Map<String, Object> broadcastMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "消息内容不能为空");
            return response;
        }

        webSocketHandler.broadcastMessage(message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "广播消息成功");
        response.put("connectionCount", webSocketHandler.getConnectionCount());
        return response;
    }

    /**
     * 获取当前连接数
     */
    @GetMapping("/connections")
    public Map<String, Object> getConnectionCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("connectionCount", webSocketHandler.getConnectionCount());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 发送系统通知
     */
    @PostMapping("/notify")
    public Map<String, Object> sendNotification(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");

        String message = "【系统通知】" + (title != null ? title : "") +
                        " - " + (content != null ? content : "");

        webSocketHandler.broadcastMessage(message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "系统通知已发送");
        return response;
    }

    /**
     * 获取所有在线用户的session ID列表
     */
    @GetMapping("/sessions")
    public Map<String, Object> getAllSessions() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessions", webSocketHandler.getAllSessionIds());
        response.put("count", webSocketHandler.getConnectionCount());
        log.info("获取所有在线用户列表，当前连接数: {}", webSocketHandler.getConnectionCount());
        return response;
    }

    /**
     * 发送点对点消息（跨服务器支持）
     * 通过消息代理（Redis/RabbitMQ）发布，所有服务器都会尝试发送，只有目标用户所在的服务器会成功
     */
    @PostMapping("/send-to-user")
    public Map<String, Object> sendToUser(@RequestBody Map<String, String> request) {
        String senderSessionId = request.get("senderSessionId");
        String receiverSessionId = request.get("receiverSessionId");
        String message = request.get("message");

        // 参数校验
        if (senderSessionId == null || senderSessionId.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送者会话ID不能为空");
            return response;
        }

        if (receiverSessionId == null || receiverSessionId.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "接收者会话ID不能为空");
            return response;
        }

        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "消息内容不能为空");
            return response;
        }

        // 发布消息到消息代理（Redis或RabbitMQ，由配置决定）
        messageHandler.publish(senderSessionId, receiverSessionId, message);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "消息已发布到" + messageHandler.getType() + "，正在转发给目标用户");
        response.put("senderSessionId", senderSessionId);
        response.put("receiverSessionId", receiverSessionId);
        response.put("brokerType", messageHandler.getType());
        log.info("发送点对点消息 - 发送者: {}, 接收者: {}, 内容: {}, 消息代理: {}",
                senderSessionId, receiverSessionId, message, messageHandler.getType());
        return response;
    }
}
