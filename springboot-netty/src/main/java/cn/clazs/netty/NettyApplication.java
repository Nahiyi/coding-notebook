package cn.clazs.netty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Spring Boot + Netty 集成示例应用
 *
 * 本模块展示Netty在Spring Boot中的常见使用场景：
 * 1. TCP服务器 - 基础的Socket通信
 * 2. WebSocket服务器 - 实时双向通信
 * 3. HTTP服务器 - 替代Tomcat的高性能HTTP服务
 *
 * @author clazs
 */
@SpringBootApplication
public class NettyApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext app = SpringApplication.run(NettyApplication.class, args);

        // 获取环境配置
        Environment env = app.getEnvironment();
        String port = env.getProperty("server.port", "9060");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        // 构建访问地址
        String baseUrl = "http://localhost:" + port + contextPath;

        System.out.println("\n========================================");
        System.out.println("Spring Boot + Netty 应用启动成功！");
        System.out.println("服务端口: " + port);
        System.out.println("访问地址: " + baseUrl);
        System.out.println("REST API: " + baseUrl + "/netty");
        System.out.println("========================================");
        System.out.println("\nNetty服务说明：");
        System.out.println("- TCP服务器: localhost:9001 (Echo服务)");
        System.out.println("- WebSocket服务器: ws://localhost:9002/ws");
        System.out.println("- HTTP服务器: localhost:9003");
        System.out.println("========================================\n");
    }
}
