package com.example.bigdatabackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 添加商品到购物车请求
 */
public class CartAddRequest {
    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @Min(value = 1, message = "商品数量必须大于0")
    private Integer quantity = 1;

    public CartAddRequest() {
    }

    public CartAddRequest(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartAddRequest{" +
                "productId='" + productId + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
