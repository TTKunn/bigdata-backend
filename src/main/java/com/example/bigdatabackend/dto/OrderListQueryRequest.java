package com.example.bigdatabackend.dto;

/**
 * 订单列表查询请求DTO
 */
public class OrderListQueryRequest {

    private String status;        // 订单状态筛选（可选）
    private Integer page = 1;     // 页码
    private Integer size = 10;    // 每页大小

    public OrderListQueryRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
