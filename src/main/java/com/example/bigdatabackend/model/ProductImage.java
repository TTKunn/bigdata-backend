package com.example.bigdatabackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private String uploadTime;    // 使用String类型避免Jackson反序列化问题

    @JsonProperty("url")
    private String url;      // HTTP访问URL

    // 默认构造函数
    public ProductImage() {}

    // 带参数构造函数
    public ProductImage(String id, String type, String filename, long size, String uploadTime) {
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

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "ProductImage{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", filename='" + filename + '\'' +
                ", size=" + size +
                ", uploadTime='" + uploadTime + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
