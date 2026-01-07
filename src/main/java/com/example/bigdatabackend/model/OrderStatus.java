package com.example.bigdatabackend.model;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    PENDING_PAYMENT("待支付"),
    PAID("已支付"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据字符串获取枚举
     */
    public static OrderStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        for (OrderStatus orderStatus : OrderStatus.values()) {
            if (orderStatus.name().equals(status)) {
                return orderStatus;
            }
        }
        return null;
    }
}
