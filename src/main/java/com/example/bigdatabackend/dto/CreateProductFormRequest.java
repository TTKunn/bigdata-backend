package com.example.bigdatabackend.dto;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品创建表单请求DTO
 * 用于处理multipart/form-data格式的请求
 */
public class CreateProductFormRequest {

    @NotBlank(message = "商品ID不能为空")
    @Pattern(regexp = "^\\d{12}$", message = "商品ID格式不正确，应为12位数字")
    private String id;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称不能超过200字符")
    private String name;

    @NotBlank(message = "商品分类不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "商品分类格式不正确，应为4位数字")
    private String category;

    @Size(max = 100, message = "品牌名称不能超过100字符")
    private String brand;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @Digits(integer = 10, fraction = 2, message = "价格格式不正确，最多2位小数")
    private String price;

    @DecimalMin(value = "0.00", message = "成本价不能小于0")
    @Digits(integer = 10, fraction = 2, message = "成本价格式不正确，最多2位小数")
    private String cost;

    @Size(max = 2000, message = "商品描述不能超过2000字符")
    private String description;

    // 规格参数JSON字符串
    private String spec;

    // 标签数组JSON字符串
    private String tags;

    // 库存信息JSON字符串
    private String stock;

    // 图片文件数组
    private MultipartFile[] images;

    // 图片类型字符串，用逗号分隔
    private String imageTypes;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public MultipartFile[] getImages() {
        return images;
    }

    public void setImages(MultipartFile[] images) {
        this.images = images;
    }

    public String getImageTypes() {
        return imageTypes;
    }

    public void setImageTypes(String imageTypes) {
        this.imageTypes = imageTypes;
    }
}
