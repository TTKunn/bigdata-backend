package com.example.bigdatabackend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 更新购物车商品选中状态请求DTO
 */
public class CartSelectRequest {

    /**
     * 商品ID列表
     */
    @NotEmpty(message = "商品ID列表不能为空")
    private List<String> productIds;

    /**
     * 是否选中
     */
    @NotNull(message = "选中状态不能为空")
    private Boolean selected;

    public CartSelectRequest() {
    }

    public CartSelectRequest(List<String> productIds, Boolean selected) {
        this.productIds = productIds;
        this.selected = selected;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "CartSelectRequest{" +
                "productIds=" + productIds +
                ", selected=" + selected +
                '}';
    }
}
