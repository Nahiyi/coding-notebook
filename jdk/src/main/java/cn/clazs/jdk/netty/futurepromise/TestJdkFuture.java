package cn.clazs.jdk.netty.futurepromise;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * JDK的Future接口API演示
 */
@Slf4j
public class TestJdkFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Integer> future = pool.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                log.debug("异步任务执行中...");
                TimeUnit.SECONDS.sleep(1);
                return 100;
            }
        });

        log.debug("正在获取异步任务的结果...");
        Integer result = future.get(); // get阻塞式获取结果
        log.debug("获取到结果: {}", result);

        pool.shutdown();
    }
}
