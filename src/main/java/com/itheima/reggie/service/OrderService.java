package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;

import java.util.List;

public interface OrderService extends IService<Orders> {
    /**
     * 用户下单
     *
     * @param orders
     */
    void submit(Orders orders);

    List<OrderDetail> getOrderDetailsByOrderId(Long orderId);
}
