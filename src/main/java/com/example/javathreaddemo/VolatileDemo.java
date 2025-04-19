package com.example.javathreaddemo;

class Worker implements Runnable {
    // 使用 volatile 修饰状态标志
    private  boolean shutdownRequested = false;

    public void shutdown() {
        shutdownRequested = true; // 写 volatile 变量
        System.out.println("Shutdown requested...");
    }

    @Override
    public void run() {
        System.out.println("Worker started.");
        // 循环检查状态标志
        while (!shutdownRequested) { // 读 volatile 变量
            // 执行工作...
            try {
                Thread.sleep(100); // 模拟工作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // 如果被中断也退出
            }
        }
        System.out.println("Worker stopping.");
    }
}

public class VolatileDemo {
    public static void main(String[] args) throws InterruptedException {
        Worker worker = new Worker();
        Thread workerThread = new Thread(worker, "工作线程");
        workerThread.start();

        Thread.sleep(500); // 让工作线程运行一会儿

        // 主线程请求停止
        worker.shutdown();

        workerThread.join(); // 等待工作线程结束
        System.out.println("Main thread finished.");
    }
}