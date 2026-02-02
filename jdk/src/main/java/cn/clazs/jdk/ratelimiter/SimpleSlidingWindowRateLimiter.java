package cn.clazs.jdk.ratelimiter;

/**
 * 标准滑动窗口限流器（单机极简版）
 * 核心：子窗口计数器数组 + 窗口滑动逻辑
 * 示例：总窗口1秒（1000ms），拆分为10个100ms的子窗口
 */
public class SimpleSlidingWindowRateLimiter {
    // 核心数据结构：子窗口计数器数组（存储每个子窗口的请求数）
    private final int[] windowCounters;
    // 子窗口的时长（毫秒），比如100ms
    private final long subWindowMs;
    // 总窗口时长（毫秒），比如1000ms
    private final long totalWindowMs;
    // 限流阈值：总窗口内允许的最大请求数
    private final int limit;
    // 子窗口的数量 = 总窗口时长 / 子窗口时长
    private final int subWindowCount;
    // 上一次记录请求的时间戳（用于判断是否需要滑动窗口）
    private long lastRequestTime;

    /**
     * 构造方法
     * @param limit 总窗口内的限流阈值（比如10次/秒）
     * @param totalWindowMs 总窗口时长（比如1000ms）
     * @param subWindowCount 子窗口数量（比如10个）
     */
    public SimpleSlidingWindowRateLimiter(int limit, long totalWindowMs, int subWindowCount) {
        this.limit = limit;
        this.totalWindowMs = totalWindowMs;
        this.subWindowCount = subWindowCount;
        // 计算每个子窗口的时长
        this.subWindowMs = totalWindowMs / subWindowCount;
        // 初始化子窗口计数器数组（长度=子窗口数量，初始值全0）
        this.windowCounters = new int[subWindowCount];
        // 初始化最后请求时间为当前时间
        this.lastRequestTime = System.currentTimeMillis();
    }

    /**
     * 尝试获取限流许可
     * @return true=允许请求，false=限流
     */
    public boolean tryAcquire() {
        long now = System.currentTimeMillis();
        // 第一步：滑动窗口（核心）——清理过期的子窗口，重置计数器
        slideWindow(now);

        // 第二步：累加所有有效子窗口的计数器，判断是否超限
        int totalCount = 0;
        for (int count : windowCounters) {
            totalCount += count;
        }
        if (totalCount < limit) {
            // 第三步：找到当前请求所属的子窗口，计数器+1
            int currentSubWindowIndex = getCurrentSubWindowIndex(now);
            windowCounters[currentSubWindowIndex]++;
            lastRequestTime = now;
            return true;
        }
        return false;
    }

    /**
     * 核心：滑动窗口，清理过期的子窗口
     */
    private void slideWindow(long now) {
        // 计算从上次请求到现在，过去了多少毫秒
        long timeElapsed = now - lastRequestTime;
        // 如果过去的时间 < 子窗口时长，无需滑动（还在同一个/相邻子窗口）
        if (timeElapsed < subWindowMs) {
            return;
        }

        // 计算需要滑动多少个子窗口
        int slideCount = (int) (timeElapsed / subWindowMs);
        // 最多滑动到总窗口数量（避免滑过所有子窗口）
        slideCount = Math.min(slideCount, subWindowCount);

        // 滑动逻辑：把过期的子窗口计数器重置为0
        for (int i = 0; i < slideCount; i++) {
            // 环形更新子窗口索引（和环形数组索引逻辑类似）
            int expiredIndex = (getCurrentSubWindowIndex(lastRequestTime) + i + 1) % subWindowCount;
            windowCounters[expiredIndex] = 0;
        }
    }

    /**
     * 计算当前时间所属的子窗口索引
     */
    private int getCurrentSubWindowIndex(long time) {
        // 计算从"时间起点"到当前时间的总子窗口数，取模得到索引
        long timeSinceStart = time - (time / totalWindowMs) * totalWindowMs;
        return (int) (timeSinceStart / subWindowMs) % subWindowCount;
    }

    // 测试方法
    public static void main(String[] args) throws InterruptedException {
        // 初始化：1秒（1000ms）内最多5次请求，拆分为10个100ms的子窗口
        SimpleSlidingWindowRateLimiter limiter = new SimpleSlidingWindowRateLimiter(5, 1000, 10);
        
        // 模拟6次连续请求
        for (int i = 1; i <= 6; i++) {
            boolean acquired = limiter.tryAcquire();
            System.out.println("第" + i + "次请求：" + (acquired ? "成功" : "被限流"));
        }
        
        // 等待500ms（滑过5个子窗口），再次请求
        Thread.sleep(500);
        boolean acquired = limiter.tryAcquire();
        System.out.println("等待500ms后请求：" + (acquired ? "成功" : "被限流"));
    }
}