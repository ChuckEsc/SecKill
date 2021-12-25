package com.sec.kill.service;

import com.sec.kill.model.Order;
import com.sec.kill.model.Stock;
import com.sec.kill.repo.StockDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@Slf4j
public class StockService {

    StockDao stockDao;

    OrderService orderService;

    @Autowired
    public StockService(StockDao stockDao, OrderService orderService) {
        this.stockDao = stockDao;
        this.orderService = orderService;
    }
    // 悲观锁
    public synchronized int kill(Integer id) throws Exception {

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
}