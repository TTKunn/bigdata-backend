package com.example.bigdatabackend.controller;

import com.example.bigdatabackend.dto.ApiResponse;
import com.example.bigdatabackend.dto.CartAddRequest;
import com.example.bigdatabackend.dto.CartRemoveRequest;
import com.example.bigdatabackend.dto.CartResponse;
import com.example.bigdatabackend.dto.CartUpdateRequest;
import com.example.bigdatabackend.service.CartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 购物车管理控制器
 */
@RestController
@RequestMapping("/api/cart")
@Api(tags = "购物车管理接口")
@Validated
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    @ApiOperation("添加商品到购物车")
    public ResponseEntity<ApiResponse<Void>> addItem(
            @Valid @RequestBody CartAddRequest request) {

        try {
            logger.info("Received add to cart request: {}", request);
            cartService.addItem(request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(ApiResponse.success(null, "添加成功"));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to add item to cart: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to add item to cart", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "添加失败"));
        }
    }

    /**
     * 查询购物车
     */
    @GetMapping
    @ApiOperation("查询购物车")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {

        try {
            logger.info("Received get cart request");
            CartResponse cart = cartService.getCart();
            return ResponseEntity.ok(ApiResponse.success(cart, "查询成功"));
        } catch (Exception e) {
            logger.error("Failed to get cart", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "查询失败"));
        }
    }

    /**
     * 更新商品数量
     */
    @PutMapping("/update")
    @ApiOperation("更新商品数量")
    public ResponseEntity<ApiResponse<Void>> updateItem(
            @Valid @RequestBody CartUpdateRequest request) {

        try {
            logger.info("Received update cart item request: {}", request);
            cartService.updateItemQuantity(request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(ApiResponse.success(null, "更新成功"));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update cart item: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update cart item", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "更新失败"));
        }
    }

    /**
     * 删除购物车商品
     */
    @DeleteMapping("/remove")
    @ApiOperation("删除购物车商品")
    public ResponseEntity<ApiResponse<Void>> removeItems(
            @Valid @RequestBody CartRemoveRequest request) {

        try {
            logger.info("Received remove cart items request: {}", request);
            cartService.removeItems(request.getProductIds());
            return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to remove cart items: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to remove cart items", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "删除失败"));
        }
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    @ApiOperation("清空购物车")
    public ResponseEntity<ApiResponse<Void>> clearCart() {

        try {
            logger.info("Received clear cart request");
            cartService.clearCart();
            return ResponseEntity.ok(ApiResponse.success(null, "清空成功"));
        } catch (Exception e) {
            logger.error("Failed to clear cart", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "清空失败"));
        }
    }
}
