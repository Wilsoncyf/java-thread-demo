package com.example.javathreaddemo;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 使用 wait(), notifyAll(), synchronized 实现的生产者-消费者模式 Demo
 */
public class ProducerConsumerWaitNotify {

    /**
     * 共享的阻塞缓冲区
     *
     * @param <T> 缓冲区中存储的数据类型
     */
    static class BlockingBuffer<T> {
        private final Queue<T> buffer; // 底层使用 LinkedList 作为队列
        private final int capacity;    // 缓冲区容量

        // 构造函数，初始化缓冲区和容量
        public BlockingBuffer(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be positive");
            }
            this.buffer = new LinkedList<>();
            this.capacity = capacity;
            System.out.println("缓冲区创建，容量为: " + capacity);
        }

        /**
         * 生产者调用的方法，向缓冲区放入数据
         *
         * @param item 要放入的数据
         * @throws InterruptedException 如果线程在等待时被中断
         */
        public void put(T item) throws InterruptedException {
            // 使用缓冲区对象自身作为锁
            synchronized (buffer) {
                // **关键点1: 必须使用 while 循环检查条件**
                // 防止“虚假唤醒”(Spurious Wakeup)，即使被唤醒也要再次检查条件是否满足
                while (buffer.size() == capacity) {
                    System.out.println("缓冲区已满! 生产者 " + Thread.currentThread().getName() + " 进入等待...");
                    // 缓冲区满了，生产者调用 wait() 等待，并释放 buffer 锁
                    buffer.wait();
                    System.out.println("生产者 " + Thread.currentThread().getName() + " 被唤醒，重新检查容量...");
                }

                // 缓冲区未满，可以放入数据
                buffer.offer(item); // 向队列添加元素
                System.out.println("生产者 " + Thread.currentThread().getName() + " 生产了: " + item + " (当前容量: " + buffer.size() + ")");

                // **关键点2: 唤醒可能在等待的消费者**
                // 因为放入了数据，缓冲区肯定不为空了，可以唤醒等待的消费者
                // 使用 notifyAll() 更安全，可以唤醒所有等待线程（包括其他生产者或消费者）
                // 如果只用 notify()，可能只唤醒了另一个生产者，而消费者继续饿死
                buffer.notifyAll();
            } // 释放 buffer 锁
        }

        /**
         * 消费者调用的方法，从缓冲区取出数据
         *
         * @return 取出的数据
         * @throws InterruptedException 如果线程在等待时被中断
         */
        public T take() throws InterruptedException {
            // 使用缓冲区对象自身作为锁
            synchronized (buffer) {
                // **关键点1: 必须使用 while 循环检查条件**
                while (buffer.isEmpty()) {
                    System.out.println("缓冲区为空! 消费者 " + Thread.currentThread().getName() + " 进入等待...");
                    // 缓冲区空了，消费者调用 wait() 等待，并释放 buffer 锁
                    buffer.wait();
                    System.out.println("消费者 " + Thread.currentThread().getName() + " 被唤醒，重新检查是否有数据...");
                }

                // 缓冲区不为空，可以取出数据
                T item = buffer.poll(); // 从队列头部取出并移除元素
                System.out.println("消费者 " + Thread.currentThread().getName() + " 消费了: " + item + " (当前容量: " + buffer.size() + ")");

                // **关键点2: 唤醒可能在等待的生产者**
                // 因为取出了数据，缓冲区肯定不为满了，可以唤醒等待的生产者
                buffer.notifyAll();

                return item;
            } // 释放 buffer 锁
        }
    }

    // --- Main 方法，启动生产者和消费者 ---
    public static void main(String[] args) throws InterruptedException {
        BlockingBuffer<Integer> buffer = new BlockingBuffer<>(5); // 创建容量为 5 的缓冲区

        // 生产者任务
        Runnable producerTask = () -> {
            try {
                int item = 0;
                while (true) { // 持续生产
                    buffer.put(item++);
                    // 模拟生产耗时
                    Thread.sleep((long) (Math.random() * 100));
                     if (Thread.currentThread().isInterrupted()) { // 检查中断标志
                         System.out.println("生产者 " + Thread.currentThread().getName() + " 被中断，停止生产。");
                         break;
                     }
                }
            } catch (InterruptedException e) {
                System.out.println("生产者 " + Thread.currentThread().getName() + " 捕获到中断异常，停止生产。");
                Thread.currentThread().interrupt(); // 重置中断状态
            }
        };

        // 消费者任务
        Runnable consumerTask = () -> {
            try {
                while (true) { // 持续消费
                    Integer item = buffer.take();
                    // 模拟消费耗时
                    Thread.sleep((long) (Math.random() * 500));
                     if (Thread.currentThread().isInterrupted()) { // 检查中断标志
                         System.out.println("消费者 " + Thread.currentThread().getName() + " 被中断，停止消费。");
                         break;
                     }
                }
            } catch (InterruptedException e) {
                System.out.println("消费者 " + Thread.currentThread().getName() + " 捕获到中断异常，停止消费。");
                Thread.currentThread().interrupt(); // 重置中断状态
            }
        };

        // 使用线程池来管理线程更优雅，但这里为了简单直接创建 Thread 对象
        Thread producer1 = new Thread(producerTask, "生产者-1");
        Thread producer2 = new Thread(producerTask, "生产者-2");
        Thread consumer1 = new Thread(consumerTask, "消费者-A");
        Thread consumer2 = new Thread(consumerTask, "消费者-B");
        Thread consumer3 = new Thread(consumerTask, "消费者-C");

        System.out.println("启动生产者和消费者线程...");
        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();
        consumer3.start();

        // 让程序运行一段时间，然后尝试停止
        Thread.sleep(5000); // 主线程休眠 5 秒
        System.out.println("\n主线程：准备中断所有工作线程...\n");

        // 中断所有线程（这是一种停止方式，更优雅的方式通常使用 volatile 标志）
        producer1.interrupt();
        producer2.interrupt();
        consumer1.interrupt();
        consumer2.interrupt();
        consumer3.interrupt();

        System.out.println("主线程：中断信号已发送。");
        // 等待所有线程结束 (可选)
        // producer1.join();
        // producer2.join();
        // consumer1.join();
        // ...

        System.out.println("主线程结束。");
    }
}