package com.example.javathreaddemo;

import java.util.HashMap;
import java.util.Map;
import java.util.Random; // 确保导入 Random 类
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 使用 ReentrantReadWriteLock 实现线程安全的缓存 Demo (修正版)
 */
public class ReadWriteLockCacheDemo {

    static class ReadWriteCache<K, V> {
        private final Map<K, V> cacheMap = new HashMap<>();
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();
        // 这个 random 仅供 ReadWriteCache 内部的 sleep 使用
        private final Random cacheInternalRandom = new Random();

        public V get(K key) {
            readLock.lock();
            try {
                System.out.printf("线程 [%s] 获取了读取锁，准备读取 key: %s\n", Thread.currentThread().getName(), key);
                sleepRandomly(10, 50); // 内部调用随机休眠
                V value = cacheMap.get(key);
                System.out.printf("线程 [%s] 读取完成 key: %s, value: %s\n", Thread.currentThread().getName(), key, value);
                return value;
            } finally {
                System.out.printf("线程 [%s] 释放了读取锁\n", Thread.currentThread().getName());
                readLock.unlock();
            }
        }

        public void put(K key, V value) {
            writeLock.lock();
            try {
                System.out.printf(">>>> 线程 [%s] 获取了写入锁，准备写入 key: %s, value: %s\n", Thread.currentThread().getName(), key, value);
                sleepRandomly(50, 100); // 内部调用随机休眠
                cacheMap.put(key, value);
                System.out.printf(">>>> 线程 [%s] 写入完成 key: %s, value: %s\n", Thread.currentThread().getName(), key, value);
            } finally {
                System.out.printf("<<<< 线程 [%s] 释放了写入锁\n", Thread.currentThread().getName());
                writeLock.unlock();
            }
        }

        public void clear() {
            writeLock.lock();
            try {
                System.out.printf(">>>> 线程 [%s] 获取了写入锁，准备清空缓存\n", Thread.currentThread().getName());
                cacheMap.clear();
                System.out.printf(">>>> 线程 [%s] 缓存已清空\n", Thread.currentThread().getName());
            } finally {
                System.out.printf("<<<< 线程 [%s] 释放了写入锁\n", Thread.currentThread().getName());
                writeLock.unlock();
            }
        }

        // 提取一个内部方法用于随机休眠
        private void sleepRandomly(int minMillis, int boundMillis) {
            try {
                // 使用类成员 cacheInternalRandom
                Thread.sleep(cacheInternalRandom.nextInt(boundMillis) + minMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // --- Main 方法，启动读写线程 ---
    public static void main(String[] args) throws InterruptedException {
        ReadWriteCache<String, String> cache = new ReadWriteCache<>();
        // **修正：在 main 方法作用域内创建一个 Random 实例**
        final Random taskRandom = new Random();

        // 初始放入一些数据
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        ExecutorService executor = Executors.newFixedThreadPool(6);

        // 读者任务
        Runnable readerTask = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                cache.get("key1");
                try {
                    // **修正：使用 taskRandom**
                    Thread.sleep(taskRandom.nextInt(100) + 50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.printf("读者线程 [%s] 结束。\n", Thread.currentThread().getName());
        };

        // 写者任务
        Runnable writerTask = () -> {
            String[] keys = {"key1", "key2"};
            int counter = 0;
            while (!Thread.currentThread().isInterrupted()) {
                // **修正：使用 taskRandom**
                String key = keys[taskRandom.nextInt(keys.length)];
                String value = "newValue_" + Thread.currentThread().getName() + "_" + (counter++);
                cache.put(key, value);
                try {
                    // **修正：使用 taskRandom**
                    Thread.sleep(taskRandom.nextInt(300) + 200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.printf("写者线程 [%s] 结束。\n", Thread.currentThread().getName());
        };

        System.out.println("启动 4 个读者线程和 2 个写者线程...");
        executor.submit(readerTask);
        executor.submit(readerTask);
        executor.submit(readerTask);
        executor.submit(readerTask);
        executor.submit(writerTask);
        executor.submit(writerTask);

        Thread.sleep(10000); // 运行 10 秒

        System.out.println("\n主线程：10秒结束，准备关闭线程池并中断任务...");
        executor.shutdownNow();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            System.err.println("线程池没有在5秒内完全终止！");
        }
        System.out.println("主线程结束。");
    }
}