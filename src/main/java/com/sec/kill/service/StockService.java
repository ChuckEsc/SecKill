package com.sec.kill.service;

import com.sec.kill.model.Order;
import com.sec.kill.model.Stock;
import com.sec.kill.repo.StockDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@Slf4j
public class StockService {

    StockDao stockDao;

    OrderService orderService;

    StringRedisTemplate redisTemplate;

    private static final String REDIS_PREFIX_KEY = "stock-";

    @Autowired
    public StockService(StockDao stockDao, OrderService orderService, StringRedisTemplate redisTemplate) {
        this.stockDao = stockDao;
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
    }

    // 悲观锁
    public synchronized int kill(Integer id) throws Exception {
        // 检验redis中秒杀商品是否在有效期内， 例如商品时限设置 一个小时
        if (!redisTemplate.hasKey(REDIS_PREFIX_KEY + id)) {
            throw new Exception("当前商品的抢购活动已经结束了～");
        }
        Stock stock = stockDao.findStockById(id);
        if (stock.getTotal().equals(stock.getSale())) {
            throw new Exception("商品已售空！！");
        } else {
            stock.setSale(stock.getSale() + 1);
            stockDao.updateStockSaleById(stock);
            Order order = new Order();
            order.setSid(stock.getId()).setName(stock.getName()).setCreateTime(new Date());
            orderService.insertOrder(order);
            return order.getId();
        }

    }

    // 乐观锁
    public int killOptimistic(Integer id) throws Exception {
        Stock stock = stockDao.findStockById(id);
        if (stock.getTotal().equals(stock.getSale())) {
            throw new Exception("商品已售空！！");
        } else {
            stock.setSale(stock.getSale() + 1);
            Integer res = stockDao.updateStockAndVersionSaleById(stock);
            if (res == 0) {
                // 无更新条目
                throw new RuntimeException("秒杀失败！！");
            }
            Order order = new Order();
            order.setSid(stock.getId()).setName(stock.getName()).setCreateTime(new Date());
            orderService.insertOrder(order);
            return order.getId();
        }
    }

    public int killSec(Integer id) {
        // 校验超时
        // 校验库存
        // 更新库存
        // 创建订单
        return 0;
    }


    /**
     * 用户通过验证值md5秒杀
     */
    public int killByMd5(Integer id, Long uid, String md5) throws Exception {

        if (id == null || uid == null || md5 == null)
            throw new RuntimeException("参数不合法，请重试~~~");

        String key = "MS_KEY_" + id + "_" + uid;
        String value = redisTemplate.opsForValue().get(key);
        log.info("验证用户：key={}, value={}", key, value);
        if (value == null || !value.equals(md5))
            throw new RuntimeException("请求数据不合法，请重试~~");

        return kill(id);
    }

    /**
     * 是否允许访问：检查 md5 、检查访问频率
     */
    public boolean allowVisit(Integer id, Long uid, String md5) {

        if (id == null || uid == null || md5 == null)
            throw new RuntimeException("参数不合法，请重试~~~");

        // 检查md5(功能与 killByMd5方法重复)
        String key = "MS_KEY_" + id + "_" + uid;
        String value = redisTemplate.opsForValue().get(key);
        log.info("验证用户：key={}, value={}", key, value);
        if (value == null || !value.equals(md5))
            throw new RuntimeException("验证信息不合法，请重试~~");

        // 检查访问频次
        String freKey = "LIMIT_VISIT_" + id + "_" + uid;
        // duration内最多有maxTimes次访问
        int maxTimes = 10;
        int duration = 3;
        String freValue = redisTemplate.opsForValue().get(freKey);
        log.info("用户访问：key={}, value={}", freKey, freValue);

        if (freValue == null) // 用户没有访问或者上一轮访问限制到时
            redisTemplate.opsForValue().set(freKey, "1", duration, TimeUnit.SECONDS);
        else if (freValue.equals(String.valueOf(maxTimes)))
            throw new RuntimeException("当前活动较为火爆，请重试~~~（访问次数过多）");//达到次数后，需要限制访问，直到 LIMIT_1_1 超时
        else {
            String newV = String.valueOf(Integer.parseInt(freValue) + 1);
            redisTemplate.opsForValue().set(freKey, newV, duration, TimeUnit.SECONDS);//更新时间
        }
        return true;
    }
}