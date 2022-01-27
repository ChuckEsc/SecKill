package com.sec.kill.resources;

import com.google.common.util.concurrent.RateLimiter;
import com.sec.kill.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/sec")
public class SecKillController {

    StockService stockService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecKillController.class);

    @Autowired
    public SecKillController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 可以解决问题（单机下） 缺点：一个线程拿到锁其他线程处于阻塞状态，用户体验差，服务器压力大，吞吐量小
     *
     * @param id item
     * @return whether success
     */
    @GetMapping("/kill")
    public String kill(Integer id) {
        try {
            synchronized (this) {   // 控制层的调用处加锁
                int orderId = stockService.kill(id);
                return "秒杀成功！，订单编号 " + orderId;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }


    @RequestMapping("/kill/optimistic")
    public String killOptimistic(Integer id) {
        try {
            synchronized (this) {   // 控制层的调用处加锁
                int orderId = stockService.killOptimistic(id);
                return "秒杀成功！，订单编号 " + orderId;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }


    // 令牌桶实例
    private RateLimiter rateLimiter = RateLimiter.create(10);

    @GetMapping("/kill/rate/limit")
    public String limit(@RequestParam(required = true) Integer id) {
        // 没有获取到令牌会阻塞等待直到获取
//        LOGGER.info("等待的时间： " + rateLimiter.acquire());
        if (!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)) {
            System.out.println("限定时间2s内没有获取到令牌token");
            return "抢购失败";
        }
        System.out.println("处理业务....");
        return "测试令牌桶";
    }
}