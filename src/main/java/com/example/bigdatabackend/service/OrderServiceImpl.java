package com.example.bigdatabackend.service;

import com.example.bigdatabackend.constants.OrderConstants;
import com.example.bigdatabackend.dto.*;
import com.example.bigdatabackend.exception.EmptyCartException;
import com.example.bigdatabackend.exception.OrderCreationException;
import com.example.bigdatabackend.model.Order;
import com.example.bigdatabackend.model.OrderStatus;
import com.example.bigdatabackend.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private OrderHBaseService orderHBaseService;

    @Autowired
    private IdGenerator idGenerator;

    /**
     * 从购物车创建订单
     */
    @Override
    @Transactional
    public OrderDetailDto createOrderFromCart() {
        logger.info("Starting to create order from cart for default user");

        // 1. 获取购物车选中商品
        CartResponse cart = cartService.getCart();
        List<CartItemDto> selectedItems = cart.getItems().stream()
                .filter(CartItemDto::getSelected)
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            logger.warn("No selected items in cart");
            throw new EmptyCartException();
        }

        logger.debug("Found {} selected items in cart", selectedItems.size());

        // 2. 预检查库存
        validateStock(selectedItems);

        // 3. 计算订单金额
        BigDecimal totalAmount = calculateTotalAmount(selectedItems);
        logger.debug("Calculated total amount: {}", totalAmount);

        // 4. 生成订单号
        String orderId = idGenerator.generateOrderId();
        logger.debug("Generated order ID: {}", orderId);

        // 5. 扣减库存
        Map<String, Integer> deductedStock = new HashMap<>();
        try {
            deductStock(selectedItems, deductedStock);

            // 6. 创建订单记录
            Order order = buildOrder(orderId, selectedItems, totalAmount);
            orderHBaseService.saveOrder(order);

            // 7. 清空购物车已下单商品
            List<String> orderedProductIds = selectedItems.stream()
                    .map(CartItemDto::getProductId)
                    .collect(Collectors.toList());
            cartService.removeItems(orderedProductIds);

            logger.info("Successfully created order: {}", orderId);

            // 8. 返回订单详情
            return convertToOrderDetailDto(order);

        } catch (Exception e) {
            // 回滚库存
            logger.error("Failed to create order, rolling back stock", e);
            rollbackStock(deductedStock);
            throw new OrderCreationException(e.getMessage(), e);
        }
    }

    /**
     * 查询订单列表
     */
    @Override
    public OrderListResponse getOrderList(OrderListQueryRequest request) {
        logger.info("Getting order list: status={}, page={}, size={}",
                request.getStatus(), request.getPage(), request.getSize());

        try {
            // 从HBase查询订单
            List<Order> orders = orderHBaseService.getOrders(
                    request.getStatus(),
                    request.getPage(),
                    request.getSize()
            );

            // 转换为DTO
            List<OrderSummaryDto> summaryList = orders.stream()
                    .map(this::convertToOrderSummaryDto)
                    .collect(Collectors.toList());

            // 构建响应
            OrderListResponse response = new OrderListResponse();
            response.setOrders(summaryList);

            // 构建分页信息（简化实现，实际应该查询总数）
            OrderListResponse.PaginationDto pagination = new OrderListResponse.PaginationDto(
                    request.getPage(),
                    request.getSize(),
                    (long) summaryList.size()
            );
            response.setPagination(pagination);

            logger.info("Retrieved {} orders", summaryList.size());
            return response;

        } catch (Exception e) {
            logger.error("Failed to get order list", e);
            throw new OrderCreationException("查询订单列表失败", e);
        }
    }

    /**
     * 预检查库存
     */
    private void validateStock(List<CartItemDto> items) {
        logger.debug("Validating stock for {} items", items.size());

        for (CartItemDto item : items) {
            Integer stock = redisService.getStock(item.getProductId());
            if (stock == null || stock < item.getQuantity()) {
                logger.warn("Insufficient stock for product: {}, requested: {}, available: {}",
                        item.getProductId(), item.getQuantity(), stock);
                throw new IllegalArgumentException(
                        String.format("商品 %s 库存不足，需要 %d 件，可用 %d 件",
                                item.getProductName(), item.getQuantity(), stock != null ? stock : 0)
                );
            }
        }

        logger.debug("Stock validation passed");
    }

    /**
     * 计算订单总金额
     */
    private BigDecimal calculateTotalAmount(List<CartItemDto> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 扣减库存
     */
    private void deductStock(List<CartItemDto> items, Map<String, Integer> deductedStock) {
        logger.debug("Deducting stock for {} items", items.size());

        for (CartItemDto item : items) {
            boolean success = redisService.deductStock(item.getProductId(), item.getQuantity());

            if (!success) {
                logger.error("Failed to deduct stock for product: {}", item.getProductId());
                throw new IllegalArgumentException(
                        String.format("商品 %s 库存扣减失败", item.getProductName())
                );
            }

            // 记录已扣减的库存，用于回滚
            deductedStock.put(item.getProductId(), item.getQuantity());
            logger.debug("Successfully deducted {} units of stock for product: {}",
                    item.getQuantity(), item.getProductId());
        }

        logger.debug("Stock deduction completed");
    }

    /**
     * 回滚库存
     */
    private void rollbackStock(Map<String, Integer> deductedStock) {
        if (deductedStock.isEmpty()) {
            return;
        }

        logger.warn("Rolling back stock for {} products", deductedStock.size());

        for (Map.Entry<String, Integer> entry : deductedStock.entrySet()) {
            try {
                redisService.incrStock(entry.getKey(), entry.getValue());
                logger.debug("Rolled back {} units of stock for product: {}",
                        entry.getValue(), entry.getKey());
            } catch (Exception e) {
                logger.error("Failed to rollback stock for product: {}", entry.getKey(), e);
            }
        }
    }

    /**
     * 构建订单对象
     */
    private Order buildOrder(String orderId, List<CartItemDto> items, BigDecimal totalAmount) {
        Order order = new Order();

        // 基本信息
        order.setOrderId(orderId);
        order.setUserId(OrderConstants.DEFAULT_USER_ID);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO); // 暂无优惠
        order.setActualAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreateTime(LocalDateTime.now());

        // 收货信息（使用默认信息）
        order.setReceiver(OrderConstants.DEFAULT_RECEIVER);
        order.setPhone(OrderConstants.DEFAULT_PHONE);
        order.setAddress(OrderConstants.DEFAULT_ADDRESS);
        order.setPostcode(OrderConstants.DEFAULT_POSTCODE);

        // 商品明细
        List<Order.OrderItem> orderItems = items.stream()
                .map(this::convertToOrderItem)
                .collect(Collectors.toList());
        order.setItems(orderItems);

        return order;
    }

    /**
     * 转换购物车项为订单项
     */
    private Order.OrderItem convertToOrderItem(CartItemDto cartItem) {
        Order.OrderItem orderItem = new Order.OrderItem();
        orderItem.setProductId(cartItem.getProductId());
        orderItem.setProductName(cartItem.getProductName());
        orderItem.setCategory(cartItem.getCategory());
        orderItem.setBrand(cartItem.getBrand());
        orderItem.setPrice(cartItem.getPrice());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setTotalAmount(cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
        return orderItem;
    }

    /**
     * 转换Order为OrderDetailDto
     */
    private OrderDetailDto convertToOrderDetailDto(Order order) {
        OrderDetailDto dto = new OrderDetailDto();
        dto.setOrderId(order.getOrderId());
        dto.setUserId(order.getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setActualAmount(order.getActualAmount());
        dto.setStatus(order.getStatus().name());
        dto.setCreateTime(order.getCreateTime());
        dto.setPayTime(order.getPayTime());

        // 转换商品明细
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(this::convertToOrderItemDto)
                .collect(Collectors.toList());
        dto.setItems(itemDtos);

        // 转换收货地址
        OrderAddressDto addressDto = new OrderAddressDto(
                order.getReceiver(),
                order.getPhone(),
                order.getAddress(),
                order.getPostcode()
        );
        dto.setAddress(addressDto);

        return dto;
    }

    /**
     * 转换Order为OrderSummaryDto
     */
    private OrderSummaryDto convertToOrderSummaryDto(Order order) {
        OrderSummaryDto dto = new OrderSummaryDto();
        dto.setOrderId(order.getOrderId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setActualAmount(order.getActualAmount());
        dto.setStatus(order.getStatus().name());
        dto.setCreateTime(order.getCreateTime());
        dto.setItemCount(order.getItems() != null ? order.getItems().size() : 0);
        return dto;
    }

    /**
     * 转换OrderItem为OrderItemDto
     */
    private OrderItemDto convertToOrderItemDto(Order.OrderItem orderItem) {
        OrderItemDto dto = new OrderItemDto();
        dto.setProductId(orderItem.getProductId());
        dto.setProductName(orderItem.getProductName());
        dto.setCategory(orderItem.getCategory());
        dto.setBrand(orderItem.getBrand());
        dto.setPrice(orderItem.getPrice());
        dto.setQuantity(orderItem.getQuantity());
        dto.setTotalAmount(orderItem.getTotalAmount());
        return dto;
    }
}
