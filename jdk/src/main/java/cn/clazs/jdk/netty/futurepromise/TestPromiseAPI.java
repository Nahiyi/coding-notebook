package cn.clazs.jdk.netty.futurepromise;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 演示Promise的API
 */
@Slf4j
public class TestPromiseAPI {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        EventLoop eventLoop = new NioEventLoopGroup().next();

        // 区别1：promise不是submit的返回值而是手动创建出来，作为结果的容器（Promise接口是Future的子接口）
        // 提前创建Promise对象，在子线程中手动setSuccess或者setFailure，而不是靠事件循环线程池提交任务返回Promise对象
        DefaultPromise<String> promise = new DefaultPromise<>(eventLoop);

        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                log.debug("异步任务计算...");
                int i = 1 / 0; // int i = 1 / 1;
                promise.setSuccess("成功"); // 区别2：Promise相较于Future扩展的两个方法：setSuccess和setFailure
            } catch (InterruptedException e) {
                promise.setFailure(e);
            }
        }).start();

        log.debug("即将获取结果...");
        String result = promise.get();
        log.debug("结果: {}", result);
    }
}
