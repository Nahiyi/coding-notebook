package cn.clazs.websocket.controller;

import cn.clazs.websocket.handler.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket REST控制器
 * 提供HTTP接口用于服务端主动推送消息
 */
@RestController
@RequestMapping("/api/ws")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private WebSocketHandler webSocketHandler;

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
}
