package com.example.bigdatabackend.dto;

import com.example.bigdatabackend.model.OrderStatus;

import java.time.LocalDateTime;

/**
 * 订单状态更新响应DTO
 */
public class OrderStatusUpdateResponse {

    private String orderId;              // 订单ID
    private OrderStatus status;          // 订单状态
    private LocalDateTime payTime;       // 支付时间
    private LocalDateTime cancelTime;    // 取消时间
    private LocalDateTime completeTime;  // 完成时间
    private String message;              // 响应消息

    public OrderStatusUpdateResponse() {
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public LocalDateTime getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(LocalDateTime cancelTime) {
        this.cancelTime = cancelTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
