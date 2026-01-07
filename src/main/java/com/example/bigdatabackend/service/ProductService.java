package com.example.bigdatabackend.service;

import com.example.bigdatabackend.dto.CreateProductRequest;
import com.example.bigdatabackend.dto.CreateProductResponse;
import com.example.bigdatabackend.dto.CreateProductImageRequest;
import com.example.bigdatabackend.dto.CreateProductStockRequest;
import com.example.bigdatabackend.dto.ProductListQueryRequest;
import com.example.bigdatabackend.dto.ProductListResponse;
import com.example.bigdatabackend.dto.ProductSummaryDto;
import com.example.bigdatabackend.model.Product;
import com.example.bigdatabackend.model.ProductImage;
import com.example.bigdatabackend.model.ProductStatus;
import com.example.bigdatabackend.model.ProductStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品业务服务类 - 协调各个组件实现商品创建业务逻辑
 */
@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductHBaseService productHBaseService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private HdfsService hdfsService;

    /**
     * 创建商品
     */
    @Transactional
    public CreateProductResponse createProduct(CreateProductRequest request) throws Exception {
        logger.info("Starting product creation for productId: {}", request.getId());

        // 1. 参数校验
        validateCreateRequest(request);

        // 2. 处理图片上传
        ProductImage image = processImage(request);

        // 3. 构建商品对象
        Product product = buildProduct(request, image);

        // 4. 保存到HBase
        boolean saved = productHBaseService.saveProduct(product);
        if (!saved) {
            throw new RuntimeException("Failed to save product to HBase");
        }

        // 5. 初始化Redis缓存
        initializeRedisCache(product);

        // 6. 构建响应
        CreateProductResponse response = new CreateProductResponse(
            product.getId(),
            product.getCreateTime(),
            image
        );

        logger.info("Successfully created product: {}", product.getId());
        return response;
    }

    /**
     * 参数校验
     */
    private void validateCreateRequest(CreateProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }

        // 检查商品ID格式
        if (request.getId() == null || request.getId().length() != 12) {
            throw new IllegalArgumentException("商品ID格式不正确，应为12位字符");
        }

        // 检查商品名称
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("商品名称不能为空");
        }

        // 检查价格
        if (request.getPrice() == null || request.getPrice().compareTo(new java.math.BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("商品价格必须大于0");
        }

        logger.debug("Request validation passed for productId: {}", request.getId());
    }

    /**
     * 处理图片上传
     */
    private ProductImage processImage(CreateProductRequest request) throws IOException {
        if (request.getImage() != null) {
            try {
                ProductImage image = hdfsService.uploadProductImage(request.getImage(), request.getId());
                logger.debug("Successfully uploaded image: {}", image.getFilename());
                return image;
            } catch (Exception e) {
                logger.error("Failed to upload image: {}", request.getImage().getFilename(), e);
                throw new RuntimeException("图片上传失败: " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * 清理已上传的图片（出错时回滚）
     */
    private void cleanupUploadedImages(List<ProductImage> images) {
        for (ProductImage image : images) {
            try {
                hdfsService.deleteFile(image.getId());
                logger.debug("Cleaned up uploaded image: {}", image.getId());
            } catch (Exception e) {
                logger.warn("Failed to cleanup image: {}", image.getId(), e);
            }
        }
    }

    /**
     * 构建商品对象
     */
    private Product buildProduct(CreateProductRequest request, ProductImage image) {
        Product product = new Product();
        product.setId(request.getId());
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setCost(request.getCost());
        product.setDescription(request.getDescription());
        product.setSpec(request.getSpec());
        product.setTags(request.getTags());
        product.setImage(image);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        // 构建库存信息
        if (request.getStock() != null) {
            CreateProductStockRequest stockRequest = request.getStock();
            ProductStock stock = new ProductStock();
            stock.setTotal(stockRequest.getTotal());
            stock.setSafe(stockRequest.getSafe());
            stock.setLock(0); // 新创建商品锁定库存为0
            stock.setWarehouse(stockRequest.getWarehouse());
            product.setStock(stock);
        }

        return product;
    }

    /**
     * 初始化Redis缓存
     */
    private void initializeRedisCache(Product product) {
        try {
            // 缓存商品基本信息
            redisService.cacheProduct(product);

            // 缓存库存信息
            if (product.getStock() != null) {
                redisService.setStock(product.getId(), product.getStock().getTotal());
            }

            logger.debug("Initialized Redis cache for product: {}", product.getId());
        } catch (Exception e) {
            logger.error("Failed to initialize Redis cache for product: {}", product.getId(), e);
            // 缓存失败不影响主流程，继续执行
        }
    }

    /**
     * 获取商品（从缓存或数据库）
     */
    public Product getProduct(String productId) {
        if (productId == null) {
            return null;
        }

        try {
            // 首先尝试从缓存获取
            Product product = getProductFromCache(productId);
            if (product != null) {
                logger.debug("Retrieved product from cache: {}", productId);
                return product;
            }

            // 从HBase获取
            product = productHBaseService.getProduct(productId);
            if (product != null) {
                // 回填缓存
                redisService.cacheProduct(product);
                if (product.getStock() != null) {
                    redisService.setStock(productId, product.getStock().getTotal());
                }
                logger.debug("Retrieved product from HBase and cached: {}", productId);
            }

            return product;
        } catch (Exception e) {
            logger.error("Failed to get product: {}", productId, e);
            return null;
        }
    }

    /**
     * 从缓存获取商品
     */
    private Product getProductFromCache(String productId) {
        try {
            var cachedData = redisService.getCachedProduct(productId);
            if (cachedData == null || cachedData.isEmpty()) {
                return null;
            }

            // 检查必要字段是否存在
            String name = cachedData.get("name");
            if (name == null || name.trim().isEmpty()) {
                logger.debug("Cached product data incomplete for productId: {}, missing name", productId);
                return null;
            }

            // 从缓存数据构建Product对象
            Product product = new Product();
            product.setId(productId);
            product.setName(name);
            product.setCategory(cachedData.get("category"));
            product.setBrand(cachedData.get("brand"));

            // 解析价格
            String priceStr = cachedData.get("price");
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                try {
                    product.setPrice(new java.math.BigDecimal(priceStr));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid price format in cache for productId: {}", productId);
                    return null;
                }
            }

            // 解析状态
            String statusStr = cachedData.get("status");
            if (statusStr != null && !statusStr.trim().isEmpty()) {
                try {
                    product.setStatus(ProductStatus.fromCode(statusStr));
                } catch (Exception e) {
                    logger.warn("Invalid status format in cache for productId: {}", productId);
                    return null;
                }
            }

            // 获取库存信息
            try {
                Integer stock = redisService.getStock(productId);
                if (stock != null) {
                    ProductStock productStock = new ProductStock();
                    productStock.setTotal(stock);
                    product.setStock(productStock);
                }
            } catch (Exception e) {
                logger.warn("Failed to get stock from cache for productId: {}", productId, e);
                // 库存获取失败不影响商品信息返回
            }

            return product;
        } catch (Exception e) {
            logger.warn("Failed to get product from cache: {}", productId, e);
            return null;
        }
    }

    /**
     * 更新商品库存
     */
    public boolean updateProductStock(String productId, int delta) {
        try {
            // 先更新Redis缓存
            boolean cacheUpdated = redisService.deductStock(productId, Math.abs(delta));
            if (!cacheUpdated) {
                logger.warn("Failed to update stock in cache for product: {}", productId);
                return false;
            }

            // TODO: 异步更新HBase中的库存信息
            // 这里可以考虑使用消息队列来异步更新HBase

            logger.info("Successfully updated stock for product: {}, delta: {}", productId, delta);
            return true;
        } catch (Exception e) {
            logger.error("Failed to update stock for product: {}", productId, e);
            return false;
        }
    }

    /**
     * 查询商品列表（带缓存和分页）
     */
    public ProductListResponse getProductList(ProductListQueryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("查询请求不能为空");
        }

        try {
            // 构建缓存Key
            String cacheKey = buildListCacheKey(request);

            // 尝试从缓存获取
            ProductListResponse cached = getProductListFromCache(cacheKey);
            if (cached != null) {
                logger.debug("Retrieved product list from cache: {}", cacheKey);
                return cached;
            }

            // 从HBase查询
            ProductListResponse response = productHBaseService.getProductList(request);

            // 回填缓存（只缓存前几页）
            if (request.getPage() <= 3) { // 只缓存前3页
                cacheProductList(cacheKey, response);
            }

            logger.debug("Retrieved product list from HBase and cached: {}", cacheKey);
            return response;

        } catch (Exception e) {
            logger.error("Failed to get product list", e);
            throw new RuntimeException("查询商品列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从缓存获取商品列表
     */
    private ProductListResponse getProductListFromCache(String cacheKey) {
        try {
            // 这里可以实现Redis缓存逻辑
            // 暂时返回null，总是从数据库查询
            return null;
        } catch (Exception e) {
            logger.warn("Failed to get product list from cache: {}", cacheKey, e);
            return null;
        }
    }

    /**
     * 缓存商品列表
     */
    private void cacheProductList(String cacheKey, ProductListResponse response) {
        try {
            // 这里可以实现Redis缓存逻辑
            // 暂时不缓存
            logger.debug("Product list caching not implemented yet: {}", cacheKey);
        } catch (Exception e) {
            logger.warn("Failed to cache product list: {}", cacheKey, e);
        }
    }

    /**
     * 构建列表缓存Key
     */
    private String buildListCacheKey(ProductListQueryRequest request) {
        return String.format("product:list:%s:%s:%s:%d:%d:%s:%s",
                request.getCategory() != null ? request.getCategory() : "all",
                request.getBrand() != null ? request.getBrand() : "all",
                request.getStatus() != null ? request.getStatus() : "ACTIVE",
                request.getPage(),
                request.getSize(),
                request.getSortBy(),
                request.getSortOrder());
    }

    /**
     * 检查服务健康状态
     */
    public boolean checkHealth() {
        boolean hbaseHealthy = productHBaseService.checkConnection();
        boolean redisHealthy = redisService.checkConnection();
        boolean hdfsHealthy = hdfsService.checkConnection();

        logger.info("Service health check - HBase: {}, Redis: {}, HDFS: {}",
                   hbaseHealthy, redisHealthy, hdfsHealthy);

        return hbaseHealthy && redisHealthy && hdfsHealthy;
    }
}
