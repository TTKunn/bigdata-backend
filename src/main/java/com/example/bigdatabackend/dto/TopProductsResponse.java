package com.example.bigdatabackend.dto;

import java.util.List;

/**
 * 畅销商品排行榜响应DTO
 */
public class TopProductsResponse {

    private List<TopProductDto> topProducts;  // 畅销商品列表

    public TopProductsResponse() {
    }

    public TopProductsResponse(List<TopProductDto> topProducts) {
        this.topProducts = topProducts;
    }

    public List<TopProductDto> getTopProducts() {
        return topProducts;
    }

    public void setTopProducts(List<TopProductDto> topProducts) {
        this.topProducts = topProducts;
    }
}
