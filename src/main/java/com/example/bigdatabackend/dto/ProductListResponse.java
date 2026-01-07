package com.example.bigdatabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 商品列表查询响应DTO
 */
public class ProductListResponse {

    @JsonProperty("total")
    private long total;

    @JsonProperty("page")
    private int page;

    @JsonProperty("size")
    private int size;

    @JsonProperty("totalPages")
    private int totalPages;

    @JsonProperty("hasNext")
    private boolean hasNext;

    @JsonProperty("hasPrevious")
    private boolean hasPrevious;

    @JsonProperty("products")
    private List<ProductSummaryDto> products;

    // 默认构造函数
    public ProductListResponse() {}

    // 构造函数
    public ProductListResponse(long total, int page, int size, List<ProductSummaryDto> products) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.products = products;
        this.totalPages = (int) Math.ceil((double) total / size);
        this.hasNext = page < totalPages;
        this.hasPrevious = page > 1;
    }

    // Getters and Setters
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / size);
        this.hasNext = page < totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
        this.hasNext = page < totalPages;
        this.hasPrevious = page > 1;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
        this.totalPages = (int) Math.ceil((double) total / size);
        this.hasNext = page < totalPages;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
        this.hasNext = page < totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public List<ProductSummaryDto> getProducts() {
        return products;
    }

    public void setProducts(List<ProductSummaryDto> products) {
        this.products = products;
    }

    @Override
    public String toString() {
        return "ProductListResponse{" +
                "total=" + total +
                ", page=" + page +
                ", size=" + size +
                ", totalPages=" + totalPages +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                ", products.size=" + (products != null ? products.size() : 0) +
                '}';
    }
}

