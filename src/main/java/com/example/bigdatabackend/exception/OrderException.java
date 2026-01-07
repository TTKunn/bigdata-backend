package com.example.bigdatabackend.exception;

/**
 * 订单业务异常基类
 */
public class OrderException extends RuntimeException {

    private final int code;

    public OrderException(int code, String message) {
        super(message);
        this.code = code;
    }

    public OrderException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
