package com.example.javathreaddemo;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存秒杀模拟 Demo
 * 使用 AtomicInteger 控制库存，ThreadPoolExecutor 模拟并发请求
 */
public class InMemorySeckillDemo {

    /**
     * 库存服务，使用 AtomicInteger 保证扣减原子性
     */
    static class StockService {
        // 使用 AtomicInteger 表示库存
        private final AtomicInteger stock;
        // 统计成功扣减次数
        private final AtomicInteger successCount = new AtomicInteger(0);
        // 统计因库存不足失败次数
        private final AtomicInteger failCount = new AtomicInteger(0);

        public StockService(int initialStock) {
            this.stock = new AtomicInteger(initialStock);
            System.out.printf("初始化库存: %d\n", initialStock);
        }

        /**
         * 尝试扣减库存 (使用 CAS 循环)
         * @return true 如果扣减成功, false 如果库存不足
         */
        public boolean deductStock() {
            int currentStock;
            do {
                // 1. 读取当前库存值
                currentStock = stock.get();

                // 2. 检查库存是否大于 0
                if (currentStock <= 0) {
                    // System.out.println(Thread.currentThread().getName() + ": 库存不足，扣减失败！");
                    failCount.incrementAndGet(); // 统计失败次数
                    return false; // 库存不足，直接返回失败
                }

                // 3. 尝试原子性地将库存从 currentStock 减到 currentStock - 1
            } while (!stock.compareAndSet(currentStock, currentStock - 1));

            // CAS 成功，库存扣减成功
            // System.out.println(Thread.currentThread().getName() + ": 库存扣减成功！剩余库存: " + (currentStock - 1));
            successCount.incrementAndGet(); // 统计成功次数
            return true;
        }

        public int getStockCount() {
            return stock.get();
        }

        public int getSuccessCount() {
            return successCount.get();
        }

        public int getFailCount() {
            return failCount.get();
        }
    }

    // --- Main 方法 ---
    public static void main(String[] args) throws InterruptedException {
        // --- 配置参数 ---
        final int INITIAL_STOCK = 100;    // 初始库存
        final int REQUEST_COUNT = 1000;   // 模拟并发请求数
        final int CORE_POOL_SIZE = 10;    // 核心线程数
        final int MAX_POOL_SIZE = 20;     // 最大线程数 (允许临时扩展)
        final long KEEP_ALIVE_TIME = 60L; // 非核心线程空闲存活时间
        final int QUEUE_CAPACITY = 100;   // **使用有界队列**，容量 100

        // --- 初始化 ---
        StockService stockService = new StockService(INITIAL_STOCK);
        AtomicInteger rejectedCount = new AtomicInteger(0); // 统计被拒绝的任务数

        // --- 创建自定义线程池 ---
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        // 使用 AbortPolicy，方便观察拒绝情况；生产环境可能用其他策略
        RejectedExecutionHandler handler = (r, executor) -> {
            // 自定义拒绝逻辑：计数并打印
            // System.out.println("任务被拒绝: " + r.toString());
            rejectedCount.incrementAndGet();
        };
        // RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy(); // 或者用默认的

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                handler
        );

        System.out.printf("线程池配置: core=%d, max=%d, queueCapacity=%d\n",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        System.out.println("开始提交 %d 个秒杀请求...\n".formatted(REQUEST_COUNT));

        // 使用 CountDownLatch 等待所有任务尝试完成 (提交 + 执行/拒绝)
        // 注意：这里只保证任务被提交并尝试执行，不保证执行成功
        CountDownLatch allTasksSubmittedLatch = new CountDownLatch(REQUEST_COUNT);

        // --- 模拟并发请求 ---
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < REQUEST_COUNT; i++) {
            try {
                // 提交扣库存任务
                executor.submit(() -> {
                    stockService.deductStock(); // 调用扣库存方法
                    allTasksSubmittedLatch.countDown(); // 标记一个任务已处理（或被拒绝后计数）
                });
            } catch (RejectedExecutionException e) {
                // 如果使用 AbortPolicy 且未自定义 handler，这里会捕获异常
                // System.err.println("任务提交被拒绝!");
                rejectedCount.incrementAndGet(); // 统计拒绝次数
                allTasksSubmittedLatch.countDown(); // 被拒绝也算处理完一个提交尝试
            }
        }

        // --- 等待结果 ---
        System.out.println("\n所有任务已提交，等待执行完成 (或被拒绝)...");

        // 等待所有任务都尝试过（执行或被拒绝）
        // 设置一个超时时间防止无限等待
        if (!allTasksSubmittedLatch.await(30, TimeUnit.SECONDS)) {
             System.err.println("警告：并非所有任务都在30秒内完成处理！");
        }

        // 关闭线程池（不再接受新任务，等待现有任务执行完）
        executor.shutdown();
        // 等待线程池完全终止
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            System.err.println("警告：线程池在关闭后10秒内未能完全终止！");
            executor.shutdownNow(); // 强制关闭
        }

        long endTime = System.currentTimeMillis();

        // --- 打印结果 ---
        System.out.println("\n--- 秒杀模拟结果 ---");
        System.out.println("模拟请求总数: " + REQUEST_COUNT);
        System.out.println("初始库存: " + INITIAL_STOCK);
        System.out.println("最终剩余库存: " + stockService.getStockCount());
        System.out.println("成功扣减次数: " + stockService.getSuccessCount());
        System.out.println("因库存不足失败次数: " + stockService.getFailCount());
        System.out.println("因线程池饱和被拒绝次数: " + rejectedCount.get());
        System.out.println("总耗时: " + (endTime - startTime) + " ms");

        // --- 验证 ---
        int finalStock = stockService.getStockCount();
        int successDeductions = stockService.getSuccessCount();
        if (finalStock < 0) {
            System.err.println("错误：出现超卖！最终库存为负数！");
        } else if (finalStock >= 0 && successDeductions == INITIAL_STOCK - finalStock) {
            System.out.println("验证通过：未超卖，成功扣减数量与库存减少数量一致。");
        } else {
            System.err.printf("错误：数据不一致！初始库存 %d, 最终库存 %d, 成功扣减 %d\n",
                             INITIAL_STOCK, finalStock, successDeductions);
        }
         int processedOrRejected = successDeductions + stockService.getFailCount() + rejectedCount.get();
         System.out.printf("处理或拒绝的总数: %d (理论应接近 %d)\n", processedOrRejected, REQUEST_COUNT);


    }
}