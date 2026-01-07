package com.example.bigdatabackend.controller;

import com.example.bigdatabackend.dto.ApiResponse;
import com.example.bigdatabackend.dto.OrderDetailDto;
import com.example.bigdatabackend.dto.OrderListQueryRequest;
import com.example.bigdatabackend.dto.OrderListResponse;
import com.example.bigdatabackend.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
    public ResponseEntity<ApiResponse<OrderDetailDto>> createOrder() {
        logger.info("Received request to create order from cart");

        try {
            OrderDetailDto order = orderService.createOrderFromCart();
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
}
