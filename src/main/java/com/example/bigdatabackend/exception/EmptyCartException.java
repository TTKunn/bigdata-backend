package com.example.bigdatabackend.exception;

/**
 * 购物车为空异常
 */
public class EmptyCartException extends OrderException {

    public EmptyCartException() {
        super(400, "购物车中没有选中的商品");
    }

    public EmptyCartException(String message) {
        super(400, message);
    }
}
