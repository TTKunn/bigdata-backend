package com.example.bigdatabackend.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建订单请求DTO
 */
public class OrderCreateRequest {

    /**
     * 要结算的商品ID列表
     */
    @NotEmpty(message = "商品列表不能为空")
    private List<String> productIds;

    /**
     * 订单备注（可选）
     */
    private String remark;

    public OrderCreateRequest() {
    }

    public OrderCreateRequest(List<String> productIds, String remark) {
        this.productIds = productIds;
        this.remark = remark;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "OrderCreateRequest{" +
                "productIds=" + productIds +
                ", remark='" + remark + '\'' +
                '}';
    }
}
