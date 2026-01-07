package com.example.bigdatabackend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车响应
 */
public class CartResponse {
    private String userId;
    private List<CartItemDto> items;
    private Integer totalQuantity;
    private BigDecimal totalAmount;

    public CartResponse() {
    }

    public CartResponse(String userId, List<CartItemDto> items, Integer totalQuantity, BigDecimal totalAmount) {
        this.userId = userId;
        this.items = items;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "CartResponse{" +
                "userId='" + userId + '\'' +
                ", items=" + items +
                ", totalQuantity=" + totalQuantity +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
