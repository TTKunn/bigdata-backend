package com.example.bigdatabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 商品库存创建请求DTO
 */
public class CreateProductStockRequest {

    @JsonProperty("total")
    @Min(value = 0, message = "总库存不能小于0")
    private int total = 0;

    @JsonProperty("safe")
    @Min(value = 0, message = "安全库存不能小于0")
    private int safe = 0;

    @JsonProperty("warehouse")
    @NotBlank(message = "仓库信息不能为空")
    private String warehouse;

    // 默认构造函数
    public CreateProductStockRequest() {}

    // 带参数构造函数
    public CreateProductStockRequest(int total, int safe, String warehouse) {
        this.total = total;
        this.safe = safe;
        this.warehouse = warehouse;
    }

    // Getter和Setter方法
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSafe() {
        return safe;
    }

    public void setSafe(int safe) {
        this.safe = safe;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public String toString() {
        return "CreateProductStockRequest{" +
                "total=" + total +
                ", safe=" + safe +
                ", warehouse='" + warehouse + '\'' +
                '}';
    }
}
