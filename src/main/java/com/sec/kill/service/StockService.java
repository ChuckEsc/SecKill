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
    public int killOptimistic(Integer id) throws Exception{
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

    public int killSec(Integer id){
        // 校验超时
        // 校验库存
        // 更新库存
        // 创建订单
        return 0;
    }
}