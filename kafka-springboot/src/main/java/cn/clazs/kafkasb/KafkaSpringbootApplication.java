package cn.clazs.kafkasb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Kafka Spring Boot 应用程序主类
 */
@SpringBootApplication
public class KafkaSpringbootApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext app = SpringApplication.run(KafkaSpringbootApplication.class, args);
        System.out.println("========================================");
        System.out.println("Kafka Spring Boot 应用启动成功！");
        String port = app.getEnvironment().getProperty("server.port", "8080");
        System.out.printf("访问 http://localhost:%s/kafka/send?message=测试消息 发送消息\n", port);
        System.out.println("========================================");
    }
}
