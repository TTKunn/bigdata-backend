package com.example.bigdatabackend.model;

/**
 * 购物车项模型（内部使用，用于Redis序列化）
 */
public class CartItem {
    private String productId;      // 商品ID
    private Integer quantity;       // 商品数量
    private Long addTime;          // 添加时间（时间戳）
    private Boolean selected;       // 是否选中

    public CartItem() {
    }

    public CartItem(String productId, Integer quantity, Long addTime, Boolean selected) {
        this.productId = productId;
        this.quantity = quantity;
        this.addTime = addTime;
        this.selected = selected;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getAddTime() {
        return addTime;
    }

    public void setAddTime(Long addTime) {
        this.addTime = addTime;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", addTime=" + addTime +
                ", selected=" + selected +
                '}';
    }
}
