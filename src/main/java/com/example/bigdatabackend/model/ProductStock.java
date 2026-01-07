package com.example.bigdatabackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 商品库存模型
 */
public class ProductStock {

    @JsonProperty("total")
    private int total;

    @JsonProperty("safe")
    private int safe;

    @JsonProperty("lock")
    private int lock;

    @JsonProperty("warehouse")
    private String warehouse;

    // 默认构造函数
    public ProductStock() {}

    // 带参数构造函数
    public ProductStock(int total, int safe, int lock, String warehouse) {
        this.total = total;
        this.safe = safe;
        this.lock = lock;
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

    public int getLock() {
        return lock;
    }

    public void setLock(int lock) {
        this.lock = lock;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    @Override
    public String toString() {
        return "ProductStock{" +
                "total=" + total +
                ", safe=" + safe +
                ", lock=" + lock +
                ", warehouse='" + warehouse + '\'' +
                '}';
    }
}
