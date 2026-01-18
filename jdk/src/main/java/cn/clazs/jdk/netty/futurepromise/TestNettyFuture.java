package cn.clazs.jdk.netty.futurepromise;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 演示Netty的Future接口的不同的API
 */
@Slf4j
public class TestNettyFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup(); // 一个事件循环组相当于一个线程池
        EventLoop eventLoop = group.next(); // 可以认为相当于一个线程

        // 事件循环执行任务返回的future是基于JDK扩展的Future
        Future<Integer> future = eventLoop.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("异步任务运行中...");
                TimeUnit.SECONDS.sleep(1);
                return 2000;
            }
        });

        // 演示一
        /* log.debug("等待异步任务结果...");
        Integer result = future.get(); // 同步阻塞获取结果，该API和JDK的Future一样
        log.debug("结果: {}", result); */

        // 演示二
        /* log.debug("等待异步任务结果...");
        Integer r = null;
        while (r == null) {
            r = future.getNow(); // 非阻塞获取结果，还无结果就返回null
            TimeUnit.MILLISECONDS.sleep(200);
            log.debug("结果: {}", r);
        } */

        // 演示三：通用思路，注册一个异步回调
        log.debug("等待异步任务结果...");
        future.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                log.debug("结果: {}", future.get()); // 此时已经成功了，调用哪个get均可
            }
        });


        log.debug("Done");
        eventLoop.shutdownGracefully();
        group.shutdownGracefully();
    }
}
