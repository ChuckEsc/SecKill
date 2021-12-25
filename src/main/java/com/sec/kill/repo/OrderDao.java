package com.sec.kill.repo;

import com.sec.kill.model.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderDao {
    void insertOrder(Order order);
}