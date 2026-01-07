package com.example.bigdatabackend.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 删除购物车商品请求
 */
public class CartRemoveRequest {
    @NotEmpty(message = "商品ID列表不能为空")
    private List<String> productIds;

    public CartRemoveRequest() {
    }

    public CartRemoveRequest(List<String> productIds) {
        this.productIds = productIds;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    @Override
    public String toString() {
        return "CartRemoveRequest{" +
                "productIds=" + productIds +
                '}';
    }
}
