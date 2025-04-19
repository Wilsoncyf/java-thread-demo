package com.example.javathreaddemo;

// (可以放在你的项目中的任何地方，或者直接在测试类里写个内部类)
class Counter {
    private int count = 0;

    // 不安全的递增方法
    public void increment() {
        count++; // 问题就出在这里！
    }

    public int getCount() {
        return count;
    }
}

public class RaceConditionDemo {

    public static void main(String[] args) throws InterruptedException {
        final Counter counter = new Counter(); // 共享的计数器实例

        Runnable task = () -> { // 定义任务：递增 10000 次
            for (int i = 0; i < 100000; i++) {
                counter.increment();
            }
        };

        // 创建并启动两个线程执行相同的任务
        Thread thread1 = new Thread(task, "线程A");
        Thread thread2 = new Thread(task, "线程B");

        long startTime = System.currentTimeMillis();
        thread1.start();
        thread2.start();

        // 等待两个线程都执行完毕
        thread1.join();
        thread2.join();
        long endTime = System.currentTimeMillis();

        System.out.println("预期结果: 200000");
        System.out.println("实际结果: " + counter.getCount()); // 打印最终计数值
        System.out.println("耗时: " + (endTime - startTime) + " ms");
    }
}