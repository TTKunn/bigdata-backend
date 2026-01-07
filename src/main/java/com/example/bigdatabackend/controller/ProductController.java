package com.example.bigdatabackend.controller;

import com.example.bigdatabackend.dto.ApiResponse;
import com.example.bigdatabackend.dto.CreateProductRequest;
import com.example.bigdatabackend.dto.CreateProductResponse;
import com.example.bigdatabackend.dto.CreateProductFormRequest;
import com.example.bigdatabackend.dto.CreateProductStockRequest;
import com.example.bigdatabackend.dto.CreateProductImageRequest;
import com.example.bigdatabackend.dto.ProductListQueryRequest;
import com.example.bigdatabackend.dto.ProductListResponse;
import com.example.bigdatabackend.model.Product;
import com.example.bigdatabackend.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jakarta.validation.Valid;

/**
 * 商品管理控制器
 * 提供商品创建和管理相关的REST API接口
 */
@RestController
@RequestMapping("/api/product")
@Api(tags = "商品管理接口", description = "商品管理相关接口")
@Validated
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @Autowired
    private ProductService productService;

    /**
     * 创建商品接口 - JSON格式
     */
    @PostMapping(consumes = "application/json")
    @ApiOperation(value = "创建商品(JSON)", notes = "创建新的商品信息，支持图片上传(JSON格式)")
    public ResponseEntity<ApiResponse<CreateProductResponse>> createProduct(
            @ApiParam(value = "商品创建请求", required = true)
            @Valid @RequestBody CreateProductRequest request) {

        logger.info("Received product creation request for productId: {}", request.getId());

        try {
            // 创建商品
            CreateProductResponse response = productService.createProduct(request);

            logger.info("Successfully created product: {}", response.getProductId());
            return ResponseEntity.ok(ApiResponse.success(response, "商品创建成功"));

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for product creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to create product: {}", request.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "商品创建失败: " + e.getMessage()));
        }
    }

    /**
     * 创建商品接口 - Form Data格式
     */
    @PostMapping(consumes = "multipart/form-data")
    @ApiOperation(value = "创建商品(Form Data)", notes = "创建新的商品信息，支持文件上传(Form Data格式)")
    public ResponseEntity<ApiResponse<CreateProductResponse>> createProductWithFormData(
            @ApiParam(value = "商品ID", required = true) @RequestParam String id,
            @ApiParam(value = "商品名称", required = true) @RequestParam String name,
            @ApiParam(value = "商品分类", required = true) @RequestParam String category,
            @ApiParam(value = "品牌名称") @RequestParam(required = false) String brand,
            @ApiParam(value = "商品价格", required = true) @RequestParam String price,
            @ApiParam(value = "成本价") @RequestParam(required = false) String cost,
            @ApiParam(value = "商品描述") @RequestParam(required = false) String description,
            @ApiParam(value = "规格参数JSON") @RequestParam(required = false) String spec,
            @ApiParam(value = "标签JSON") @RequestParam(required = false) String tags,
            @ApiParam(value = "库存信息JSON") @RequestParam(required = false) String stock,
            @ApiParam(value = "图片文件") @RequestParam(required = false) MultipartFile image,
            @ApiParam(value = "图片类型") @RequestParam(required = false, defaultValue = "main") String imageType) {

        logger.info("Received form data product creation request for productId: {}", id);

        try {
            // 转换Form Data为标准请求对象
            CreateProductRequest request = convertFormDataToRequest(id, name, category, brand, price, cost,
                description, spec, tags, stock, image, imageType);

            // 创建商品
            CreateProductResponse response = productService.createProduct(request);

            logger.info("Successfully created product: {}", response.getProductId());
            return ResponseEntity.ok(ApiResponse.success(response, "商品创建成功"));

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for product creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to create product: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "商品创建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取商品列表接口
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取商品列表", notes = "分页查询商品列表，支持条件筛选和排序")
    public ResponseEntity<ApiResponse<ProductListResponse>> getProductList(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(required = false) Integer page,
            @ApiParam(value = "每页大小", defaultValue = "20") @RequestParam(required = false) Integer size,
            @ApiParam(value = "商品分类ID") @RequestParam(required = false) String category,
            @ApiParam(value = "品牌名称") @RequestParam(required = false) String brand,
            @ApiParam(value = "商品状态") @RequestParam(required = false) String status,
            @ApiParam(value = "排序字段") @RequestParam(required = false) String sortBy,
            @ApiParam(value = "排序方向") @RequestParam(required = false) String sortOrder) {

        logger.info("Received product list request - page: {}, size: {}, category: {}, brand: {}, status: {}, sortBy: {}, sortOrder: {}",
                   page, size, category, brand, status, sortBy, sortOrder);

        try {
            // 构建查询请求
            ProductListQueryRequest request = new ProductListQueryRequest();
            request.setPage(page);
            request.setSize(size);
            request.setCategory(category);
            request.setBrand(brand);
            request.setStatus(status);
            request.setSortBy(sortBy);
            request.setSortOrder(sortOrder);

            // 查询商品列表
            ProductListResponse response = productService.getProductList(request);

            logger.info("Successfully retrieved product list - total: {}, page: {}, size: {}",
                       response.getTotal(), response.getPage(), response.getSize());
            return ResponseEntity.ok(ApiResponse.success(response, "查询成功"));

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for product list: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to query product list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取商品信息接口
     */
    @GetMapping("/{productId}")
    @ApiOperation(value = "获取商品信息", notes = "根据商品ID获取商品详细信息")
    public ResponseEntity<ApiResponse<Product>> getProduct(
            @ApiParam(value = "商品ID", required = true, example = "000100000001")
            @PathVariable String productId) {

        logger.info("Received product query request for productId: {}", productId);

        // 参数校验
        if (productId == null || productId.trim().isEmpty()) {
            logger.warn("Product ID is null or empty");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "商品ID不能为空"));
        }

        if (productId.length() != 12) {
            logger.warn("Invalid product ID format: {} (length: {})", productId, productId.length());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "商品ID格式不正确，应为12位字符"));
        }

        try {
            Product product = productService.getProduct(productId);

            if (product != null) {
                logger.info("Successfully retrieved product: {}", productId);
                return ResponseEntity.ok(ApiResponse.success(product, "查询成功"));
            } else {
                logger.warn("Product not found: {}", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "商品不存在"));
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters for product query: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Product not found: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to query product: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "查询失败: " + e.getMessage()));
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @ApiOperation(value = "服务健康检查", notes = "检查商品服务各组件的健康状态")
    public ResponseEntity<ApiResponse<Object>> healthCheck() {
        try {
            boolean healthy = productService.checkHealth();

            if (healthy) {
                return ResponseEntity.ok(ApiResponse.success(null, "服务运行正常"));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error(503, "服务不可用"));
            }

        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "健康检查失败: " + e.getMessage()));
        }
    }

    /**
     * 更新商品库存接口（演示用）
     */
    @PutMapping("/{productId}/stock")
    @ApiOperation(value = "更新商品库存", notes = "扣减商品库存（演示缓存和数据库协同工作）")
    public ResponseEntity<ApiResponse<Object>> updateStock(
            @ApiParam(value = "商品ID", required = true)
            @PathVariable String productId,

            @ApiParam(value = "扣减数量", required = true, example = "1")
            @RequestParam int quantity) {

        logger.info("Received stock update request for productId: {}, quantity: {}", productId, quantity);

        // 参数校验
        if (quantity <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "扣减数量必须大于0"));
        }

        try {
            boolean success = productService.updateProductStock(productId, quantity);

            if (success) {
                logger.info("Successfully updated stock for productId: {}, quantity: {}", productId, quantity);
                return ResponseEntity.ok(ApiResponse.success(null, "库存更新成功"));
            } else {
                logger.warn("Failed to update stock for productId: {}, insufficient stock", productId);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(409, "库存不足"));
            }

        } catch (Exception e) {
            logger.error("Failed to update stock for productId: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "库存更新失败: " + e.getMessage()));
        }
    }

    /**
     * 将Form Data参数转换为标准请求对象
     */
    private CreateProductRequest convertFormDataToRequest(String id, String name, String category, String brand,
            String price, String cost, String description, String spec, String tags, String stock,
            MultipartFile image, String imageType) {
        Gson gson = new Gson();
        CreateProductRequest request = new CreateProductRequest();

        // 基本字段转换
        request.setId(id);
        request.setName(name);
        request.setCategory(category);
        request.setBrand(brand);

        // 数值字段转换
        if (price != null && !price.trim().isEmpty()) {
            request.setPrice(new java.math.BigDecimal(price));
        }
        if (cost != null && !cost.trim().isEmpty()) {
            request.setCost(new java.math.BigDecimal(cost));
        }

        request.setDescription(description);

        // JSON字符串解析
        if (spec != null && !spec.trim().isEmpty()) {
            try {
                request.setSpec(gson.fromJson(spec,
                    new TypeToken<java.util.Map<String, Object>>(){}.getType()));
            } catch (Exception e) {
                throw new IllegalArgumentException("规格参数JSON格式不正确");
            }
        }

        if (tags != null && !tags.trim().isEmpty()) {
            try {
                request.setTags(gson.fromJson(tags,
                    new TypeToken<java.util.List<String>>(){}.getType()));
            } catch (Exception e) {
                throw new IllegalArgumentException("标签JSON格式不正确");
            }
        }

        if (stock != null && !stock.trim().isEmpty()) {
            try {
                request.setStock(gson.fromJson(stock, CreateProductStockRequest.class));
            } catch (Exception e) {
                throw new IllegalArgumentException("库存信息JSON格式不正确");
            }
        }

        // 处理图片文件
        if (image != null && !image.isEmpty()) {
            CreateProductImageRequest imageRequest = new CreateProductImageRequest();

            try {
                // 转换为Base64
                byte[] bytes = image.getBytes();
                String base64Data = java.util.Base64.getEncoder().encodeToString(bytes);
                imageRequest.setFile(base64Data);
                imageRequest.setFilename(image.getOriginalFilename());

                // 设置图片类型
                imageRequest.setType(imageType != null && !imageType.trim().isEmpty() ? imageType.trim() : "main");

                // 设置为单张图片
                request.setImage(imageRequest);

            } catch (Exception e) {
                logger.warn("Failed to process image file: {}", image.getOriginalFilename(), e);
                throw new IllegalArgumentException("图片文件处理失败: " + e.getMessage());
            }
        }

        return request;
    }

}
