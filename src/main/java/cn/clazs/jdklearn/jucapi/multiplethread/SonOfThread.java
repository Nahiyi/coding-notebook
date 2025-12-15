package cn.clazs.jdklearn.jucapi.multiplethread;

import java.time.LocalDateTime;

public class SonOfThread extends Thread {

    // 必须覆盖父类run方法，无需再依赖Runnable
    @Override
    public void run() {
        int cnt = 100;
        while (cnt >= 0) {
            cnt--;
            LocalDateTime now = LocalDateTime.now();
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("now = " + now.toString().replace("T", " "));
        }
    }
}
