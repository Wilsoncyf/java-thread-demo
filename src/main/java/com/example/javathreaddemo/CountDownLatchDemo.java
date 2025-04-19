package com.example.javathreaddemo;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch 使用示例 Demo:
 * 主线程等待多个工作线程完成任务。
 */
public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        // 定义需要等待的任务数量
        final int TASK_COUNT = 3;
        // 创建一个 CountDownLatch，计数器初始化为 3
        final CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        System.out.println("主线程：启动工作线程...");

        // 创建一个线程池来执行任务
        ExecutorService executor = Executors.newFixedThreadPool(TASK_COUNT);
        Random random = new Random(); // 用于模拟随机耗时

        // 提交 3 个工作任务
        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskId = i + 1;
            executor.submit(() -> {
                try {
                    System.out.printf("工作线程 %d：开始执行任务...\n", taskId);
                    // 模拟任务耗时 (1-3秒)
                    int sleepTime = random.nextInt(2001) + 1000;
                    Thread.sleep(sleepTime);
                    System.out.printf("工作线程 %d：任务完成！(耗时 %d ms)\n", taskId, sleepTime);
                } catch (InterruptedException e) {
                    System.err.printf("工作线程 %d：被中断！\n", taskId);
                    Thread.currentThread().interrupt(); // 重置中断状态
                } finally {
                    // **关键：任务完成（无论成功还是异常），计数器减 1**
                    System.out.printf("工作线程 %d：调用 countDown(). 当前计数: %d -> %d\n",
                            taskId, latch.getCount(), latch.getCount() - 1);
                    latch.countDown();
                }
            });
        }

        System.out.println("主线程：所有任务已提交，等待所有工作线程完成 (调用 await())...");

        // **关键：主线程调用 await() 等待计数器归零**
        // 这会阻塞主线程，直到 latch.countDown() 被调用 3 次
        boolean completedInTime = latch.await(10, TimeUnit.SECONDS); // 可以设置超时等待

        if (completedInTime) {
             System.out.println("\n主线程：所有工作线程已完成！门闩已打开，主线程继续执行...");
        } else {
             System.out.println("\n主线程：等待超时！不是所有工作线程都在10秒内完成。");
        }

        // 关闭线程池
        System.out.println("主线程：关闭线程池...");
        executor.shutdown();
        try {
            // 等待最多 5 秒让现有任务结束
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("主线程：线程池任务未完全终止，强制关闭！");
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("主线程：执行完毕。");
    }
}