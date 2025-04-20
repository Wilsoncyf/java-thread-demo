package com.example.javathreaddemo.service; // 建议新建一个 service 包

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service // 标记为 Spring 的 Service 组件
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    // 商品ID -> 库存 映射 (简化，只模拟一个商品 productId=1)
    private final AtomicInteger stock = new AtomicInteger(100); // 初始库存 100
    private final int productId = 1;

    // 统计成功次数 (可选，用于观察)
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);


    /**
     * 处理秒杀请求，尝试扣减库存
     *
     * @param requestedProductId 请求的商品ID (当前只支持 1)
     * @return true 如果秒杀成功, false 如果库存不足或商品ID不对
     */
    public boolean processSeckill(int requestedProductId) {
        // 简单验证商品 ID
        if (requestedProductId != this.productId) {
            log.warn("请求了无效的商品ID: {}", requestedProductId);
            return false;
        }

        // --- 使用 CAS 循环扣减库存 ---
        int currentStock;
        do {
            currentStock = stock.get();
            if (currentStock <= 0) {
                // log.info("库存不足 ({}). 请求线程: {}", currentStock, Thread.currentThread().getName());
                failCount.incrementAndGet(); // 统计失败
                return false; // 库存不足
            }
        } while (!stock.compareAndSet(currentStock, currentStock - 1));

        // CAS 成功，库存扣减成功
        int successNum = successCount.incrementAndGet(); // 统计成功
        log.info("秒杀成功! 商品ID: {}, 成功次数: {}, 剩余库存: {}. 请求线程: {}",
                 requestedProductId, successNum, currentStock - 1, Thread.currentThread().getName());
        // 实际项目中，这里应该继续创建订单、发送消息等后续操作...
        return true;
    }

    /**
     * 获取当前库存 (用于查询)
     * @param requestedProductId 商品ID
     * @return 当前库存, 如果商品ID无效返回-1
     */
    public int getCurrentStock(int requestedProductId) {
         if (requestedProductId != this.productId) {
            return -1;
         }
        return stock.get();
    }

    // --- 用于统计的方法 (可选) ---
     public int getSuccessCount(int requestedProductId) {
         if (requestedProductId != this.productId) return 0;
         return successCount.get();
     }
      public int getFailCount(int requestedProductId) {
          if (requestedProductId != this.productId) return 0;
          return failCount.get();
      }
}