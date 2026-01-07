package com.example.bigdatabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 商品图片创建请求DTO
 */
public class CreateProductImageRequest {

    @JsonProperty("file")
    @NotBlank(message = "图片文件不能为空")
    private String file;  // Base64编码的图片数据或文件URL

    @JsonProperty("type")
    @NotBlank(message = "图片类型不能为空")
    private String type;  // main, detail, thumbnail

    @JsonProperty("filename")
    @NotBlank(message = "文件名不能为空")
    private String filename;

    // 默认构造函数
    public CreateProductImageRequest() {}

    // 带参数构造函数
    public CreateProductImageRequest(String file, String type, String filename) {
        this.file = file;
        this.type = type;
        this.filename = filename;
    }

    // Getter和Setter方法
    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String toString() {
        return "CreateProductImageRequest{" +
                "file='" + (file != null ? file.substring(0, Math.min(50, file.length())) + "..." : null) + '\'' +
                ", type='" + type + '\'' +
                ", filename='" + filename + '\'' +
                '}';
    }
}
