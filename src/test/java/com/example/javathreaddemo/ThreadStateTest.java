package com.example.javathreaddemo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // 使用 SLF4J 日志更规范

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport; // 用于 WAITING 状态演示 (替代 wait/notify)

/**
 * 演示 Java 线程状态的单元测试
 * 注意：线程状态的测试可能受 OS 调度和精确时机的影响，
 * sleep() 时间的设置是为了增加观察到预期状态的概率，但不能完全保证。
 */
public class ThreadStateTest {

    private static final Logger log = LoggerFactory.getLogger(ThreadStateTest.class);

    // 用于 BLOCKED 状态演示的共享锁
    private static final Object lock = new Object();

    @BeforeEach
    void setUp() {
        log.info("------ 开始新的测试 ------");
    }

    /**
     * 测试目的：验证线程对象刚被创建后，但在调用 start() 之前的状态是否为 NEW。
     */
    @Test
    void testNewState() {
        Thread thread = new Thread(() -> {}, "新线程");
        log.info("创建线程后，状态应为 NEW: {}", thread.getState());
        Assertions.assertEquals(Thread.State.NEW, thread.getState(), "线程创建后应为 NEW 状态");
    }

    /**
     * 测试目的：验证线程调用 start() 方法后，其状态是否转变为 RUNNABLE。
     * 注意：RUNNABLE 包含 Ready 和 Running 两种子状态，具体取决于 OS 调度。
     */
    @Test
    void testRunnableState() throws InterruptedException {
        Thread thread = new Thread(() -> {
            log.info("{} 开始执行 run()...", Thread.currentThread().getName());
            // 做一些简单的CPU密集型工作来尽量保持运行
            long sum = 0;
            for (int i = 0; i < 1000000; i++) {
                sum += i;
            }
            log.info("{} 执行 run() 结束.", Thread.currentThread().getName());
        }, "运行线程");

        thread.start(); // 启动线程
        // start() 后，线程进入 RUNNABLE (可能是 Ready 或 Running)
        log.info("调用 start() 后，状态应为 RUNNABLE: {}", thread.getState());
        // 由于状态切换极快，直接断言 RUNNABLE 可能不稳定，但通常会是这个状态
        Assertions.assertEquals(Thread.State.RUNNABLE, thread.getState(), "线程启动后应为 RUNNABLE 状态");

        // 等待线程结束 (避免影响其他测试)
        thread.join();
    }

    /**
     * 测试目的：验证线程调用 Thread.sleep(long) 方法后，其状态是否转变为 TIMED_WAITING。
     */
    @Test
    void testTimedWaitingState() throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                log.info("{} 准备休眠 1 秒...", Thread.currentThread().getName());
                Thread.sleep(1000); // 休眠 1 秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "限时等待线程");

        thread.start();
        // 等待一小段时间，让子线程有机会调用 sleep()
        Thread.sleep(100); // 等待 100ms

        log.info("子线程调用 sleep(1000) 后，状态应为 TIMED_WAITING: {}", thread.getState());
        Assertions.assertEquals(Thread.State.TIMED_WAITING, thread.getState(), "调用 sleep() 后应为 TIMED_WAITING 状态");

        // 等待线程结束
        thread.join();
    }

    /**
     * 测试目的：验证线程调用 LockSupport.park() 方法后，其状态是否转变为 WAITING。
     * （调用 Object.wait() 或 Thread.join() 也能达到 WAITING 状态）
     */
    @Test
    void testWaitingState() throws InterruptedException {
        Thread thread = new Thread(() -> {
            log.info("{} 准备 park...", Thread.currentThread().getName());
            LockSupport.park(); // 使线程进入 WAITING 状态，等待 unpark
            log.info("{} 被 unpark!", Thread.currentThread().getName());
        }, "无限等待线程");

        thread.start();
        // 等待一小段时间，让子线程有机会调用 park()
        Thread.sleep(100);

        log.info("子线程调用 park() 后，状态应为 WAITING: {}", thread.getState());
        Assertions.assertEquals(Thread.State.WAITING, thread.getState(), "调用 park() 后应为 WAITING 状态");

        // 唤醒等待的线程
        log.info("准备 unpark {}...", thread.getName());
        LockSupport.unpark(thread);

        // 等待线程结束
        thread.join();
    }

    /**
     * 测试目的：验证当一个线程尝试获取一个已被其他线程持有的 synchronized 锁时，
     * 其状态是否转变为 BLOCKED。
     */
    @Test
    void testBlockedState() throws InterruptedException {
        // 线程A: 获取锁并持有
        Thread threadA = new Thread(() -> {
            synchronized (lock) {
                log.info("{} 获取到锁并持有 2 秒...", Thread.currentThread().getName());
                try {
                    Thread.sleep(2000); // 持有锁
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.info("{} 释放锁.", Thread.currentThread().getName());
            }
        }, "持有锁线程A");

        // 线程B: 尝试获取同一个锁，会被阻塞
        Thread threadB = new Thread(() -> {
            log.info("{} 尝试获取锁...", Thread.currentThread().getName());
            synchronized (lock) { // 将在此阻塞，直到 ThreadA 释放锁
                log.info("{} 获取到锁!", Thread.currentThread().getName());
            }
        }, "阻塞线程B");

        // 启动线程A，让它先拿到锁
        threadA.start();
        Thread.sleep(100); // 确保线程A已启动并获取锁

        // 启动线程B，它会尝试获取锁并被阻塞
        threadB.start();
        Thread.sleep(100); // 确保线程B已启动并尝试获取锁

        log.info("线程B尝试获取锁时，状态应为 BLOCKED: {}", threadB.getState());
        Assertions.assertEquals(Thread.State.BLOCKED, threadB.getState(), "等待 synchronized 锁时应为 BLOCKED 状态");

        // 等待两个线程都结束
        threadA.join();
        threadB.join();
    }

    /**
     * 测试目的：验证当线程的 run() 方法执行完毕后，其状态是否转变为 TERMINATED。
     */
    @Test
    void testTerminatedState() throws InterruptedException {
        Thread thread = new Thread(() -> {
            log.info("{} 执行完毕.", Thread.currentThread().getName());
        }, "终止线程");

        thread.start(); // 启动
        thread.join(); // 等待线程执行结束

        log.info("线程 join() 返回后，状态应为 TERMINATED: {}", thread.getState());
        Assertions.assertEquals(Thread.State.TERMINATED, thread.getState(), "线程执行完毕后应为 TERMINATED 状态");
    }




    // ===== 新增的测试方法，演示唤醒过程 =====

    /**
     * 测试目的：演示线程从 BLOCKED 状态被唤醒（锁被释放）后，最终能够执行并结束。
     * 验证：BLOCKED -> RUNNABLE -> TERMINATED 的转换路径。
     */
    @Test
    void testBlockedAndWakeUp() throws InterruptedException {
        CountDownLatch threadBEnteredSync = new CountDownLatch(1); // 用于确认线程B确实进入过同步块

        // 线程A: 获取锁并持有短暂时间后释放
        Thread threadA = new Thread(() -> {
            synchronized (lock) {
                log.info("{} 获取到锁，持有 1 秒...", Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                log.info("{} 释放锁.", Thread.currentThread().getName());
                // 释放锁后，threadB 才可能进入 RUNNABLE 并获取锁
            }
        }, "持有锁线程A-Wakeup");

        // 线程B: 尝试获取锁，会被阻塞，获取成功后标记 CountDownLatch
        Thread threadB = new Thread(() -> {
            log.info("{} 尝试获取锁...", Thread.currentThread().getName());
            synchronized (lock) { // 会在这里阻塞
                log.info("{} 成功获取到锁!", Thread.currentThread().getName());
                threadBEnteredSync.countDown(); // 标记已进入同步块
            }
            log.info("{} 执行完毕.", Thread.currentThread().getName());
        }, "阻塞线程B-Wakeup");

        threadA.start();
        Thread.sleep(100); // 确保 A 拿到锁
        threadB.start();
        Thread.sleep(100); // 确保 B 开始尝试获取锁

        log.info("线程B尝试获取锁时，状态断言为 BLOCKED: {}", threadB.getState());
        Assertions.assertEquals(Thread.State.BLOCKED, threadB.getState());

        // 不需要主动唤醒，等待 threadA 释放锁即可
        log.info("等待线程A释放锁...");
        threadA.join(); // 等待A执行完毕并释放锁

        log.info("线程A已释放锁，等待线程B执行完毕...");
        // 等待线程B执行完毕，如果它能结束，说明它成功从 BLOCKED -> RUNNABLE -> 获取锁 -> TERMINATED
        threadB.join(2000); // 设置超时，防止死锁或其他问题

        log.info("线程B最终状态断言为 TERMINATED: {}", threadB.getState());
        Assertions.assertEquals(Thread.State.TERMINATED, threadB.getState(), "线程B应能被唤醒并最终终止");
        Assertions.assertEquals(0, threadBEnteredSync.getCount(), "线程B应该成功进入过同步块");
    }

    /**
     * 测试目的：演示线程通过 Object.wait() 进入 WAITING 状态，
     * 然后通过 Object.notify() 被唤醒，最终能够执行并结束。
     * 验证：RUNNABLE -> WAITING -> RUNNABLE -> TERMINATED 的转换路径。
     */
    @Test
    void testWaitingAndWakeUp() throws InterruptedException {
        CountDownLatch enteredWait = new CountDownLatch(1); // 确认线程已进入 wait

        Thread thread = new Thread(() -> {
            synchronized (lock) {
                try {
                    log.info("{} 获取到锁，准备调用 wait()...", Thread.currentThread().getName());
                    enteredWait.countDown(); // 通知主线程，我已准备好 wait
                    lock.wait(); // 进入 WAITING 状态，并释放锁
                    log.info("{} 从 wait() 状态被唤醒，并重新获取了锁!", Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("等待被中断", e);
                }
            }
            log.info("{} 执行完毕.", Thread.currentThread().getName());
        }, "等待线程-Wakeup");

        thread.start();

        // 等待，直到子线程确认它准备好调用 wait()
        if (!enteredWait.await(1, TimeUnit.SECONDS)) {
            Assertions.fail("线程未能及时进入 wait 准备阶段");
        }

        // 在子线程调用 wait() 前后，主线程尝试获取锁并检查状态
        Thread.sleep(100); // 给子线程一点时间调用 wait() 并释放锁

        synchronized (lock) { // 主线程现在应该能获取到锁
            log.info("主线程获取到锁，此时 {} 状态断言为 WAITING: {}", thread.getName(), thread.getState());
            Assertions.assertEquals(Thread.State.WAITING, thread.getState(), "调用 wait() 后应为 WAITING 状态");

            log.info("主线程准备调用 notify() 唤醒 {}...", thread.getName());
            lock.notify(); // 唤醒一个等待线程
            log.info("主线程调用 notify() 完成，即将释放锁.");
        } // 主线程释放锁

        // 等待子线程执行完毕
        thread.join(2000); // 设置超时

        log.info("子线程最终状态断言为 TERMINATED: {}", thread.getState());
        Assertions.assertEquals(Thread.State.TERMINATED, thread.getState(), "线程应能被 notify 唤醒并最终终止");
    }

    /**
     * 测试目的：演示线程通过 Object.wait(long) 进入 TIMED_WAITING 状态，
     * 然后在超时前通过 Object.notify() 被唤醒，最终能够执行并结束。
     * 验证：RUNNABLE -> TIMED_WAITING -> RUNNABLE -> TERMINATED 的转换路径。
     */
    @Test
    void testTimedWaitingAndWakeUp() throws InterruptedException {
        CountDownLatch enteredWait = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            synchronized (lock) {
                try {
                    log.info("{} 获取到锁，准备调用 wait(5000)...", Thread.currentThread().getName());
                    enteredWait.countDown(); // 通知主线程已准备好 wait
                    lock.wait(5000); // 进入 TIMED_WAITING 状态，释放锁，最多等5秒
                    log.info("{} 从 wait(5000) 返回 (可能被唤醒或超时)，并重新获取了锁.", Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("等待被中断", e);
                }
            }
            log.info("{} 执行完毕.", Thread.currentThread().getName());
        }, "限时等待线程-Wakeup");

        thread.start();

        // 等待子线程确认它准备好调用 wait()
        if (!enteredWait.await(1, TimeUnit.SECONDS)) {
            Assertions.fail("线程未能及时进入 wait 准备阶段");
        }

        // 在子线程调用 wait() 前后，主线程尝试获取锁并检查状态
        Thread.sleep(100); // 给子线程时间调用 wait() 并释放锁

        synchronized (lock) { // 主线程现在应该能获取到锁
            log.info("主线程获取到锁，此时 {} 状态断言为 TIMED_WAITING: {}", thread.getName(), thread.getState());
            Assertions.assertEquals(Thread.State.TIMED_WAITING, thread.getState(), "调用 wait(long) 后应为 TIMED_WAITING 状态");

            log.info("主线程准备调用 notify() 唤醒 {}...", thread.getName());
            lock.notify(); // 在超时前唤醒它
            log.info("主线程调用 notify() 完成，即将释放锁.");
        } // 主线程释放锁

        // 等待子线程执行完毕
        thread.join(2000); // 设置超时

        log.info("子线程最终状态断言为 TERMINATED: {}", thread.getState());
        Assertions.assertEquals(Thread.State.TERMINATED, thread.getState(), "线程应能被 notify 唤醒并最终终止");
    }

}