package com.example.bigdatabackend.dto;

import java.util.List;

/**
 * 订单列表响应DTO
 */
public class OrderListResponse {

    private List<OrderSummaryDto> orders;
    private PaginationDto pagination;

    public OrderListResponse() {
    }

    public List<OrderSummaryDto> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderSummaryDto> orders) {
        this.orders = orders;
    }

    public PaginationDto getPagination() {
        return pagination;
    }

    public void setPagination(PaginationDto pagination) {
        this.pagination = pagination;
    }

    /**
     * 分页信息DTO
     */
    public static class PaginationDto {
        private Integer page;
        private Integer size;
        private Long total;
        private Integer totalPages;

        public PaginationDto() {
        }

        public PaginationDto(Integer page, Integer size, Long total) {
            this.page = page;
            this.size = size;
            this.total = total;
            this.totalPages = (int) Math.ceil((double) total / size);
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

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Integer getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(Integer totalPages) {
            this.totalPages = totalPages;
        }
    }
}
