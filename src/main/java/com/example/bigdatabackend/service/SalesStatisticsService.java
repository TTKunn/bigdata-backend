package com.example.bigdatabackend.service;

import com.example.bigdatabackend.dto.DailyOrdersResponse;
import com.example.bigdatabackend.dto.DailySalesResponse;
import com.example.bigdatabackend.dto.TopProductsResponse;
import com.example.bigdatabackend.dto.TotalSalesResponse;

/**
 * 销售统计服务接口
 */
public interface SalesStatisticsService {

    /**
     * 获取总销售额统计
     * @return 总销售额统计信息
     */
    TotalSalesResponse getTotalSales();

    /**
     * 获取当日销售额统计
     * @param date 统计日期，null表示当天
     * @return 当日销售额统计信息
     */
    DailySalesResponse getDailySales(String date);

    /**
     * 获取当日订单数量统计
     * @param date 统计日期，null表示当天
     * @return 当日订单数量统计信息
     */
    DailyOrdersResponse getDailyOrders(String date);

    /**
     * 获取最畅销商品排行榜
     * @param limit 返回数量限制
     * @return 畅销商品排行榜
     */
    TopProductsResponse getTopProducts(int limit);

    /**
     * 将订单ID写入待更新队列
     * @param orderId 已完成的订单ID
     */
    void enqueueOrderForStatisticsUpdate(String orderId);

    /**
     * 定时任务：批量处理待更新的订单统计数据
     */
    void processPendingStatisticsUpdates();
}
