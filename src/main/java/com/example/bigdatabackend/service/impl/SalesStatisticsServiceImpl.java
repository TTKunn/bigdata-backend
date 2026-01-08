package com.example.bigdatabackend.service.impl;

import com.example.bigdatabackend.dto.*;
import com.example.bigdatabackend.model.Order;
import com.example.bigdatabackend.model.OrderStatus;
import com.example.bigdatabackend.model.Product;
import com.example.bigdatabackend.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 销售统计服务实现类
 */
@Service
public class SalesStatisticsServiceImpl implements SalesStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(SalesStatisticsServiceImpl.class);

    @Autowired
    private RedisService redisService;

    @Autowired
    private SalesStatisticsHBaseService hBaseService;

    @Autowired
    private OrderHBaseService orderHBaseService;

    @Autowired
    private ProductService productService;

    /**
     * 获取总销售额统计
     * 优先从Redis获取，Redis没有则从HBase计算
     */
    @Override
    public TotalSalesResponse getTotalSales() {
        try {
            // 1. 尝试从Redis获取
            String totalSalesStr = redisService.getString("statistics:sales:total");
            String totalCountStr = redisService.getString("statistics:sales:total:count");
            String lastUpdateStr = redisService.getString("statistics:sales:total:update");

            if (totalSalesStr != null && totalCountStr != null) {
                BigDecimal totalSales = new BigDecimal(totalSalesStr);
                Integer completedOrders = Integer.parseInt(totalCountStr);
                LocalDateTime lastUpdate = lastUpdateStr != null
                    ? LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lastUpdateStr)), ZoneId.systemDefault())
                    : LocalDateTime.now();

                logger.debug("Retrieved total sales from Redis: totalSales={}, completedOrders={}",
                    totalSales, completedOrders);
                return new TotalSalesResponse(totalSales, completedOrders, lastUpdate);
            }

            // 2. 从HBase计算并缓存
            logger.info("Total sales not found in Redis, calculating from HBase");
            TotalSalesResponse response = hBaseService.calculateTotalSales();
            cacheTotalSales(response);
            return response;
        } catch (Exception e) {
            logger.error("Failed to get total sales", e);
            return new TotalSalesResponse(BigDecimal.ZERO, 0, LocalDateTime.now());
        }
    }

    /**
     * 获取当日销售额统计
     */
    @Override
    public DailySalesResponse getDailySales(String date) {
        try {
            String dateKey = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String hashKey = "statistics:sales:daily:" + dateKey;

            // 从Redis获取当日统计数据
            Map<String, String> dailyStats = redisService.hgetAll(hashKey);

            if (dailyStats.isEmpty()) {
                logger.debug("Daily sales not found in Redis for date: {}", dateKey);
                return createEmptyDailyResponse(dateKey);
            }

            return parseDailySalesResponse(dateKey, dailyStats);
        } catch (Exception e) {
            logger.error("Failed to get daily sales for date: {}", date, e);
            String dateKey = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            return createEmptyDailyResponse(dateKey);
        }
    }

    /**
     * 获取当日订单数量统计
     */
    @Override
    public DailyOrdersResponse getDailyOrders(String date) {
        try {
            String dateKey = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String hashKey = "statistics:sales:daily:" + dateKey;

            // 从Redis获取当日订单数
            String orderCountStr = redisService.hget(hashKey, "orders");
            Integer orderCount = orderCountStr != null ? Integer.parseInt(orderCountStr) : 0;

            String lastUpdateStr = redisService.hget(hashKey, "lastUpdate");
            LocalDateTime lastUpdate = lastUpdateStr != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lastUpdateStr)), ZoneId.systemDefault())
                : LocalDateTime.now();

            logger.debug("Retrieved daily orders from Redis: date={}, orderCount={}", dateKey, orderCount);
            return new DailyOrdersResponse(dateKey, orderCount, lastUpdate);
        } catch (Exception e) {
            logger.error("Failed to get daily orders for date: {}", date, e);
            String dateKey = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            return new DailyOrdersResponse(dateKey, 0, LocalDateTime.now());
        }
    }

    /**
     * 获取最畅销商品排行榜
     */
    @Override
    public TopProductsResponse getTopProducts(int limit) {
        List<TopProductDto> topProducts = new ArrayList<>();

        try {
            // 从Redis Sorted Set获取排行榜
            Set<String> productIds = redisService.zrevrange("statistics:product:sales:rank", 0, limit - 1);

            int rank = 1;
            for (String productId : productIds) {
                // 获取商品详细信息
                Map<String, String> productStats = redisService.hgetAll("statistics:product:sales:" + productId);
                if (!productStats.isEmpty()) {
                    TopProductDto dto = new TopProductDto();
                    dto.setRank(rank++);
                    dto.setProductId(productId);
                    dto.setProductName(productStats.get("name"));

                    String totalSalesStr = productStats.get("totalSales");
                    dto.setTotalSales(totalSalesStr != null ? Integer.parseInt(totalSalesStr) : 0);

                    String lastUpdateStr = productStats.get("lastUpdate");
                    if (lastUpdateStr != null) {
                        dto.setLastUpdateTime(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(Long.parseLong(lastUpdateStr)), ZoneId.systemDefault()));
                    }

                    topProducts.add(dto);
                }
            }

            logger.debug("Retrieved top {} products from Redis", topProducts.size());
        } catch (Exception e) {
            logger.error("Failed to get top products", e);
        }

        return new TopProductsResponse(topProducts);
    }

    /**
     * 订单完成时将订单ID写入待更新队列
     */
    @Override
    public void enqueueOrderForStatisticsUpdate(String orderId) {
        if (orderId == null) {
            logger.warn("Cannot enqueue order: orderId is null");
            return;
        }

        try {
            // 将订单ID写入待更新队列
            redisService.lpush("statistics:update:queue", orderId);
            logger.info("Enqueued order for statistics update: {}", orderId);
        } catch (Exception e) {
            logger.error("Failed to enqueue order for statistics update: {}", orderId, e);
        }
    }

    /**
     * 定时任务：批量处理待更新的订单统计数据
     * 每20秒执行一次
     */
    @Override
    @Scheduled(fixedRate = 20000)
    public void processPendingStatisticsUpdates() {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            int batchSize = 1000; // 每次最多处理1000条
            int processedCount = 0;

            for (int i = 0; i < batchSize; i++) {
                // 从队列中取出订单ID
                String orderId = redisService.rpop("statistics:update:queue");
                if (orderId == null) {
                    break; // 队列为空
                }

                // 检查是否已处理（去重）
                if (Boolean.TRUE.equals(redisService.sismember("statistics:processed:orders:" + today, orderId))) {
                    logger.debug("Order already processed, skipping: {}", orderId);
                    continue;
                }

                // 查询订单详情
                Order order = orderHBaseService.getOrderById(orderId);
                if (order == null || order.getStatus() != OrderStatus.COMPLETED) {
                    logger.warn("Order not found or not completed, skipping: {}", orderId);
                    continue;
                }

                // 更新统计数据
                updateTotalSales(order.getActualAmount());
                updateDailySales(order);
                updateProductSalesStatistics(order);

                // 记录已处理
                redisService.sadd("statistics:processed:orders:" + today, orderId);
                redisService.expire("statistics:processed:orders:" + today, 86400); // 24小时过期

                processedCount++;
            }

            if (processedCount > 0) {
                logger.info("Processed {} orders for statistics update", processedCount);
            }
        } catch (Exception e) {
            logger.error("Failed to process pending statistics updates", e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 更新总销售额
     */
    private void updateTotalSales(BigDecimal amount) {
        try {
            // 更新总销售额
            redisService.incrByFloat("statistics:sales:total", amount.doubleValue());

            // 更新已完成订单数
            redisService.incr("statistics:sales:total:count");

            // 更新最后更新时间
            redisService.setString("statistics:sales:total:update",
                String.valueOf(System.currentTimeMillis()));

            logger.debug("Updated total sales: amount={}", amount);
        } catch (Exception e) {
            logger.error("Failed to update total sales", e);
        }
    }

    /**
     * 更新当日销售额
     */
    private void updateDailySales(Order order) {
        try {
            // 使用订单完成时间作为统计日期
            String dateKey = order.getCompleteTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String hashKey = "statistics:sales:daily:" + dateKey;

            // 增加销售额
            redisService.hincrByFloat(hashKey, "sales", order.getActualAmount().doubleValue());

            // 增加订单数
            redisService.hincrBy(hashKey, "orders", 1);

            // 更新最后更新时间
            redisService.hset(hashKey, "lastUpdate", String.valueOf(System.currentTimeMillis()));

            // 设置过期时间（30天）
            redisService.expire(hashKey, 2592000);

            logger.debug("Updated daily sales: date={}, amount={}", dateKey, order.getActualAmount());
        } catch (Exception e) {
            logger.error("Failed to update daily sales for order: {}", order.getOrderId(), e);
        }
    }

    /**
     * 更新商品销量统计
     */
    private void updateProductSalesStatistics(Order order) {
        try {
            if (order.getItems() != null) {
                for (Order.OrderItem item : order.getItems()) {
                    String productId = item.getProductId();
                    int quantity = item.getQuantity();

                    // 更新销量排行榜
                    redisService.zincrby("statistics:product:sales:rank", quantity, productId);

                    // 更新商品销量详情
                    String detailKey = "statistics:product:sales:" + productId;
                    redisService.hincrBy(detailKey, "totalSales", quantity);
                    redisService.hset(detailKey, "lastUpdate", String.valueOf(System.currentTimeMillis()));

                    // 设置商品名称（如果还没有设置）
                    if (redisService.hget(detailKey, "name") == null) {
                        try {
                            Product product = productService.getProduct(productId);
                            if (product != null) {
                                redisService.hset(detailKey, "name", product.getName());
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to get product name for statistics: {}", productId, e);
                        }
                    }

                    logger.debug("Updated product sales: productId={}, quantity={}", productId, quantity);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to update product sales statistics for order: {}", order.getOrderId(), e);
        }
    }

    /**
     * 缓存总销售额
     */
    private void cacheTotalSales(TotalSalesResponse response) {
        try {
            redisService.setString("statistics:sales:total", response.getTotalSales().toString());
            redisService.setString("statistics:sales:total:count", String.valueOf(response.getCompletedOrders()));
            redisService.setString("statistics:sales:total:update", String.valueOf(System.currentTimeMillis()));

            logger.info("Cached total sales to Redis: totalSales={}, completedOrders={}",
                response.getTotalSales(), response.getCompletedOrders());
        } catch (Exception e) {
            logger.error("Failed to cache total sales", e);
        }
    }

    /**
     * 解析当日销售额响应
     */
    private DailySalesResponse parseDailySalesResponse(String dateKey, Map<String, String> dailyStats) {
        try {
            String salesStr = dailyStats.get("sales");
            String ordersStr = dailyStats.get("orders");
            String lastUpdateStr = dailyStats.get("lastUpdate");

            BigDecimal dailySales = salesStr != null ? new BigDecimal(salesStr) : BigDecimal.ZERO;
            Integer orderCount = ordersStr != null ? Integer.parseInt(ordersStr) : 0;

            // 计算平均客单价
            BigDecimal averageOrderValue = orderCount > 0
                ? dailySales.divide(new BigDecimal(orderCount), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

            LocalDateTime lastUpdate = lastUpdateStr != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(lastUpdateStr)), ZoneId.systemDefault())
                : LocalDateTime.now();

            return new DailySalesResponse(dateKey, dailySales, orderCount, averageOrderValue, lastUpdate);
        } catch (Exception e) {
            logger.error("Failed to parse daily sales response", e);
            return createEmptyDailyResponse(dateKey);
        }
    }

    /**
     * 创建空的当日销售额响应
     */
    private DailySalesResponse createEmptyDailyResponse(String dateKey) {
        return new DailySalesResponse(dateKey, BigDecimal.ZERO, 0, BigDecimal.ZERO, LocalDateTime.now());
    }
}
