package com.example.bigdatabackend.service;

import com.example.bigdatabackend.dto.CartResponse;

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
}
