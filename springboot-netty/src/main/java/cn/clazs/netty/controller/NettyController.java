package cn.clazs.netty.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Netty服务管理控制器
 *
 * 功能：提供REST API用于测试和管理Netty服务
 *
 * @author clazs
 */
@Slf4j
@RestController
@RequestMapping("/netty")
public class NettyController {

    @Value("${netty.tcp.port:9001}")
    private int tcpPort;

    @Value("${netty.websocket.port:9002}")
    private int websocketPort;

    @Value("${netty.websocket.path:/ws}")
    private String websocketPath;

    @Value("${netty.http.port:9003}")
    private int httpPort;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("application", "springboot-netty");
        result.put("description", "Spring Boot + Netty 集成示例");
        return result;
    }

    /**
     * 获取Netty服务信息
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new HashMap<>();
        result.put("application", "Spring Boot + Netty 集成示例");
        result.put("description", "展示Netty在Spring Boot中的常见使用场景");

        // TCP服务信息
        Map<String, Object> tcpInfo = new HashMap<>();
        tcpInfo.put("enabled", true);
        tcpInfo.put("port", tcpPort);
        tcpInfo.put("description", "TCP Echo服务器");
        tcpInfo.put("testCommand", "telnet localhost " + tcpPort);
        tcpInfo.put("useCase", "自定义协议、RPC框架、游戏服务器、物联网设备通信");
        result.put("tcpServer", tcpInfo);

        // WebSocket服务信息
        Map<String, Object> wsInfo = new HashMap<>();
        wsInfo.put("enabled", true);
        wsInfo.put("port", websocketPort);
        wsInfo.put("path", websocketPath);
        wsInfo.put("url", "ws://localhost:" + websocketPort + websocketPath);
        wsInfo.put("description", "WebSocket实时双向通信服务器");
        wsInfo.put("testCommand", "new WebSocket('ws://localhost:" + websocketPort + websocketPath + "')");
        wsInfo.put("useCase", "聊天室、即时通讯、实时数据推送、在线协作");
        result.put("webSocketServer", wsInfo);

        // HTTP服务信息
        Map<String, Object> httpInfo = new HashMap<>();
        httpInfo.put("enabled", true);
        httpInfo.put("port", httpPort);
        httpInfo.put("url", "http://localhost:" + httpPort);
        httpInfo.put("description", "HTTP服务器（可替代Tomcat）");
        httpInfo.put("testCommand", "curl http://localhost:" + httpPort);
        httpInfo.put("useCase", "高性能REST API、API网关、微服务网关");
        result.put("httpServer", httpInfo);

        return result;
    }

    /**
     * 获取测试指南
     */
    @GetMapping("/guide")
    public Map<String, Object> guide() {
        Map<String, Object> result = new HashMap<>();

        // TCP测试指南
        Map<String, String> tcpGuide = new HashMap<>();
        tcpGuide.put("title", "TCP服务器测试指南");
        tcpGuide.put("method1", "使用telnet: telnet localhost " + tcpPort);
        tcpGuide.put("method2", "使用nc: nc localhost " + tcpPort);
        tcpGuide.put("method3", "使用代码: Socket连接到 localhost:" + tcpPort);
        tcpGuide.put("description", "输入任意文本，服务器会原样返回（Echo服务）");
        result.put("tcpGuide", tcpGuide);

        // WebSocket测试指南
        Map<String, String> wsGuide = new HashMap<>();
        wsGuide.put("title", "WebSocket服务器测试指南");
        wsGuide.put("method1", "在线工具: http://www.websocket-test.com/");
        wsGuide.put("method2", "浏览器控制台: const ws = new WebSocket('ws://localhost:" + websocketPort + websocketPath + "')");
        wsGuide.put("method3", "Postman: 新建WebSocket请求连接到 ws://localhost:" + websocketPort + websocketPath);
        wsGuide.put("javascriptExample", "ws.send('Hello Netty!'); ws.onmessage = (msg) => console.log(msg.data);");
        result.put("webSocketGuide", wsGuide);

        // HTTP测试指南
        Map<String, String> httpGuide = new HashMap<>();
        httpGuide.put("title", "HTTP服务器测试指南");
        httpGuide.put("method1", "浏览器访问: http://localhost:" + httpPort);
        httpGuide.put("method2", "使用curl: curl http://localhost:" + httpPort);
        httpGuide.put("method3", "使用Postman: GET http://localhost:" + httpPort);
        httpGuide.put("description", "返回 'Hello World from Netty HTTP Server!'");
        result.put("httpGuide", httpGuide);

        return result;
    }
}
