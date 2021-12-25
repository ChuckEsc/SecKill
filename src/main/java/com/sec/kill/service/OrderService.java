package com.sec.kill.service;


import com.sec.kill.model.Order;
import com.sec.kill.repo.OrderDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    OrderDao orderDao;

    @Autowired
    public OrderService(OrderDao orderDao) {
        this.orderDao = orderDao;
    }

    public void insertOrder(Order order) {
        orderDao.insertOrder(order);
    }
}
