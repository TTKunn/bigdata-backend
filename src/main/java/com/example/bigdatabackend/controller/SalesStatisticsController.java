package com.example.bigdatabackend.controller;

import com.example.bigdatabackend.dto.*;
import com.example.bigdatabackend.service.SalesStatisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 销售统计控制器
 */
@RestController
@RequestMapping("/api/statistics")
@Api(tags = "销售统计管理")
public class SalesStatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(SalesStatisticsController.class);

    @Autowired
    private SalesStatisticsService statisticsService;

    /**
     * 获取总销售额统计
     */
    @GetMapping("/total-sales")
    @ApiOperation("获取总销售额统计")
    public ApiResponse<TotalSalesResponse> getTotalSales() {
        try {
            logger.info("Received request to get total sales");
            TotalSalesResponse response = statisticsService.getTotalSales();
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("Failed to get total sales", e);
            return ApiResponse.error("获取总销售额统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取当日销售额统计
     */
    @GetMapping("/daily-sales")
    @ApiOperation("获取当日销售额统计")
    public ApiResponse<DailySalesResponse> getDailySales(
            @ApiParam("统计日期(yyyyMMdd格式)，不传则查询当天") @RequestParam(required = false) String date) {
        try {
            logger.info("Received request to get daily sales for date: {}", date);
            DailySalesResponse response = statisticsService.getDailySales(date);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("Failed to get daily sales for date: {}", date, e);
            return ApiResponse.error("获取当日销售额统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取当日订单数量统计
     */
    @GetMapping("/daily-orders")
    @ApiOperation("获取当日订单数量统计")
    public ApiResponse<DailyOrdersResponse> getDailyOrders(
            @ApiParam("统计日期(yyyyMMdd格式)，不传则查询当天") @RequestParam(required = false) String date) {
        try {
            logger.info("Received request to get daily orders for date: {}", date);
            DailyOrdersResponse response = statisticsService.getDailyOrders(date);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("Failed to get daily orders for date: {}", date, e);
            return ApiResponse.error("获取当日订单数量统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取最畅销商品排行榜
     */
    @GetMapping("/top-products")
    @ApiOperation("获取最畅销商品排行榜")
    public ApiResponse<TopProductsResponse> getTopProducts(
            @ApiParam("返回数量限制，默认3") @RequestParam(defaultValue = "3") int limit) {
        try {
            if (limit <= 0 || limit > 20) {
                return ApiResponse.error("limit参数必须在1-20之间");
            }

            logger.info("Received request to get top {} products", limit);
            TopProductsResponse response = statisticsService.getTopProducts(limit);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("Failed to get top products", e);
            return ApiResponse.error("获取畅销商品排行榜失败: " + e.getMessage());
        }
    }
}
