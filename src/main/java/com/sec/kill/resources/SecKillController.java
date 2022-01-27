package com.sec.kill.resources;

import com.google.common.util.concurrent.RateLimiter;
import com.sec.kill.service.CustomerService;
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

    CustomerService customerService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecKillController.class);

    @Autowired
    public SecKillController(StockService stockService, CustomerService customerService) {
        this.stockService = stockService;
        this.customerService = customerService;
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

    @GetMapping("/rate/limit")
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

    /**
     * 乐观锁防止超卖 + 令牌桶算法限流
     *
     * @param id 商品编号
     * @return string
     */
    @GetMapping("/kill/rate/limit")
    public String rateLimit(@RequestParam(required = true) Integer id) {
        if (!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)) {
            System.out.println("======抛弃请求======");
            return "当前抢购过于火爆，请稍后再试～";
        }
        try {
            int orderId = stockService.kill(id);
            return "秒杀成功！，订单编号 " + orderId;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    /**
     * 乐观锁防止超卖 + 令牌桶算法限流 + md5签名&接口隐藏
     *
     * @param id 商品编号
     * @return string
     */
    @GetMapping("/kill/rate/")
    public String killWithMd5(@RequestParam(required = true) Integer id, Long uid, String md5) {
        if (!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)) {
            System.out.println("======抛弃请求======");
            return "当前抢购过于火爆，请稍后再试～";
        }
        try {
            int orderId = stockService.killByMd5(id, uid, md5);
            return "秒杀成功！，订单编号 " + orderId;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    /**
     * 乐观锁防超卖 + 令牌桶限流 + md5签名（隐藏 getMd5 接口！） + 单用户访问频率限制
     */
    @GetMapping("/kill/tms")
    public String killByMd5AndTimes(Integer id, Long uid, String md5) {

        // 这里主要为了测试<限制访问频率>的功能，不考虑超时抢购，需要考虑md5
        //if (!stringRedisTemplate.hasKey("REDIS_PREFIX_KEY" + id)) { // 规定缓存中超时记录的键为 <REDIS_PREFIX_KEY + 商品id>
        //    //throw new RuntimeException("抢购已结束~~~");
        //    LOGGER.info("抢购已结束!!~");
        //    return "over";
        //}

        try {
            stockService.allowVisit(id, uid, md5);//需要验证值md5且不超时，检查访问频率
        } catch (Exception e) {
            //e.printStackTrace();
            LOGGER.info(e.getMessage());
            return e.getMessage();
        }

        if (!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)) { // 调用服务层业务之前进行限流
            LOGGER.info("抢购过于火爆，请重试~~~");
            //throw new RuntimeException("抢购过于火爆，请重试~~~");
            return "为了控制台更好的显示，这里不抛异常，不打印堆栈";
        }

        try {
            int orderId = stockService.killByMd5(id, uid, md5); //  已经检验过md5
            LOGGER.info("秒杀成功！，订单编号 " + orderId);
            return "秒杀成功！，订单编号 " + orderId;
        } catch (Exception e) {
            //e.printStackTrace();
            LOGGER.info(e.getMessage());
            return e.getMessage();
        }

    }

}