package com.example.bigdatabackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 当日销售额统计响应DTO
 */
public class DailySalesResponse {

    private String date;                     // 统计日期 (yyyyMMdd)
    private BigDecimal dailySales;           // 当日销售额
    private Integer orderCount;              // 订单数量
    private BigDecimal averageOrderValue;    // 平均客单价
    private LocalDateTime lastUpdateTime;    // 最后更新时间

    public DailySalesResponse() {
    }

    public DailySalesResponse(String date, BigDecimal dailySales, Integer orderCount,
                             BigDecimal averageOrderValue, LocalDateTime lastUpdateTime) {
        this.date = date;
        this.dailySales = dailySales;
        this.orderCount = orderCount;
        this.averageOrderValue = averageOrderValue;
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getDailySales() {
        return dailySales;
    }

    public void setDailySales(BigDecimal dailySales) {
        this.dailySales = dailySales;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(BigDecimal averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
