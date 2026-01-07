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
    private ProductService productService;

    private static final String DEFAULT_USER_ID = "000000000001";
    private static final String CART_KEY_PREFIX = "cart:";
    private static final int CART_EXPIRE_SECONDS = 604800; // 7天

    private final Gson gson = new Gson();

    /**
     * 添加商品到购物车
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

        // 4. 保存到Redis
        redisService.hset(cartKey, field, serializeCartItem(cartItem));
        redisService.expire(cartKey, CART_EXPIRE_SECONDS);

        logger.info("Successfully added item to cart: productId={}, quantity={}", productId, quantity);
    }

    /**
     * 查询购物车
     */
    @Override
    public CartResponse getCart() {
        logger.info("Getting cart for default user");

        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;
        Map<String, String> cartData = redisService.hgetAll(cartKey);

        CartResponse response = new CartResponse();
        response.setUserId(DEFAULT_USER_ID);

        if (cartData == null || cartData.isEmpty()) {
            logger.debug("Cart is empty for user: {}", DEFAULT_USER_ID);
            response.setItems(new ArrayList<>());
            response.setTotalQuantity(0);
            response.setTotalAmount(BigDecimal.ZERO);
            return response;
        }

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

        logger.info("Successfully retrieved cart: items={}, totalQuantity={}, totalAmount={}",
                items.size(), totalQuantity, totalAmount);

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
     * 更新商品数量
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

        // 3. 更新数量
        CartItem cartItem = parseCartItem(existingItem);
        cartItem.setQuantity(quantity);

        // 4. 保存到Redis
        redisService.hset(cartKey, field, serializeCartItem(cartItem));
        redisService.expire(cartKey, CART_EXPIRE_SECONDS);

        logger.info("Successfully updated item quantity: productId={}, quantity={}", productId, quantity);
    }

    /**
     * 删除购物车商品
     */
    @Override
    public void removeItems(List<String> productIds) {
        logger.info("Removing items from cart: productIds={}", productIds);

        if (productIds == null || productIds.isEmpty()) {
            logger.warn("Product IDs list is empty");
            throw new IllegalArgumentException("商品ID列表不能为空");
        }

        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;

        // 转换为数组
        String[] fields = productIds.toArray(new String[0]);

        // 删除指定商品
        redisService.hdel(cartKey, fields);

        logger.info("Successfully removed {} items from cart", productIds.size());
    }

    /**
     * 清空购物车
     */
    @Override
    public void clearCart() {
        logger.info("Clearing cart for default user");

        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;

        // 删除整个购物车
        redisService.del(cartKey);

        logger.info("Successfully cleared cart");
    }
}
