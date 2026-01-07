package com.example.bigdatabackend.dto;

import io.swagger.annotations.ApiParam;

/**
 * 商品列表查询请求DTO
 */
public class ProductListQueryRequest {

    @ApiParam(value = "页码", defaultValue = "1", example = "1")
    private Integer page = 1;

    @ApiParam(value = "每页大小", defaultValue = "20", example = "20")
    private Integer size = 20;

    @ApiParam(value = "商品分类ID", example = "0001")
    private String category;

    @ApiParam(value = "品牌名称", example = "华为")
    private String brand;

    @ApiParam(value = "商品状态", defaultValue = "ACTIVE", example = "ACTIVE")
    private String status = "ACTIVE";

    @ApiParam(value = "排序字段", defaultValue = "create_time", example = "create_time")
    private String sortBy = "create_time";

    @ApiParam(value = "排序方向", defaultValue = "desc", allowableValues = "asc,desc", example = "desc")
    private String sortOrder = "desc";

    // 默认构造函数
    public ProductListQueryRequest() {}

    // 带参数构造函数
    public ProductListQueryRequest(Integer page, Integer size) {
        this.page = page != null ? page : 1;
        this.size = size != null ? size : 20;
    }

    // Getters and Setters
    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page != null && page > 0 ? page : 1;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size != null && size > 0 && size <= 100 ? size : 20;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder != null &&
                        (sortOrder.equalsIgnoreCase("asc") || sortOrder.equalsIgnoreCase("desc")) ?
                        sortOrder.toLowerCase() : "desc";
    }

    @Override
    public String toString() {
        return "ProductListQueryRequest{" +
                "page=" + page +
                ", size=" + size +
                ", category='" + category + '\'' +
                ", brand='" + brand + '\'' +
                ", status='" + status + '\'' +
                ", sortBy='" + sortBy + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                '}';
    }
}

