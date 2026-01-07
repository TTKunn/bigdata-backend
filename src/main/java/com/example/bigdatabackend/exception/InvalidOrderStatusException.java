package com.example.bigdatabackend.exception;

/**
 * 订单状态无效异常
 */
public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(String message) {
        super(message);
    }
}
