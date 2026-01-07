package com.example.bigdatabackend.service;

import com.example.bigdatabackend.dto.CartItemDto;
import com.example.bigdatabackend.dto.CartResponse;
import com.example.bigdatabackend.model.CartItem;
import com.example.bigdatabackend.model.Product;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务实现类
 */
@Service
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private RedisService redisService;

    @Autowired
    private CartHBaseService cartHBaseService;

    @Autowired
    private ProductService productService;

    private static final String DEFAULT_USER_ID = "000000000001";
    private static final String CART_KEY_PREFIX = "cart:";
    private static final int CART_EXPIRE_SECONDS = 604800; // 7天

    private final Gson gson = new Gson();

    /**
     * 添加商品到购物车（同步双写Redis和HBase）
     */
    @Override
    public void addItem(String productId, Integer quantity) {
        logger.info("Adding item to cart: productId={}, quantity={}", productId, quantity);

        // 1. 验证商品存在性
        Product product = productService.getProduct(productId);
        if (product == null) {
            logger.warn("Product not found: {}", productId);
            throw new IllegalArgumentException("商品不存在");
        }

        // 2. 检查库存（仅校验，不扣减）
        Integer stock = redisService.getStock(productId);
        if (stock == null || stock < quantity) {
            logger.warn("Insufficient stock for productId: {}, requested: {}, available: {}",
                    productId, quantity, stock);
            throw new IllegalArgumentException("商品库存不足");
        }

        // 3. 获取或创建购物车项
        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;
        String field = productId;  // 直接使用商品ID作为field

        String existingItem = redisService.hget(cartKey, field);
        CartItem cartItem;

        if (existingItem != null) {
            // 商品已存在，增加数量
            cartItem = parseCartItem(existingItem);
            int newQuantity = cartItem.getQuantity() + quantity;

            // 检查新数量是否超过库存
            if (stock < newQuantity) {
                logger.warn("Insufficient stock after adding: productId={}, newQuantity={}, available={}",
                        productId, newQuantity, stock);
                throw new IllegalArgumentException("商品库存不足");
            }
            cartItem.setQuantity(newQuantity);
            logger.debug("Updated existing cart item: productId={}, newQuantity={}", productId, newQuantity);
        } else {
            // 新增商品
            cartItem = new CartItem();
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setAddTime(System.currentTimeMillis());
            cartItem.setSelected(true); // 默认选中
            logger.debug("Created new cart item: productId={}, quantity={}", productId, quantity);
        }

        // 4. 同步写入Redis
        redisService.hset(cartKey, field, serializeCartItem(cartItem));
        redisService.expire(cartKey, CART_EXPIRE_SECONDS);

        // 5. 同步写入HBase
        try {
            List<CartItem> allItems = getAllCartItemsFromRedis();
            cartHBaseService.saveCart(DEFAULT_USER_ID, allItems);
            logger.info("Successfully added item and synced to HBase: productId={}, quantity={}", productId, quantity);
        } catch (Exception e) {
            // HBase写入失败，回滚Redis
            logger.error("Failed to save cart to HBase, rolling back Redis: productId={}", productId, e);
            redisService.hdel(cartKey, field);
            throw new RuntimeException("购物车保存失败", e);
        }
    }

    /**
     * 查询购物车（缓存穿透：优先Redis，未命中则从HBase加载）
     */
    @Override
    public CartResponse getCart() {
        logger.info("Getting cart for default user");

        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;
        Map<String, String> cartData = redisService.hgetAll(cartKey);

        // 1. Redis缓存命中
        if (cartData != null && !cartData.isEmpty()) {
            logger.debug("Cart found in Redis");
            return buildCartResponse(cartData);
        }

        // 2. Redis未命中，从HBase加载
        logger.debug("Cart not found in Redis, loading from HBase");
        try {
            List<CartItem> items = cartHBaseService.loadCart(DEFAULT_USER_ID);

            if (!items.isEmpty()) {
                // 3. 回填到Redis
                for (CartItem item : items) {
                    redisService.hset(cartKey, item.getProductId(), serializeCartItem(item));
                }
                redisService.expire(cartKey, CART_EXPIRE_SECONDS);
                logger.info("Cart loaded from HBase and cached to Redis: itemCount={}", items.size());

                // 构建响应
                return buildCartResponseFromItems(items);
            }
        } catch (Exception e) {
            logger.error("Failed to load cart from HBase", e);
        }

        // 4. 返回空购物车
        logger.debug("Cart is empty for user: {}", DEFAULT_USER_ID);
        return createEmptyCartResponse();
    }

    /**
     * 从Redis数据构建购物车响应
     */
    private CartResponse buildCartResponse(Map<String, String> cartData) {
        CartResponse response = new CartResponse();
        response.setUserId(DEFAULT_USER_ID);

        List<CartItemDto> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 遍历购物车项
        for (Map.Entry<String, String> entry : cartData.entrySet()) {
            String productId = entry.getKey();
            String itemJson = entry.getValue();

            try {
                CartItem cartItem = parseCartItem(itemJson);

                // 获取商品详细信息
                Product product = productService.getProduct(productId);
                if (product == null) {
                    logger.warn("Product not found in cart: {}, skipping", productId);
                    continue;
                }

                // 构建CartItemDto
                CartItemDto itemDto = new CartItemDto();
                itemDto.setProductId(productId);
                itemDto.setProductName(product.getName());
                itemDto.setCategory(product.getCategory());
                itemDto.setBrand(product.getBrand());
                itemDto.setPrice(product.getPrice());
                itemDto.setQuantity(cartItem.getQuantity());
                itemDto.setAddTime(cartItem.getAddTime());
                itemDto.setSelected(cartItem.getSelected());

                items.add(itemDto);

                // 计算总数量和总金额
                totalQuantity += cartItem.getQuantity();
                if (product.getPrice() != null) {
                    BigDecimal itemAmount = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
                    totalAmount = totalAmount.add(itemAmount);
                }

            } catch (Exception e) {
                logger.error("Failed to parse cart item: productId={}", productId, e);
            }
        }

        response.setItems(items);
        response.setTotalQuantity(totalQuantity);
        response.setTotalAmount(totalAmount);

        logger.info("Successfully retrieved cart from Redis: items={}, totalQuantity={}, totalAmount={}",
                items.size(), totalQuantity, totalAmount);

        return response;
    }

    /**
     * 从CartItem列表构建购物车响应
     */
    private CartResponse buildCartResponseFromItems(List<CartItem> items) {
        CartResponse response = new CartResponse();
        response.setUserId(DEFAULT_USER_ID);

        List<CartItemDto> itemDtos = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : items) {
            try {
                // 获取商品详细信息
                Product product = productService.getProduct(cartItem.getProductId());
                if (product == null) {
                    logger.warn("Product not found: {}, skipping", cartItem.getProductId());
                    continue;
                }

                // 构建CartItemDto
                CartItemDto itemDto = new CartItemDto();
                itemDto.setProductId(cartItem.getProductId());
                itemDto.setProductName(product.getName());
                itemDto.setCategory(product.getCategory());
                itemDto.setBrand(product.getBrand());
                itemDto.setPrice(product.getPrice());
                itemDto.setQuantity(cartItem.getQuantity());
                itemDto.setAddTime(cartItem.getAddTime());
                itemDto.setSelected(cartItem.getSelected());

                itemDtos.add(itemDto);

                // 计算总数量和总金额
                totalQuantity += cartItem.getQuantity();
                if (product.getPrice() != null) {
                    BigDecimal itemAmount = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
                    totalAmount = totalAmount.add(itemAmount);
                }

            } catch (Exception e) {
                logger.error("Failed to process cart item: productId={}", cartItem.getProductId(), e);
            }
        }

        response.setItems(itemDtos);
        response.setTotalQuantity(totalQuantity);
        response.setTotalAmount(totalAmount);

        return response;
    }

    /**
     * 创建空购物车响应
     */
    private CartResponse createEmptyCartResponse() {
        CartResponse response = new CartResponse();
        response.setUserId(DEFAULT_USER_ID);
        response.setItems(new ArrayList<>());
        response.setTotalQuantity(0);
        response.setTotalAmount(BigDecimal.ZERO);
        return response;
    }

    /**
     * 序列化购物车项
     */
    private String serializeCartItem(CartItem item) {
        return gson.toJson(item);
    }

    /**
     * 反序列化购物车项
     */
    private CartItem parseCartItem(String json) {
        return gson.fromJson(json, CartItem.class);
    }

    /**
     * 更新商品数量（同步双写Redis和HBase）
     */
    @Override
    public void updateItemQuantity(String productId, Integer quantity) {
        logger.info("Updating item quantity: productId={}, quantity={}", productId, quantity);

        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;
        String field = productId;

        // 1. 检查商品是否在购物车中
        String existingItem = redisService.hget(cartKey, field);
        if (existingItem == null) {
            logger.warn("Product not found in cart: {}", productId);
            throw new IllegalArgumentException("购物车中不存在该商品");
        }

        // 2. 检查库存
        Integer stock = redisService.getStock(productId);
        if (stock == null || stock < quantity) {
            logger.warn("Insufficient stock for productId: {}, requested: {}, available: {}",
                    productId, quantity, stock);
            throw new IllegalArgumentException("商品库存不足");
        }

        // 3. 保存旧数据用于回滚
        String oldItemJson = existingItem;

        // 4. 更新数量
        CartItem cartItem = parseCartItem(existingItem);
        cartItem.setQuantity(quantity);

        // 5. 同步写入Redis
        redisService.hset(cartKey, field, serializeCartItem(cartItem));
        redisService.expire(cartKey, CART_EXPIRE_SECONDS);

        // 6. 同步写入HBase
        try {
            List<CartItem> allItems = getAllCartItemsFromRedis();
            cartHBaseService.saveCart(DEFAULT_USER_ID, allItems);
            logger.info("Successfully updated item quantity and synced to HBase: productId={}, quantity={}", productId, quantity);
        } catch (Exception e) {
            // HBase写入失败，回滚Redis
            logger.error("Failed to save cart to HBase, rolling back Redis: productId={}", productId, e);
            redisService.hset(cartKey, field, oldItemJson);
            throw new RuntimeException("购物车更新失败", e);
        }
    }

    /**
     * 删除购物车商品（同步双写Redis和HBase）
     */
    @Override
    public void removeItems(List<String> productIds) {
        logger.info("Removing items from cart: productIds={}", productIds);

        if (productIds == null || productIds.isEmpty()) {
            logger.warn("Product IDs list is empty");
            throw new IllegalArgumentException("商品ID列表不能为空");
        }

        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;

        // 1. 保存旧数据用于回滚
        Map<String, String> oldItems = new java.util.HashMap<>();
        for (String productId : productIds) {
            String oldItem = redisService.hget(cartKey, productId);
            if (oldItem != null) {
                oldItems.put(productId, oldItem);
            }
        }

        // 2. 从Redis删除指定商品
        String[] fields = productIds.toArray(new String[0]);
        redisService.hdel(cartKey, fields);

        // 3. 同步写入HBase
        try {
            List<CartItem> allItems = getAllCartItemsFromRedis();
            cartHBaseService.saveCart(DEFAULT_USER_ID, allItems);
            logger.info("Successfully removed {} items from cart and synced to HBase", productIds.size());
        } catch (Exception e) {
            // HBase写入失败，回滚Redis
            logger.error("Failed to save cart to HBase, rolling back Redis", e);
            for (Map.Entry<String, String> entry : oldItems.entrySet()) {
                redisService.hset(cartKey, entry.getKey(), entry.getValue());
            }
            throw new RuntimeException("购物车删除失败", e);
        }
    }

    /**
     * 清空购物车（同步删除Redis和HBase）
     */
    @Override
    public void clearCart() {
        logger.info("Clearing cart for default user");

        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;

        // 1. 保存旧数据用于回滚
        Map<String, String> oldCartData = redisService.hgetAll(cartKey);

        // 2. 删除Redis
        redisService.del(cartKey);

        // 3. 同步删除HBase
        try {
            cartHBaseService.deleteCart(DEFAULT_USER_ID);
            logger.info("Successfully cleared cart from both Redis and HBase");
        } catch (Exception e) {
            // HBase删除失败，回滚Redis
            logger.error("Failed to delete cart from HBase, rolling back Redis", e);
            if (oldCartData != null && !oldCartData.isEmpty()) {
                for (Map.Entry<String, String> entry : oldCartData.entrySet()) {
                    redisService.hset(cartKey, entry.getKey(), entry.getValue());
                }
                redisService.expire(cartKey, CART_EXPIRE_SECONDS);
            }
            throw new RuntimeException("购物车清空失败", e);
        }
    }

    /**
     * 从Redis获取购物车所有商品（内部方法）
     */
    private List<CartItem> getAllCartItemsFromRedis() {
        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;
        Map<String, String> cartData = redisService.hgetAll(cartKey);

        List<CartItem> items = new ArrayList<>();
        if (cartData != null && !cartData.isEmpty()) {
            for (String json : cartData.values()) {
                try {
                    items.add(parseCartItem(json));
                } catch (Exception e) {
                    logger.error("Failed to parse cart item from Redis", e);
                }
            }
        }
        return items;
    }

    /**
     * 更新商品选中状态（同步双写Redis和HBase）
     */
    @Override
    public void updateItemsSelected(List<String> productIds, Boolean selected) {
        logger.info("Updating items selected status: productIds={}, selected={}", productIds, selected);

        if (productIds == null || productIds.isEmpty()) {
            logger.warn("Product IDs list is empty");
            throw new IllegalArgumentException("商品ID列表不能为空");
        }

        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;

        // 1. 保存旧数据用于回滚
        Map<String, String> oldItems = new java.util.HashMap<>();
        for (String productId : productIds) {
            String oldItem = redisService.hget(cartKey, productId);
            if (oldItem != null) {
                oldItems.put(productId, oldItem);
            } else {
                logger.warn("Product not found in cart: {}", productId);
                throw new IllegalArgumentException("购物车中不存在商品: " + productId);
            }
        }

        // 2. 更新Redis中的选中状态
        for (String productId : productIds) {
            String itemJson = redisService.hget(cartKey, productId);
            CartItem cartItem = parseCartItem(itemJson);
            cartItem.setSelected(selected);
            redisService.hset(cartKey, productId, serializeCartItem(cartItem));
        }
        redisService.expire(cartKey, CART_EXPIRE_SECONDS);

        // 3. 同步写入HBase
        try {
            List<CartItem> allItems = getAllCartItemsFromRedis();
            cartHBaseService.saveCart(DEFAULT_USER_ID, allItems);
            logger.info("Successfully updated {} items selected status and synced to HBase", productIds.size());
        } catch (Exception e) {
            // HBase写入失败，回滚Redis
            logger.error("Failed to save cart to HBase, rolling back Redis", e);
            for (Map.Entry<String, String> entry : oldItems.entrySet()) {
                redisService.hset(cartKey, entry.getKey(), entry.getValue());
            }
            throw new RuntimeException("购物车更新失败", e);
        }
    }
}
