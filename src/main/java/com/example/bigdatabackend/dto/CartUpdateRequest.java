package com.example.bigdatabackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 修改购物车商品数量请求
 */
public class CartUpdateRequest {
    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @Min(value = 1, message = "商品数量必须大于0")
    private Integer quantity;

    public CartUpdateRequest() {
    }

    public CartUpdateRequest(String productId, Integer quantity) {
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
        return "CartUpdateRequest{" +
                "productId='" + productId + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
