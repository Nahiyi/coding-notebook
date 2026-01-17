package cn.clazs.jdk.netty.hello;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ScheduledTaskDemo {
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup(2); // 可处理IO事件、普通任务、定时任务
        // EventLoopGroup defaultGroup = new DefaultEventLoop(); // 处理普通任务、定时任务

        // demo1: 获取下一个事件循环对象（一共只有两个线程，所以只有两个事件循环对象）
        System.out.println(group.next());
        System.out.println(group.next());
        System.out.println(group.next());


        AtomicInteger i = new AtomicInteger(1);
        log.info("异步任务开始执行...");
        // demo2: 执行普通任务
        Runnable task = () -> {
            log.info("进入任务");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("一个Task ----- 序号: {}", i.getAndIncrement());
        };
        // 两个事件循环都可以做定时任务（且由于事件循环继承了：juc.ScheduledExecutorService，本质也是一个执行器）
        EventLoop eventLoop = group.next();
        eventLoop.submit(task); // submit，可以提交Callable拿到返回值

        eventLoop = group.next();
        eventLoop.execute(task);


        eventLoop = group.next();
        // demo3: 执行定时任务（以scheduleX开头一共 4 个方法）
        log.info("将执行定时任务, 开始定时...");
        eventLoop.schedule(task, 1, TimeUnit.SECONDS); // 延迟1秒后执行（只执行1次）
        // eventLoop.scheduleAtFixedRate(task, 1, 1, TimeUnit.SECONDS); // 初始延迟1秒后，每间隔1秒持续不断执行

        // Thread.sleep(2000);
        // group.shutdownGracefully();
    }
}
