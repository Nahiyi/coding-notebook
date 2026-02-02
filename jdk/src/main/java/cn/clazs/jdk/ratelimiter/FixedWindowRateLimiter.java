package cn.clazs.jdk.ratelimiter;

/**
 * 固定时间窗口下存在的临界突变问题演示
 */
public class FixedWindowRateLimiter {
    private final long windowSize; // 窗口大小（毫秒）
    private final long maxRequests; // 窗口最大请求数
    private long currentCount; // 当前窗口计数器
    private long windowStart; // 当前窗口开始时间

    public FixedWindowRateLimiter(long windowSize, long maxRequests) {
        this.windowSize = windowSize;
        this.maxRequests = maxRequests;
        this.windowStart = System.currentTimeMillis();
        this.currentCount = 0;
    }

    public boolean tryAcquire() {
        long now = System.currentTimeMillis();
        // 检查是否进入新窗口，是则重置计数器和窗口开始时间
        if (now - windowStart >= windowSize) {
            windowStart = now;
            currentCount = 0;
        }
        // 计数器未超则+1，否则限流
        if (currentCount < maxRequests) {
            currentCount++;
            return true;
        }
        return false;
    }

    // 测试临界突变
    public static void main(String[] args) throws InterruptedException {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1000, 100); // 1秒窗口，最多100请求

        // 窗口1的最后100ms：发100个请求
        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire();
        }
        System.out.println("窗口1结束时计数器：" + limiter.currentCount); // 100

        // 等待100ms，进入窗口2
        Thread.sleep(100);

        // 窗口2的前100ms：再发100个请求
        for (int i = 0; i < 100; i++) {
            limiter.tryAcquire();
        }
        System.out.println("窗口2开始后计数器：" + limiter.currentCount); // 100

        // 实际：200ms内发了200个请求，突破100/秒的限制
    }
}