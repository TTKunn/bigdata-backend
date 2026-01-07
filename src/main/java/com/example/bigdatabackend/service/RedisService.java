package com.example.bigdatabackend.service;

import com.example.bigdatabackend.model.Product;
import com.example.bigdatabackend.model.ProductStatus;
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

            Map<String, String> data = new HashMap<>();
            data.put("name", product.getName() != null ? product.getName() : "");
            data.put("category", product.getCategory() != null ? product.getCategory() : "");
            data.put("brand", product.getBrand() != null ? product.getBrand() : "");
            data.put("price", product.getPrice() != null ? product.getPrice().toString() : "0.00");
            data.put("status", product.getStatus() != null ? product.getStatus().getCode() : ProductStatus.ACTIVE.getCode());

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
}
