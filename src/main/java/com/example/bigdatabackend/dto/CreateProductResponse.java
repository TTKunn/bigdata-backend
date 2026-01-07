package com.example.bigdatabackend.dto;

import com.example.bigdatabackend.model.ProductImage;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品创建响应DTO
 */
public class CreateProductResponse {

    @JsonProperty("productId")
    private String productId;

    @JsonProperty("createTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createTime;

    @JsonProperty("images")
    private List<ProductImage> images;

    // 默认构造函数
    public CreateProductResponse() {}

    // 带参数构造函数
    public CreateProductResponse(String productId, LocalDateTime createTime, List<ProductImage> images) {
        this.productId = productId;
        this.createTime = createTime;
        this.images = images;
    }

    // Getter和Setter方法
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "CreateProductResponse{" +
                "productId='" + productId + '\'' +
                ", createTime=" + createTime +
                ", images=" + images +
                '}';
    }
}
