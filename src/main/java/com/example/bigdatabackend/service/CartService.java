package com.example.bigdatabackend.service;

import com.example.bigdatabackend.dto.CartResponse;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService {

    /**
     * 添加商品到购物车（使用默认用户）
     * @param productId 商品ID
     * @param quantity 商品数量
     */
    void addItem(String productId, Integer quantity);

    /**
     * 查询购物车（使用默认用户）
     * @return 购物车信息
     */
    CartResponse getCart();

    /**
     * 更新商品数量
     * @param productId 商品ID
     * @param quantity 新的数量
     */
    void updateItemQuantity(String productId, Integer quantity);

    /**
     * 删除购物车商品
     * @param productIds 商品ID列表
     */
    void removeItems(List<String> productIds);

    /**
     * 清空购物车
     */
    void clearCart();

    /**
     * 更新商品选中状态
     * @param productIds 商品ID列表
     * @param selected 是否选中
     */
    void updateItemsSelected(List<String> productIds, Boolean selected);
}
