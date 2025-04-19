package com.example.javathreaddemo;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Semaphore 使用示例 Demo:
 * 控制同时访问某个资源的线程数量。
 */
public class SemaphoreDemo {

    public static void main(String[] args) throws InterruptedException {
        // 假设只有 3 个可用资源 (许可)
        final int AVAILABLE_PERMITS = 3;
        // 创建一个 Semaphore，初始许可为 3 (非公平模式)
        final Semaphore semaphore = new Semaphore(AVAILABLE_PERMITS);

        // 模拟需要使用资源的线程数量
        final int THREAD_COUNT = 10;

        System.out.printf("资源池容量：%d, 共有 %d 个线程尝试使用资源。\n", AVAILABLE_PERMITS, THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        Random random = new Random();

        // 提交 10 个任务
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i + 1;
            executor.submit(() -> {
                try {
                    System.out.printf("线程 %d：尝试获取许可...\n", threadId);
                    // **关键：获取一个许可，如果无可用许可则阻塞**
                    semaphore.acquire();

                    // --- 获取到许可后，可以访问受限资源 ---
                    try {
                        System.out.printf("线程 %d：**获取许可成功**，开始使用资源... (当前可用许可: %d)\n",
                                threadId, semaphore.availablePermits());
                        // 模拟使用资源耗时 (1-3秒)
                        Thread.sleep(random.nextInt(2001) + 1000);
                        System.out.printf("线程 %d：资源使用完毕。\n", threadId);
                    } finally {
                        // **关键：使用完毕后，必须释放许可！**
                        System.out.printf("线程 %d：准备释放许可... (当前可用许可: %d -> %d)\n",
                                threadId, semaphore.availablePermits(), semaphore.availablePermits() + 1);
                        semaphore.release();
                    }
                    // --- 资源访问结束 ---

                } catch (InterruptedException e) {
                    System.err.printf("线程 %d：在获取或使用资源时被中断！\n", threadId);
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 关闭线程池，让任务执行完毕
        executor.shutdown();
        // 等待所有任务完成（设置一个较长的超时时间）
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            System.err.println("任务在60秒内未能全部完成！");
            executor.shutdownNow();
        }

        System.out.println("\n所有线程执行尝试完毕。");
        // 理论上，结束后可用许可应恢复为初始值
        System.out.printf("最终可用许可数量：%d\n", semaphore.availablePermits());
    }
}