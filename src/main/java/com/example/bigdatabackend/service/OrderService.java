package com.example.bigdatabackend.service;

import com.example.bigdatabackend.dto.OrderCreateRequest;
import com.example.bigdatabackend.dto.OrderDetailDto;
import com.example.bigdatabackend.dto.OrderListQueryRequest;
import com.example.bigdatabackend.dto.OrderListResponse;
import com.example.bigdatabackend.dto.OrderStatusUpdateResponse;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 从购物车创建订单（使用默认用户）
     *
     * @param request 订单创建请求（包含要结算的商品ID列表）
     * @return 订单详情
     */
    OrderDetailDto createOrderFromCart(OrderCreateRequest request);

    /**
     * 查询订单列表（使用默认用户）
     *
     * @param request 查询请求
     * @return 订单列表
     */
    OrderListResponse getOrderList(OrderListQueryRequest request);

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return 状态更新结果
     */
    OrderStatusUpdateResponse payOrder(String orderId);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return 状态更新结果
     */
    OrderStatusUpdateResponse cancelOrder(String orderId);

    /**
     * 完成订单（确认收货）
     *
     * @param orderId 订单ID
     * @return 状态更新结果
     */
    OrderStatusUpdateResponse completeOrder(String orderId);

    /**
     * 查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderDetailDto getOrderDetail(String orderId);
}
