package com.example.javathreaddemo.controller; // 建议新建一个 controller 包

import com.example.javathreaddemo.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 标记为 RESTful Controller
@RequestMapping("/seckill") // 请求路径前缀
public class SeckillController {

    // 注入 SeckillService
    private final SeckillService seckillService;

    @Autowired // 构造器注入
    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 处理秒杀购买请求
     * 使用 POST /seckill/buy/{productId}
     *
     * @param productId 商品ID
     * @return ResponseEntity 包含成功或失败信息
     */
    @PostMapping("/buy/{productId}")
    public ResponseEntity<String> buy(@PathVariable int productId) {
        boolean success = seckillService.processSeckill(productId);

        if (success) {
            // 秒杀成功，返回 HTTP 200 OK
            return ResponseEntity.ok("抢购成功！商品ID: " + productId);
        } else {
            // 库存不足或商品ID无效，返回 HTTP 410 GONE (或者其他合适的错误码)
            // 这里用 200 OK 但 body 指示失败也很常见
             // return ResponseEntity.status(HttpStatus.GONE).body("已售罄或商品无效！商品ID: " + productId);
             return ResponseEntity.ok("已售罄或商品无效！商品ID: " + productId);
        }
    }

    // (可选) 添加一个查询库存的接口
    // GET /seckill/stock/{productId}
    // ...
}