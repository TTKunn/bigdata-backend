package com.example.bigdatabackend.controller;

import com.example.bigdatabackend.dto.ApiResponse;
import com.example.bigdatabackend.dto.OrderCreateRequest;
import com.example.bigdatabackend.dto.OrderDetailDto;
import com.example.bigdatabackend.dto.OrderListQueryRequest;
import com.example.bigdatabackend.dto.OrderListResponse;
import com.example.bigdatabackend.dto.OrderStatusUpdateResponse;
import com.example.bigdatabackend.exception.InvalidOrderStatusException;
import com.example.bigdatabackend.exception.OrderNotFoundException;
import com.example.bigdatabackend.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理控制器
 */
@RestController
@RequestMapping("/api/order")
@Api(tags = "订单管理接口")
@Validated
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    /**
     * 从购物车创建订单
     */
    @PostMapping("/create")
    @ApiOperation("从购物车创建订单")
    public ResponseEntity<ApiResponse<OrderDetailDto>> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {
        logger.info("Received request to create order from cart: {}", request);

        try {
            OrderDetailDto order = orderService.createOrderFromCart(request);
            return ResponseEntity.ok(ApiResponse.success(order, "订单创建成功"));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create order: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create order", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "订单创建失败"));
        }
    }

    /**
     * 查询订单列表
     */
    @GetMapping("/list")
    @ApiOperation("查询订单列表")
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrderList(
            @ApiParam(value = "订单状态筛选", example = "PENDING_PAYMENT")
            @RequestParam(required = false) String status,
            @ApiParam(value = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页大小", example = "10")
            @RequestParam(defaultValue = "10") Integer size) {

        logger.info("Received request to get order list: status={}, page={}, size={}", status, page, size);

        try {
            OrderListQueryRequest request = new OrderListQueryRequest();
            request.setStatus(status);
            request.setPage(page);
            request.setSize(size);

            OrderListResponse response = orderService.getOrderList(request);
            return ResponseEntity.ok(ApiResponse.success(response, "查询成功"));
        } catch (Exception e) {
            logger.error("Failed to get order list", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "查询失败"));
        }
    }

    /**
     * 支付订单
     */
    @PostMapping("/{orderId}/pay")
    @ApiOperation("支付订单")
    public ResponseEntity<ApiResponse<OrderStatusUpdateResponse>> payOrder(
            @ApiParam(value = "订单ID", required = true)
            @PathVariable String orderId) {

        logger.info("Received request to pay order: {}", orderId);

        try {
            OrderStatusUpdateResponse response = orderService.payOrder(orderId);
            return ResponseEntity.ok(ApiResponse.success(response, "支付成功"));
        } catch (OrderNotFoundException e) {
            logger.warn("Order not found: {}", orderId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, e.getMessage()));
        } catch (InvalidOrderStatusException e) {
            logger.warn("Invalid order status for payment: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to pay order: {}", orderId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "支付失败"));
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    @ApiOperation("取消订单")
    public ResponseEntity<ApiResponse<OrderStatusUpdateResponse>> cancelOrder(
            @ApiParam(value = "订单ID", required = true)
            @PathVariable String orderId) {

        logger.info("Received request to cancel order: {}", orderId);

        try {
            OrderStatusUpdateResponse response = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(ApiResponse.success(response, "取消成功"));
        } catch (OrderNotFoundException e) {
            logger.warn("Order not found: {}", orderId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, e.getMessage()));
        } catch (InvalidOrderStatusException e) {
            logger.warn("Invalid order status for cancellation: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to cancel order: {}", orderId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "取消失败"));
        }
    }

    /**
     * 完成订单（确认收货）
     */
    @PostMapping("/{orderId}/complete")
    @ApiOperation("完成订单（确认收货）")
    public ResponseEntity<ApiResponse<OrderStatusUpdateResponse>> completeOrder(
            @ApiParam(value = "订单ID", required = true)
            @PathVariable String orderId) {

        logger.info("Received request to complete order: {}", orderId);

        try {
            OrderStatusUpdateResponse response = orderService.completeOrder(orderId);
            return ResponseEntity.ok(ApiResponse.success(response, "确认收货成功"));
        } catch (OrderNotFoundException e) {
            logger.warn("Order not found: {}", orderId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, e.getMessage()));
        } catch (InvalidOrderStatusException e) {
            logger.warn("Invalid order status for completion: {}", e.getMessage());
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to complete order: {}", orderId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "确认收货失败"));
        }
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderId}")
    @ApiOperation("查询订单详情")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getOrderDetail(
            @ApiParam(value = "订单ID", required = true)
            @PathVariable String orderId) {

        logger.info("Received request to get order detail: {}", orderId);

        try {
            OrderDetailDto orderDetail = orderService.getOrderDetail(orderId);
            return ResponseEntity.ok(ApiResponse.success(orderDetail, "查询成功"));
        } catch (OrderNotFoundException e) {
            logger.warn("Order not found: {}", orderId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(404, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to get order detail: {}", orderId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(500, "查询失败"));
        }
    }
}
