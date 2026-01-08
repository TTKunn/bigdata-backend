package com.example.bigdatabackend.dto;

import java.time.LocalDateTime;

/**
 * 畅销商品DTO
 */
public class TopProductDto {

    private Integer rank;                    // 排名
    private String productId;                // 商品ID
    private String productName;              // 商品名称
    private Integer totalSales;              // 总销量
    private LocalDateTime lastUpdateTime;    // 最后更新时间

    public TopProductDto() {
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Integer totalSales) {
        this.totalSales = totalSales;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
