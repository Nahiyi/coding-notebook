package cn.clazs.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * SpringBoot WebSocket应用启动类
 */
@SpringBootApplication
public class WebSocketApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext app = SpringApplication.run(WebSocketApplication.class, args);

        // 获取环境配置
        Environment env = app.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        // 构建访问地址
        String baseUrl = "http://localhost:" + port + contextPath;
        String wsUrl = "ws://localhost:" + port + contextPath + "/ws";

        System.out.println("\n========================================");
        System.out.println("WebSocket服务器启动成功！");
        System.out.println("服务端口: " + port);
        System.out.println("上下文路径: " + (contextPath.isEmpty() ? "/" : contextPath));
        System.out.println("访问地址: " + baseUrl);
        System.out.println("WebSocket地址: " + wsUrl);
        System.out.println("========================================\n");
    }
}
