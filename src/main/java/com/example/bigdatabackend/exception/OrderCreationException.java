package com.example.bigdatabackend.exception;

/**
 * 订单创建失败异常
 */
public class OrderCreationException extends OrderException {

    public OrderCreationException(String message) {
        super(500, "订单创建失败: " + message);
    }

    public OrderCreationException(String message, Throwable cause) {
        super(500, "订单创建失败: " + message, cause);
    }
}
