import requests
import concurrent.futures
import time
import os
from collections import Counter

# --- 配置 ---
# 目标秒杀接口 URL (请确保你的 Spring Boot 应用正在运行，并且 productId=1)
SECKILL_URL = "http://localhost:8080/seckill/buy/1"
# 总共要发起的请求数量
TOTAL_REQUESTS = 1000
# 同时并发的线程数 (模拟多少个用户同时请求)
CONCURRENT_WORKERS = 50 # 可以调整这个值来改变并发级别

# --- 用于统计结果 ---
results_counter = Counter() # 使用 Counter 统计各种响应结果

# --- 发送单个秒杀请求的函数 ---
def make_seckill_request(req_id):
    """发送一次秒杀请求并记录结果"""
    global results_counter
    try:
        # 发送 POST 请求
        response = requests.post(SECKILL_URL, timeout=5) # 设置5秒超时

        # 检查 HTTP 状态码
        if response.status_code == 200:
            # 根据响应体内容判断成功或失败
            response_text = response.text
            if "抢购成功" in response_text:
                results_counter.update(["Success"])
                print(f"Req {req_id}: Success") # 可以取消注释看详细过程
                return "Success"
            elif "已售罄" in response_text or "无效" in response_text:
                results_counter.update(["Sold Out / Invalid"])
                print(f"Req {req_id}: Sold Out / Invalid")
                return "Sold Out / Invalid"
            else:
                results_counter.update(["Unknown Response"])
                print(f"Req {req_id}: Unknown Response - {response_text}")
                return "Unknown Response"
        else:
            # 处理非 200 的状态码
            results_counter.update([f"HTTP Error {response.status_code}"])
            print(f"Req {req_id}: HTTP Error {response.status_code}")
            return f"HTTP Error {response.status_code}"

    except requests.exceptions.Timeout:
        results_counter.update(["Timeout"])
        print(f"Req {req_id}: Timeout")
        return "Timeout"
    except requests.exceptions.RequestException as e:
        results_counter.update(["Request Exception"])
        print(f"Req {req_id}: Request Exception - {e}")
        return "Request Exception"
    except Exception as e:
        results_counter.update(["Other Exception"])
        print(f"Req {req_id}: Other Exception - {e}")
        return "Other Exception"

# --- 主执行逻辑 ---
if __name__ == "__main__":
    print(f"准备向 {SECKILL_URL} 发送 {TOTAL_REQUESTS} 个请求，并发数: {CONCURRENT_WORKERS}...")

    # 记录开始时间
    start_time = time.perf_counter()

    # 使用 ThreadPoolExecutor 创建线程池
    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_WORKERS) as executor:
        # 提交所有任务
        # 使用 map 可以更简洁地获取结果，但这里我们主要关心计数，所以可以用 submit + future (或者直接循环调用)
        # 为了简单起见，我们只提交任务，结果通过全局 Counter 收集
        futures = [executor.submit(make_seckill_request, i + 1) for i in range(TOTAL_REQUESTS)]

        # 等待所有任务完成 (可选，因为 with 语句块结束时会自动等待)
        # concurrent.futures.wait(futures)

    # 记录结束时间
    end_time = time.perf_counter()
    elapsed_time = end_time - start_time

    print("\n--- 请求发送完毕 ---")
    print(f"总耗时: {elapsed_time:.2f} 秒")

    # --- 打印统计结果 ---
    print("\n--- 结果统计 ---")
    success_count = results_counter.get("Success", 0)
    sold_out_count = results_counter.get("Sold Out / Invalid", 0)
    print(f"成功请求 (抢购成功): {success_count}")
    print(f"失败请求 (已售罄/无效): {sold_out_count}")

    total_processed = 0
    for result, count in results_counter.items():
        print(f"- {result}: {count}")
        total_processed += count

    print(f"总处理请求数: {total_processed}")
    if total_processed == TOTAL_REQUESTS:
        print("所有请求均已处理或统计。")
    else:
        print(f"警告：处理/统计的请求数 ({total_processed}) 与总请求数 ({TOTAL_REQUESTS}) 不符！")

    # 验证 (假设初始库存为 100)
    initial_stock = 100 # 根据你的 Java 代码设置
    if success_count == initial_stock:
        print(f"\n验证通过：成功抢购数量 ({success_count}) 等于初始库存 ({initial_stock})。")
    elif success_count > initial_stock:
        print(f"\n验证失败：出现超卖！成功抢购数量 ({success_count}) 大于初始库存 ({initial_stock})！")
    else:
        print(f"\n验证注意：成功抢购数量 ({success_count}) 小于初始库存 ({initial_stock})，可能部分请求失败或未成功处理。")

    # 计算吞吐率 (可选)
    if elapsed_time > 0:
        rps = TOTAL_REQUESTS / elapsed_time
        print(f"\n平均吞吐率 (RPS): {rps:.2f} 请求/秒")