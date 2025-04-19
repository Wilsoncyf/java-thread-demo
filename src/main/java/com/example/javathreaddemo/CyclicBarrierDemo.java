package com.example.javathreaddemo;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CyclicBarrier 使用示例 Demo:
 * 模拟多个线程分阶段执行任务，并在每个阶段结束时同步。
 */
public class CyclicBarrierDemo {

    public static void main(String[] args) {
        // 定义参与方数量 (士兵数量)
        final int SOLDIER_COUNT = 3;

        // 创建一个 CyclicBarrier，需要 3 个线程到达
        // 并指定一个“栅栏动作”，在栅栏被破开时执行 (由最后一个到达的线程执行)
        final CyclicBarrier barrier = new CyclicBarrier(SOLDIER_COUNT, () -> {
            // 这个 Runnable 会在 3 个士兵都到达栅栏时执行一次
            System.out.println("\n====================================");
            System.out.println("报告指挥官！所有 " + SOLDIER_COUNT + " 名士兵已在栅栏集合完毕！准备执行下一阶段任务！");
            System.out.println("====================================\n");
        });

        System.out.println("演习开始：派遣 " + SOLDIER_COUNT + " 名士兵执行任务...");

        ExecutorService executor = Executors.newFixedThreadPool(SOLDIER_COUNT);
        Random random = new Random();

        // 派遣士兵执行任务
        for (int i = 0; i < SOLDIER_COUNT; i++) {
            final int soldierId = i + 1;
            executor.submit(() -> {
                try {
                    // --- 阶段 1 ---
                    System.out.printf("士兵 %d：开始执行【阶段 1】任务...\n", soldierId);
                    Thread.sleep(random.nextInt(2001) + 1000); // 模拟耗时 1-3 秒
                    System.out.printf("士兵 %d：【阶段 1】任务完成，向栅栏移动...\n", soldierId);

                    // **关键：到达第一个栅栏点并等待其他士兵**
                    int arrivalIndex1 = barrier.await(); // 等待其他线程，返回到达序号(从N-1到0)
                    System.out.printf("士兵 %d：通过第一个栅栏 (到达序号 %d)。\n", soldierId, arrivalIndex1);


                    // --- 阶段 2 ---
                    // 所有线程会几乎同时从上面的 await() 返回，开始执行阶段 2
                    System.out.printf("士兵 %d：开始执行【阶段 2】任务...\n", soldierId);
                    Thread.sleep(random.nextInt(1501) + 500); // 模拟耗时 0.5-2 秒
                    System.out.printf("士兵 %d：【阶段 2】任务完成，向最终集合点移动...\n", soldierId);

                    // **关键：到达第二个栅栏点并等待其他士兵 (演示循环重用)**
                    // 注意：这次没有栅栏动作了，因为初始化时只指定了一个
                    int arrivalIndex2 = barrier.await();
                    System.out.printf("士兵 %d：通过第二个栅栏，任务全部完成！(到达序号 %d)。\n", soldierId, arrivalIndex2);

                } catch (InterruptedException e) {
                    System.err.printf("士兵 %d：任务被中断！\n", soldierId);
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    // 如果有线程在等待时被中断、超时或 reset，其他等待的线程会抛出此异常
                    System.err.printf("士兵 %d：发现栅栏已被破坏！无法继续任务。\n", soldierId);
                }
            });
        }

        // 关闭线程池（不再提交新任务）
        executor.shutdown();

        // 等待线程池执行完毕（可选，便于观察完整输出）
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) { // 等待足够长的时间
                System.err.println("演习超时，强制结束！");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("\n所有士兵任务完成，演习结束。");
    }
}