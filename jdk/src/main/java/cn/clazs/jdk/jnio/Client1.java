package cn.clazs.jdk.jnio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;

@Slf4j
public class Client1 {
    public static void main(String[] args) {
        // 通过指定ip、端口建立连接
        try (Socket socket = new Socket("localhost", 7070)) {
            log.debug("{}", socket);

            // 尝试发送数据
            String data = "hello";
            log.debug("发送数据: {}", data);
            socket.getOutputStream().write(data.getBytes());

            // 模拟一个阻塞，来维持连接不关闭
            System.in.read();

            // close是正常断开连接
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}