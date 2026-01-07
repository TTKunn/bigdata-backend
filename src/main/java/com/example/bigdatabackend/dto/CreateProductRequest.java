package com.example.bigdatabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品创建请求DTO
 */
public class CreateProductRequest {

    @JsonProperty("id")
    @NotBlank(message = "商品ID不能为空")
    @Size(min = 12, max = 12, message = "商品ID格式不正确，应为12位字符")
    private String id;

    @JsonProperty("name")
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称不能超过200个字符")
    private String name;

    @JsonProperty("category")
    @NotBlank(message = "商品分类不能为空")
    @Size(min = 4, max = 4, message = "商品分类格式不正确，应为4位字符")
    private String category;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("price")
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    @Digits(integer = 10, fraction = 2, message = "商品价格格式不正确")
    private BigDecimal price;

    @JsonProperty("cost")
    @DecimalMin(value = "0.00", message = "成本价不能小于0")
    @Digits(integer = 10, fraction = 2, message = "成本价格式不正确")
    private BigDecimal cost;

    @JsonProperty("description")
    @Size(max = 2000, message = "商品描述不能超过2000个字符")
    private String description;

    @JsonProperty("spec")
    private Map<String, Object> spec;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("image")
    private CreateProductImageRequest image;

    @JsonProperty("stock")
    private CreateProductStockRequest stock;

    // 默认构造函数
    public CreateProductRequest() {}

    // Getter和Setter方法
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getSpec() {
        return spec;
    }

    public void setSpec(Map<String, Object> spec) {
        this.spec = spec;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public CreateProductImageRequest getImage() {
        return image;
    }

    public void setImage(CreateProductImageRequest image) {
        this.image = image;
    }

    public CreateProductStockRequest getStock() {
        return stock;
    }

    public void setStock(CreateProductStockRequest stock) {
        this.stock = stock;
    }

    @Override
    public String toString() {
        return "CreateProductRequest{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", brand='" + brand + '\'' +
                ", price=" + price +
                ", cost=" + cost +
                ", description='" + description + '\'' +
                ", spec=" + spec +
                ", tags=" + tags +
                ", image=" + image +
                ", stock=" + stock +
                '}';
    }
}
