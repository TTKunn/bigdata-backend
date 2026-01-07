package com.example.bigdatabackend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单模型类
 */
public class Order {

    private String orderId;           // 订单号
    private String userId;            // 用户ID
    private BigDecimal totalAmount;   // 商品总金额
    private BigDecimal discountAmount; // 优惠金额
    private BigDecimal actualAmount;  // 实付金额
    private OrderStatus status;       // 订单状态
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime payTime;    // 支付时间
    private LocalDateTime cancelTime; // 取消时间
    private LocalDateTime completeTime; // 完成时间

    // 收货信息
    private String receiver;          // 收货人
    private String phone;             // 联系电话
    private String address;           // 详细地址
    private String postcode;          // 邮编

    // 商品明细
    private List<OrderItem> items;

    public Order() {
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public LocalDateTime getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(LocalDateTime cancelTime) {
        this.cancelTime = cancelTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    /**
     * 验证是否可以转换到目标状态
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        if (this.status == null || newStatus == null) {
            return false;
        }

        switch (this.status) {
            case PENDING_PAYMENT:
                return newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELLED;
            case PAID:
                return newStatus == OrderStatus.COMPLETED;
            case COMPLETED:
            case CANCELLED:
                return false; // 终态不能转换
            default:
                return false;
        }
    }

    /**
     * 更新支付时间
     */
    public void updatePayTime() {
        this.payTime = LocalDateTime.now();
    }

    /**
     * 更新取消时间
     */
    public void updateCancelTime() {
        this.cancelTime = LocalDateTime.now();
    }

    /**
     * 更新完成时间
     */
    public void updateCompleteTime() {
        this.completeTime = LocalDateTime.now();
    }

    /**
     * 订单商品明细内部类
     */
    public static class OrderItem {
        private String productId;
        private String productName;
        private String category;
        private String brand;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalAmount;

        public OrderItem() {
        }

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
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

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }
    }
}
