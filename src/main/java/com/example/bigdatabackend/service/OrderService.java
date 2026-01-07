package com.example.bigdatabackend.service;

import com.example.bigdatabackend.dto.OrderDetailDto;
import com.example.bigdatabackend.dto.OrderListQueryRequest;
import com.example.bigdatabackend.dto.OrderListResponse;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 从购物车创建订单（使用默认用户）
     *
     * @return 订单详情
     */
    OrderDetailDto createOrderFromCart();

    /**
     * 查询订单列表（使用默认用户）
     *
     * @param request 查询请求
     * @return 订单列表
     */
    OrderListResponse getOrderList(OrderListQueryRequest request);
}
