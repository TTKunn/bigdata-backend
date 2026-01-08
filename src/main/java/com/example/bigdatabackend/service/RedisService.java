package com.example.bigdatabackend.service;

import com.example.bigdatabackend.model.Product;
import com.example.bigdatabackend.model.ProductStatus;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存服务类
 */
@Service
public class RedisService {

    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);

    @Autowired
    private JedisPool jedisPool;

    /**
     * 缓存商品基本信息
     */
    public void cacheProduct(Product product) {
        if (product == null || product.getId() == null) {
            logger.warn("Cannot cache product: product or product ID is null");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "product:cache:" + product.getId();
            Gson gson = new Gson();

            Map<String, String> data = new HashMap<>();
            data.put("name", product.getName() != null ? product.getName() : "");
            data.put("category", product.getCategory() != null ? product.getCategory() : "");
            data.put("brand", product.getBrand() != null ? product.getBrand() : "");
            data.put("price", product.getPrice() != null ? product.getPrice().toString() : "0.00");
            data.put("status", product.getStatus() != null ? product.getStatus().getCode() : ProductStatus.ACTIVE.getCode());

            // 缓存图片信息（JSON格式）
            if (product.getImage() != null) {
                data.put("image", gson.toJson(product.getImage()));
            }

            jedis.hmset(key, data);
            jedis.expire(key, 300); // 5分钟过期

            logger.debug("Cached product info for productId: {}", product.getId());
        } catch (Exception e) {
            logger.error("Failed to cache product: {}", product.getId(), e);
        }
    }

    /**
     * 获取缓存的商品信息
     */
    public Map<String, String> getCachedProduct(String productId) {
        if (productId == null) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "product:cache:" + productId;
            Map<String, String> data = jedis.hgetAll(key);

            if (data != null && !data.isEmpty()) {
                logger.debug("Retrieved cached product info for productId: {}", productId);
                return data;
            }
        } catch (Exception e) {
            logger.error("Failed to get cached product: {}", productId, e);
        }

        return null;
    }

    /**
     * 设置商品库存
     */
    public void setStock(String productId, int stock) {
        if (productId == null) {
            logger.warn("Cannot set stock: productId is null");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "stock:" + productId;
            jedis.set(key, String.valueOf(stock));
            jedis.expire(key, 3600); // 1小时过期

            logger.debug("Set stock for productId: {} to {}", productId, stock);
        } catch (Exception e) {
            logger.error("Failed to set stock for productId: {}", productId, e);
        }
    }

    /**
     * 获取商品库存
     */
    public Integer getStock(String productId) {
        if (productId == null) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "stock:" + productId;
            String stockStr = jedis.get(key);

            if (stockStr != null) {
                return Integer.valueOf(stockStr);
            }
        } catch (Exception e) {
            logger.error("Failed to get stock for productId: {}", productId, e);
        }

        return null;
    }

    /**
     * 扣减库存（原子操作）
     */
    public boolean deductStock(String productId, int quantity) {
        if (productId == null || quantity <= 0) {
            logger.warn("Invalid parameters for stock deduction: productId={}, quantity={}", productId, quantity);
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "stock:" + productId;

            // 使用Lua脚本进行原子扣减
            String script = """
                local stock = redis.call('get', KEYS[1])
                if not stock then return -1 end
                stock = tonumber(stock)
                if stock < tonumber(ARGV[1]) then return -2 end
                redis.call('decrby', KEYS[1], ARGV[1])
                return stock - tonumber(ARGV[1])
                """;

            Object result = jedis.eval(script, 1, key, String.valueOf(quantity));

            if (result instanceof Long) {
                long newStock = (Long) result;
                if (newStock >= 0) {
                    logger.debug("Successfully deducted {} from stock for productId: {}, new stock: {}", quantity, productId, newStock);
                    return true;
                } else if (newStock == -2) {
                    logger.warn("Insufficient stock for productId: {}, requested: {}, available: {}", productId, quantity, getStock(productId));
                    return false;
                }
            }

            logger.error("Unexpected result from stock deduction script: {}", result);
            return false;
        } catch (Exception e) {
            logger.error("Failed to deduct stock for productId: {}", productId, e);
            return false;
        }
    }

    /**
     * 删除商品缓存
     */
    public void deleteProductCache(String productId) {
        if (productId == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "product:cache:" + productId;
            jedis.del(key);
            logger.debug("Deleted cache for productId: {}", productId);
        } catch (Exception e) {
            logger.error("Failed to delete product cache for productId: {}", productId, e);
        }
    }

    /**
     * 删除库存缓存
     */
    public void deleteStockCache(String productId) {
        if (productId == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "stock:" + productId;
            jedis.del(key);
            logger.debug("Deleted stock cache for productId: {}", productId);
        } catch (Exception e) {
            logger.error("Failed to delete stock cache for productId: {}", productId, e);
        }
    }

    /**
     * 检查Redis连接
     */
    public boolean checkConnection() {
        try (Jedis jedis = jedisPool.getResource()) {
            String pong = jedis.ping();
            return "PONG".equals(pong);
        } catch (Exception e) {
            logger.error("Redis connection check failed", e);
            return false;
        }
    }

    /**
     * 批量删除商品缓存
     */
    public void deleteProductCaches(String... productIds) {
        if (productIds == null || productIds.length == 0) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String[] keys = new String[productIds.length];
            for (int i = 0; i < productIds.length; i++) {
                keys[i] = "product:cache:" + productIds[i];
            }
            jedis.del(keys);
            logger.debug("Deleted {} product caches", productIds.length);
        } catch (Exception e) {
            logger.error("Failed to delete product caches", e);
        }
    }

    // ==================== 购物车相关方法 ====================

    /**
     * 设置Hash字段值
     */
    public void hset(String key, String field, String value) {
        if (key == null || field == null || value == null) {
            logger.warn("Cannot hset: key, field or value is null");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(key, field, value);
            logger.debug("Set hash field: key={}, field={}", key, field);
        } catch (Exception e) {
            logger.error("Failed to hset: key={}, field={}", key, field, e);
        }
    }

    /**
     * 获取Hash字段值
     */
    public String hget(String key, String field) {
        if (key == null || field == null) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.hget(key, field);
            logger.debug("Get hash field: key={}, field={}, found={}", key, field, value != null);
            return value;
        } catch (Exception e) {
            logger.error("Failed to hget: key={}, field={}", key, field, e);
            return null;
        }
    }

    /**
     * 获取Hash所有字段
     */
    public Map<String, String> hgetAll(String key) {
        if (key == null) {
            return new HashMap<>();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> data = jedis.hgetAll(key);
            logger.debug("Get all hash fields: key={}, size={}", key, data != null ? data.size() : 0);
            return data != null ? data : new HashMap<>();
        } catch (Exception e) {
            logger.error("Failed to hgetAll: key={}", key, e);
            return new HashMap<>();
        }
    }

    /**
     * 删除Hash字段
     */
    public void hdel(String key, String... fields) {
        if (key == null || fields == null || fields.length == 0) {
            logger.warn("Cannot hdel: key or fields is null/empty");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(key, fields);
            logger.debug("Deleted hash fields: key={}, fields={}", key, fields.length);
        } catch (Exception e) {
            logger.error("Failed to hdel: key={}", key, e);
        }
    }

    /**
     * 删除整个Key
     */
    public void del(String key) {
        if (key == null) {
            logger.warn("Cannot del: key is null");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
            logger.debug("Deleted key: {}", key);
        } catch (Exception e) {
            logger.error("Failed to del: key={}", key, e);
        }
    }

    /**
     * 设置Key过期时间
     */
    public void expire(String key, int seconds) {
        if (key == null || seconds <= 0) {
            logger.warn("Cannot expire: key is null or seconds <= 0");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.expire(key, seconds);
            logger.debug("Set expiration for key: {}, seconds={}", key, seconds);
        } catch (Exception e) {
            logger.error("Failed to expire: key={}", key, e);
        }
    }

    // ==================== 订单相关方法 ====================

    /**
     * 原子递增（用于订单号序列号生成）
     */
    public Long incr(String key) {
        if (key == null) {
            logger.warn("Cannot incr: key is null");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Long value = jedis.incr(key);
            logger.debug("Incremented key: {}, new value={}", key, value);
            return value;
        } catch (Exception e) {
            logger.error("Failed to incr: key={}", key, e);
            return null;
        }
    }

    /**
     * 增加库存（用于回滚）
     */
    public void incrStock(String productId, int quantity) {
        if (productId == null || quantity <= 0) {
            logger.warn("Invalid parameters for stock increment: productId={}, quantity={}", productId, quantity);
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "stock:" + productId;
            jedis.incrBy(key, quantity);
            logger.debug("Incremented stock for productId: {} by {}", productId, quantity);
        } catch (Exception e) {
            logger.error("Failed to increment stock for productId: {}", productId, e);
        }
    }

    /**
     * 回库库存（取消订单时使用）
     * 与incrStock功能相同，但语义更清晰
     */
    public void restoreStock(String productId, int quantity) {
        incrStock(productId, quantity);
        logger.info("Restored {} units of stock for product: {}", quantity, productId);
    }

    // ==================== 订单状态计数器相关方法 ====================

    /**
     * 增加订单状态计数
     */
    public void incrementOrderStatusCount(String status) {
        if (status == null) {
            logger.warn("Cannot increment order status count: status is null");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 增加总订单数
            jedis.incr("order:count:total");

            // 增加状态计数
            jedis.incr("order:count:status:" + status);

            // 增加每日订单数
            jedis.incr("order:count:daily:" + today);

            // 增加每日状态计数
            jedis.incr("order:count:daily:" + today + ":" + status);

            logger.debug("Incremented order status count for status: {}", status);
        } catch (Exception e) {
            logger.error("Failed to increment order status count for status: {}", status, e);
        }
    }

    /**
     * 减少订单状态计数
     */
    public void decrementOrderStatusCount(String status) {
        if (status == null) {
            logger.warn("Cannot decrement order status count: status is null");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 减少状态计数
            jedis.decr("order:count:status:" + status);

            // 减少每日状态计数
            jedis.decr("order:count:daily:" + today + ":" + status);

            logger.debug("Decremented order status count for status: {}", status);
        } catch (Exception e) {
            logger.error("Failed to decrement order status count for status: {}", status, e);
        }
    }

    /**
     * 更新订单状态计数（状态转换时使用）
     */
    public void updateOrderStatusCount(String oldStatus, String newStatus) {
        if (oldStatus == null || newStatus == null) {
            logger.warn("Cannot update order status count: oldStatus or newStatus is null");
            return;
        }

        decrementOrderStatusCount(oldStatus);
        incrementOrderStatusCount(newStatus);
        logger.info("Updated order status count from {} to {}", oldStatus, newStatus);
    }

    /**
     * 获取订单状态统计
     */
    public Map<String, Long> getOrderStatusStatistics() {
        Map<String, Long> stats = new HashMap<>();

        try (Jedis jedis = jedisPool.getResource()) {
            // 获取总订单数
            String total = jedis.get("order:count:total");
            stats.put("total", total != null ? Long.parseLong(total) : 0L);

            // 获取各状态订单数
            String[] statuses = {"PENDING_PAYMENT", "PAID", "COMPLETED", "CANCELLED"};
            for (String status : statuses) {
                String count = jedis.get("order:count:status:" + status);
                stats.put(status, count != null ? Long.parseLong(count) : 0L);
            }

            logger.debug("Retrieved order status statistics: {}", stats);
        } catch (Exception e) {
            logger.error("Failed to get order status statistics", e);
        }

        return stats;
    }

    // ==================== 销售统计相关方法 ====================

    /**
     * 设置字符串值
     */
    public void setString(String key, String value) {
        if (key == null || value == null) {
            logger.warn("Cannot setString: key or value is null");
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(key, value);
            logger.debug("Set string: key={}", key);
        } catch (Exception e) {
            logger.error("Failed to setString: key={}", key, e);
        }
    }

    /**
     * 获取字符串值
     */
    public String getString(String key) {
        if (key == null) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key);
            logger.debug("Get string: key={}, found={}", key, value != null);
            return value;
        } catch (Exception e) {
            logger.error("Failed to getString: key={}", key, e);
            return null;
        }
    }

    /**
     * 浮点数原子增加（用于销售额统计）
     */
    public Double incrByFloat(String key, double increment) {
        if (key == null) {
            logger.warn("Cannot incrByFloat: key is null");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Double newValue = jedis.incrByFloat(key, increment);
            logger.debug("Incremented float: key={}, increment={}, newValue={}", key, increment, newValue);
            return newValue;
        } catch (Exception e) {
            logger.error("Failed to incrByFloat: key={}", key, e);
            return null;
        }
    }

    /**
     * Hash字段浮点数原子增加
     */
    public Double hincrByFloat(String key, String field, double increment) {
        if (key == null || field == null) {
            logger.warn("Cannot hincrByFloat: key or field is null");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Double newValue = jedis.hincrByFloat(key, field, increment);
            logger.debug("Incremented hash float: key={}, field={}, increment={}, newValue={}",
                key, field, increment, newValue);
            return newValue;
        } catch (Exception e) {
            logger.error("Failed to hincrByFloat: key={}, field={}", key, field, e);
            return null;
        }
    }

    /**
     * Hash字段整数原子增加
     */
    public Long hincrBy(String key, String field, long increment) {
        if (key == null || field == null) {
            logger.warn("Cannot hincrBy: key or field is null");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Long newValue = jedis.hincrBy(key, field, increment);
            logger.debug("Incremented hash: key={}, field={}, increment={}, newValue={}",
                key, field, increment, newValue);
            return newValue;
        } catch (Exception e) {
            logger.error("Failed to hincrBy: key={}, field={}", key, field, e);
            return null;
        }
    }

    /**
     * 有序集合成员分数增加
     */
    public Double zincrby(String key, double increment, String member) {
        if (key == null || member == null) {
            logger.warn("Cannot zincrby: key or member is null");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Double newScore = jedis.zincrby(key, increment, member);
            logger.debug("Incremented sorted set: key={}, member={}, increment={}, newScore={}",
                key, member, increment, newScore);
            return newScore;
        } catch (Exception e) {
            logger.error("Failed to zincrby: key={}, member={}", key, member, e);
            return null;
        }
    }

    /**
     * 获取有序集合指定范围成员（按分数降序）
     */
    public java.util.Set<String> zrevrange(String key, long start, long end) {
        if (key == null) {
            return new java.util.HashSet<>();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // Jedis的zrevrange返回的是LinkedHashSet（实现了Set接口）
            java.util.Set<String> members = new java.util.LinkedHashSet<>(jedis.zrevrange(key, start, end));
            logger.debug("Get sorted set range: key={}, start={}, end={}, size={}",
                key, start, end, members.size());
            return members;
        } catch (Exception e) {
            logger.error("Failed to zrevrange: key={}", key, e);
            return new java.util.HashSet<>();
        }
    }

    /**
     * 列表左侧推入（用于待更新队列）
     */
    public Long lpush(String key, String... values) {
        if (key == null || values == null || values.length == 0) {
            logger.warn("Cannot lpush: key or values is null/empty");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Long length = jedis.lpush(key, values);
            logger.debug("Pushed to list: key={}, count={}, newLength={}", key, values.length, length);
            return length;
        } catch (Exception e) {
            logger.error("Failed to lpush: key={}", key, e);
            return null;
        }
    }

    /**
     * 列表右侧弹出（用于消费待更新队列）
     */
    public String rpop(String key) {
        if (key == null) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.rpop(key);
            logger.debug("Popped from list: key={}, value={}", key, value);
            return value;
        } catch (Exception e) {
            logger.error("Failed to rpop: key={}", key, e);
            return null;
        }
    }

    /**
     * 集合添加成员（用于去重）
     */
    public Long sadd(String key, String... members) {
        if (key == null || members == null || members.length == 0) {
            logger.warn("Cannot sadd: key or members is null/empty");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Long count = jedis.sadd(key, members);
            logger.debug("Added to set: key={}, count={}", key, count);
            return count;
        } catch (Exception e) {
            logger.error("Failed to sadd: key={}", key, e);
            return null;
        }
    }

    /**
     * 检查集合成员是否存在（用于去重检查）
     */
    public Boolean sismember(String key, String member) {
        if (key == null || member == null) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Boolean exists = jedis.sismember(key, member);
            logger.debug("Check set member: key={}, member={}, exists={}", key, member, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Failed to sismember: key={}, member={}", key, member, e);
            return false;
        }
    }
}
