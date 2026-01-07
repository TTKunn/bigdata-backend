package com.example.bigdatabackend.exception;

/**
 * 订单未找到异常
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("订单不存在: " + orderId);
    }
}
