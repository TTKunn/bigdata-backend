package com.example.bigdatabackend.dto;

import java.time.LocalDateTime;

/**
 * 当日订单数量统计响应DTO
 */
public class DailyOrdersResponse {

    private String date;                     // 统计日期 (yyyyMMdd)
    private Integer orderCount;              // 订单数量
    private LocalDateTime lastUpdateTime;    // 最后更新时间

    public DailyOrdersResponse() {
    }

    public DailyOrdersResponse(String date, Integer orderCount, LocalDateTime lastUpdateTime) {
        this.date = date;
        this.orderCount = orderCount;
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
