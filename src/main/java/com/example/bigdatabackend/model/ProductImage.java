package com.example.bigdatabackend.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 商品图片模型
 */
public class ProductImage {

    @JsonProperty("id")
    private String id;        // HDFS路径

    @JsonProperty("type")
    private String type;      // main, detail, thumbnail

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("size")
    private long size;

    @JsonProperty("upload_time")  // HBase中使用蛇形命名
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime uploadTime;

    // 默认构造函数
    public ProductImage() {}

    // 带参数构造函数
    public ProductImage(String id, String type, String filename, long size, LocalDateTime uploadTime) {
        this.id = id;
        this.type = type;
        this.filename = filename;
        this.size = size;
        this.uploadTime = uploadTime;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    @Override
    public String toString() {
        return "ProductImage{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", filename='" + filename + '\'' +
                ", size=" + size +
                ", uploadTime=" + uploadTime +
                '}';
    }
}
