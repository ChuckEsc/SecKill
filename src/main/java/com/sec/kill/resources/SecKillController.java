package com.sec.kill.resources;

import com.sec.kill.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sec")
public class SecKillController {
    
    StockService stockService;

    @Autowired
    public SecKillController(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * 可以解决问题（单机下） 缺点：一个线程拿到锁其他线程处于阻塞状态，用户体验差，服务器压力大，吞吐量小
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


    @RequestMapping("/kill_optim")
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
}