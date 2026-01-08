package com.example.bigdatabackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 总销售额统计响应DTO
 */
public class TotalSalesResponse {

    private BigDecimal totalSales;           // 总销售额
    private Integer completedOrders;         // 已完成订单数
    private LocalDateTime lastUpdateTime;    // 最后更新时间

    public TotalSalesResponse() {
    }

    public TotalSalesResponse(BigDecimal totalSales, Integer completedOrders, LocalDateTime lastUpdateTime) {
        this.totalSales = totalSales;
        this.completedOrders = completedOrders;
        this.lastUpdateTime = lastUpdateTime;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public Integer getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(Integer completedOrders) {
        this.completedOrders = completedOrders;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
