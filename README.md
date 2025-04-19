# Java 并发编程学习与演示 (Java Concurrency Learning Demos)

本项目包含了学习 Java 并发编程核心概念时编写的演示代码。代码主要涵盖了从基础的线程同步到 JUC (java.util.concurrent) 包中常用工具的使用。

## 项目目的

* 系统学习 Java 多线程与并发知识。
* 通过可运行的 Demo 代码理解核心概念和工具的用法。
* 记录学习过程 (可能是在 AI 导师 Mr. Ranedeer 的协助下完成)。

## 技术栈

* Java 17
* Maven
* Spring Boot 3.4.4 (项目基础结构，但大部分 Demo 是独立的 Java 程序)
* Junit 5 (用于线程状态测试)

## 包含的 Demo 示例

根据 `src` 目录下的代码，本项目主要演示了以下 Java 并发主题：

* **基础同步与问题:**
    * `RaceConditionDemo.java`: 演示多线程下的竞态条件 (如 `count++`)。
    * `VolatileDemo.java`: 演示 `volatile` 关键字保证可见性的作用及局限。
* **内置锁与协作 (`synchronized`, `wait`, `notify`):**
    * `ProducerConsumerWaitNotify.java`: 使用 `wait()` 和 `notifyAll()` 实现的生产者-消费者模式。
* **JUC Lock 接口:**
    * `ProducerConsumerLockConditionDemo.java`: 使用 `ReentrantLock` 和 `Condition` 实现的生产者-消费者模式。
    * `ReadWriteLockCacheDemo.java`: 使用 `ReentrantReadWriteLock` 实现的读写缓存示例。
* **JUC 原子类:**
    * (暂未在 `main` 中直接体现，但在理论学习中已覆盖 CAS, `AtomicInteger`, ABA 问题等)
* **JUC 并发容器:**
    * (暂未在 `main` 中直接体现，但在理论学习中已覆盖 `ConcurrentHashMap`, `CopyOnWriteArrayList`, `BlockingQueue` 等)
* **JUC 协作工具:**
    * `CountDownLatchDemo.java`: 演示主线程等待多个子任务完成。
    * `CyclicBarrierDemo.java`: 演示多个线程在屏障点互相等待，可循环使用。
    * `SemaphoreDemo.java`: 演示控制对有限资源的并发访问数量。
* **线程状态测试:**
    * `src/test/java/.../ThreadStateTest.java`: 使用 JUnit 测试演示线程的不同状态 (NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED)。

## 如何运行

* 大部分 `src/main/java` 下的 Demo 类包含 `main` 方法，可以直接在 IDE 中运行或使用 `java` 命令执行。
* `ThreadStateTest.java` 是一个 JUnit 5 测试类，可以在 IDE 中运行单个测试方法或整个测试类。
* 项目使用 Maven 构建，可以通过 `mvn compile` 进行编译。

## 免责声明

本项目代码主要用于学习和演示目的，可能未经过严格的生产环境测试。