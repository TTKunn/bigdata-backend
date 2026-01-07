package com.example.bigdatabackend.model;

/**
 * 商品状态枚举
 */
public enum ProductStatus {

    ACTIVE("active", "上架"),
    INACTIVE("inactive", "下架"),
    DELETED("deleted", "删除");

    private final String code;
    private final String description;

    ProductStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ProductStatus fromCode(String code) {
        for (ProductStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return ACTIVE; // 默认状态
    }

    @Override
    public String toString() {
        return code;
    }
}
