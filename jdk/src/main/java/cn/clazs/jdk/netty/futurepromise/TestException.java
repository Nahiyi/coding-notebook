package cn.clazs.jdk.netty.futurepromise;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class TestException {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Integer> jdkFuture = pool.submit(() -> {
            log.debug("返回JDK的Future的任务");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 100 / 0;
        });

        log.debug("JDK的Future的返回结果：");
        try {
            Integer r1 = jdkFuture.get(); // 这里先catch异常，因为异常会导致程序崩溃，不会再向下运行
            log.debug("r1: {}", r1);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


        System.out.println("----------------------------------------------------------");


        EventLoop eventLoop = new NioEventLoopGroup().next();
        io.netty.util.concurrent.Future<Integer> nettyFuture = eventLoop.submit(() -> {
            log.debug("返回Netty的Future的任务");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 200 / 0;
        });

        io.netty.util.concurrent.Future<Integer> future = null;
        try {
            future = nettyFuture.sync(); // await方法类似，只不过未完成不抛异常，只是返回null
        } catch (Exception e) {
            log.error("获取过程发生异常");
            e.printStackTrace();
        }

        log.debug("成功情况: [{}]", future != null && future.isSuccess());

        try {
            log.debug("get获取结果：");
            Integer r2 = future.get();
            log.debug("r2: {}", r2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            log.debug("getNow获取结果：");
            Integer r2_ = future.getNow();
            log.debug("r2: {}", r2_);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
