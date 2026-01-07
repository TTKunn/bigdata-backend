// 此文件仅用于环境联调测试，测试完成后请删除
package com.example.bigdatabackend.controller;

import com.example.bigdatabackend.model.TestProduct;
import com.example.bigdatabackend.service.TestHBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 仅用于环境联调测试
 * 提供基础的REST API接口用于验证HBase连接和数据操作
 * 测试完成后请删除此文件
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private TestHBaseService testHBaseService;

    /**
     * 健康检查接口
     * GET /api/test/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());

        try {
            boolean isConnected = testHBaseService.checkConnection();
            if (isConnected) {
                response.put("status", "success");
                response.put("message", "HBase connection is healthy");
                logger.info("Health check passed - HBase connection is healthy");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "HBase connection is not healthy");
                logger.error("Health check failed - HBase connection is not healthy");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Health check failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            logger.error("Health check failed with exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建测试表接口
     * POST /api/test/table
     */
    @PostMapping("/table")
    public ResponseEntity<Map<String, Object>> createTestTable() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());

        try {
            boolean success = testHBaseService.createTestTable();
            if (success) {
                response.put("status", "success");
                response.put("message", "Test table created successfully");
                logger.info("Test table creation request processed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to create test table");
                logger.error("Test table creation failed");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Table creation failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            logger.error("Table creation failed with exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 写入商品数据接口
     * POST /api/test/product
     */
    @PostMapping("/product")
    public ResponseEntity<Map<String, Object>> saveProduct(@RequestBody TestProduct product) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());

        // 参数校验
        if (product == null) {
            response.put("status", "error");
            response.put("message", "Request body is null");
            logger.error("Product save failed - request body is null");
            return ResponseEntity.badRequest().body(response);
        }

        if (product.getId() == null || product.getId().trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "Product ID is required");
            logger.error("Product save failed - product ID is null or empty");
            return ResponseEntity.badRequest().body(response);
        }

        // 设置默认创建时间
        if (product.getCreateTime() == null) {
            product.setCreateTime(LocalDateTime.now());
        }

        try {
            boolean success = testHBaseService.saveProduct(product);
            if (success) {
                response.put("status", "success");
                response.put("message", "Product data written to HBase successfully");
                response.put("rowKey", product.getId());
                logger.info("Product data saved successfully, rowKey: {}", product.getId());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to write product data to HBase");
                logger.error("Product save failed for rowKey: {}", product.getId());
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Product save failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            logger.error("Product save failed with exception for rowKey: {}", product.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 查询商品数据接口
     * GET /api/test/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productId) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());

        if (productId == null || productId.trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "Product ID is required");
            logger.error("Product query failed - product ID is null or empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            TestProduct product = testHBaseService.getProduct(productId);
            if (product != null) {
                response.put("status", "success");
                response.put("message", "Product data retrieved successfully");
                response.put("data", product);
                logger.info("Product data retrieved successfully, rowKey: {}", productId);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Product not found");
                logger.warn("Product not found for rowKey: {}", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Product query failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            logger.error("Product query failed with exception for rowKey: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除测试表接口（清理用）
     * DELETE /api/test/table
     */
    @DeleteMapping("/table")
    public ResponseEntity<Map<String, Object>> dropTestTable() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());

        try {
            boolean success = testHBaseService.dropTestTable();
            if (success) {
                response.put("status", "success");
                response.put("message", "Test table dropped successfully");
                logger.info("Test table drop request processed successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to drop test table");
                logger.error("Test table drop failed");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Table drop failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            logger.error("Table drop failed with exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
