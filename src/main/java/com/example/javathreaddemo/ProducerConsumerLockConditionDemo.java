package com.example.javathreaddemo;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 使用 ReentrantLock 和 Condition 实现的生产者-消费者模式完整 Demo
 */
public class ProducerConsumerLockConditionDemo {

    /**
     * 共享的阻塞缓冲区 (使用 Lock 和 Condition)
     * @param <T> 缓冲区中存储的数据类型
     */
    static class BlockingBuffer<T> {
        private final Queue<T> buffer;
        private final int capacity;
        private final Lock lock = new ReentrantLock(); // 使用 ReentrantLock
        // 条件变量：缓冲区非满 (供生产者等待)
        private final Condition notFull = lock.newCondition();
        // 条件变量：缓冲区非空 (供消费者等待)
        private final Condition notEmpty = lock.newCondition();

        public BlockingBuffer(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be positive");
            }
            this.buffer = new LinkedList<>();
            this.capacity = capacity;
            System.out.println("缓冲区(Lock/Condition)创建，容量为: " + capacity);
        }

        /**
         * 生产者放入数据
         */
        public void put(T item) throws InterruptedException {
            lock.lock(); // 获取锁
            try {
                // **必须用 while 循环检查条件**
                while (buffer.size() == capacity) {
                    System.out.println("缓冲区已满! 生产者 " + Thread.currentThread().getName() + " 在 notFull 条件上等待...");
                    notFull.await(); // 缓冲区满，在 notFull 上等待 (释放 lock)
                    System.out.println("生产者 " + Thread.currentThread().getName() + " 从 notFull 等待中唤醒，重新检查容量...");
                }
                buffer.offer(item);
                System.out.printf("生产者 %s 生产了: %s (当前容量: %d)\n", Thread.currentThread().getName(), item, buffer.size());

                // **唤醒等待 notEmpty 的线程 (消费者)**
                // System.out.println("生产者 " + Thread.currentThread().getName() + " 发出 notEmpty 信号...");
                notEmpty.signal(); // 只唤醒一个等待的消费者就够了（也可以用 signalAll）
            } finally {
                lock.unlock(); // **必须在 finally 中释放锁**
            }
        }

        /**
         * 消费者取出数据
         */
        public T take() throws InterruptedException {
            lock.lock(); // 获取锁
            try {
                // **必须用 while 循环检查条件**
                while (buffer.isEmpty()) {
                    System.out.println("缓冲区为空! 消费者 " + Thread.currentThread().getName() + " 在 notEmpty 条件上等待...");
                    notEmpty.await(); // 缓冲区空，在 notEmpty 上等待 (释放 lock)
                     System.out.println("消费者 " + Thread.currentThread().getName() + " 从 notEmpty 等待中唤醒，重新检查是否有数据...");
                }
                T item = buffer.poll();
                System.out.printf("消费者 %s 消费了: %s (当前容量: %d)\n", Thread.currentThread().getName(), item, buffer.size());

                // **唤醒等待 notFull 的线程 (生产者)**
                // System.out.println("消费者 " + Thread.currentThread().getName() + " 发出 notFull 信号...");
                notFull.signal(); // 只唤醒一个等待的生产者（也可以用 signalAll）
                return item;
            } finally {
                lock.unlock(); // **必须在 finally 中释放锁**
            }
        }
    }

    // --- Main 方法，启动生产者和消费者 ---
    public static void main(String[] args) throws InterruptedException {
        // 创建容量为 3 的缓冲区
        BlockingBuffer<Integer> buffer = new BlockingBuffer<>(3);

        // 生产者任务: 持续生产 0, 1, 2, ...
        Runnable producerTask = () -> {
            try {
                int item = 0;
                while (!Thread.currentThread().isInterrupted()) { // 检查中断标志来停止
                    buffer.put(item++);
                    // 模拟生产耗时
                    Thread.sleep((long) (Math.random() * 50 + 50)); // 50-100ms
                }
            } catch (InterruptedException e) {
                System.out.println("生产者 " + Thread.currentThread().getName() + " 被中断，停止。");
                Thread.currentThread().interrupt(); // 重新设置中断状态
            }
        };

        // 消费者任务: 持续消费
        Runnable consumerTask = () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Integer item = buffer.take();
                    // 模拟消费耗时
                    Thread.sleep((long) (Math.random() * 150 + 100)); // 100-250ms
                }
            } catch (InterruptedException e) {
                System.out.println("消费者 " + Thread.currentThread().getName() + " 被中断，停止。");
                Thread.currentThread().interrupt(); // 重新设置中断状态
            }
        };

        // 创建线程
        Thread p1 = new Thread(producerTask, "生产者-1");
        Thread p2 = new Thread(producerTask, "生产者-2");
        Thread c1 = new Thread(consumerTask, "消费者-A");
        Thread c2 = new Thread(consumerTask, "消费者-B");

        System.out.println("启动生产者和消费者线程...");
        p1.start();
        p2.start();
        c1.start();
        c2.start();

        // 让它们运行 5 秒
        Thread.sleep(5000);

        // 中断所有线程来结束程序
        System.out.println("\n主线程：5秒结束，准备中断所有工作线程...\n");
        p1.interrupt();
        p2.interrupt();
        c1.interrupt();
        c2.interrupt();

        System.out.println("主线程结束。");
    }
}