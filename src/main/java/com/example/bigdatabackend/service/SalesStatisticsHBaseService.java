package com.example.bigdatabackend.service;

import com.example.bigdatabackend.constants.OrderConstants;
import com.example.bigdatabackend.dto.DailySalesResponse;
import com.example.bigdatabackend.dto.TotalSalesResponse;
import com.example.bigdatabackend.model.OrderStatus;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 销售统计HBase服务类 - 从HBase计算统计数据
 */
@Service
public class SalesStatisticsHBaseService {

    private static final Logger logger = LoggerFactory.getLogger(SalesStatisticsHBaseService.class);

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private Connection hBaseConnection;

    /**
     * 计算总销售额（从HBase查询所有已完成订单）
     */
    public TotalSalesResponse calculateTotalSales() throws IOException {
        BigDecimal totalSales = BigDecimal.ZERO;
        int completedOrders = 0;

        try (Table table = hBaseConnection.getTable(TableName.valueOf(OrderConstants.ORDER_TABLE_NAME))) {
            Scan scan = new Scan();

            // 只查询COMPLETED状态的订单
            SingleColumnValueFilter statusFilter = new SingleColumnValueFilter(
                Bytes.toBytes(OrderConstants.CF_BASE),
                Bytes.toBytes("status"),
                CompareFilter.CompareOp.EQUAL,
                Bytes.toBytes(OrderStatus.COMPLETED.name())
            );
            statusFilter.setFilterIfMissing(true);
            scan.setFilter(statusFilter);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    // 解析订单金额
                    byte[] amountBytes = result.getValue(
                        Bytes.toBytes(OrderConstants.CF_BASE),
                        Bytes.toBytes("actual_amount")
                    );

                    if (amountBytes != null) {
                        String amountStr = Bytes.toString(amountBytes);
                        totalSales = totalSales.add(new BigDecimal(amountStr));
                        completedOrders++;
                    }
                }
            }

            logger.info("Calculated total sales from HBase: totalSales={}, completedOrders={}",
                totalSales, completedOrders);
        } catch (IOException e) {
            logger.error("Failed to calculate total sales from HBase", e);
            throw e;
        }

        return new TotalSalesResponse(totalSales, completedOrders, LocalDateTime.now());
    }

    /**
     * 计算指定日期的销售额统计
     */
    public DailySalesResponse calculateDailySales(String dateStr) throws IOException {
        BigDecimal dailySales = BigDecimal.ZERO;
        int orderCount = 0;

        // 解析日期
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        try (Table table = hBaseConnection.getTable(TableName.valueOf(OrderConstants.ORDER_TABLE_NAME))) {
            Scan scan = new Scan();

            // 只查询COMPLETED状态的订单
            SingleColumnValueFilter statusFilter = new SingleColumnValueFilter(
                Bytes.toBytes(OrderConstants.CF_BASE),
                Bytes.toBytes("status"),
                CompareFilter.CompareOp.EQUAL,
                Bytes.toBytes(OrderStatus.COMPLETED.name())
            );
            statusFilter.setFilterIfMissing(true);
            scan.setFilter(statusFilter);

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    // 解析完成时间
                    byte[] completeTimeBytes = result.getValue(
                        Bytes.toBytes(OrderConstants.CF_BASE),
                        Bytes.toBytes("complete_time")
                    );

                    if (completeTimeBytes != null) {
                        String completeTimeStr = Bytes.toString(completeTimeBytes);
                        LocalDateTime completeTime = LocalDateTime.parse(completeTimeStr, DATETIME_FORMATTER);

                        // 检查是否在指定日期范围内
                        if (completeTime.isAfter(startOfDay) && completeTime.isBefore(endOfDay)) {
                            // 解析订单金额
                            byte[] amountBytes = result.getValue(
                                Bytes.toBytes(OrderConstants.CF_BASE),
                                Bytes.toBytes("actual_amount")
                            );

                            if (amountBytes != null) {
                                String amountStr = Bytes.toString(amountBytes);
                                dailySales = dailySales.add(new BigDecimal(amountStr));
                                orderCount++;
                            }
                        }
                    }
                }
            }

            logger.info("Calculated daily sales from HBase for date {}: dailySales={}, orderCount={}",
                dateStr, dailySales, orderCount);
        } catch (IOException e) {
            logger.error("Failed to calculate daily sales from HBase for date: {}", dateStr, e);
            throw e;
        }

        // 计算平均客单价
        BigDecimal averageOrderValue = orderCount > 0
            ? dailySales.divide(new BigDecimal(orderCount), 2, BigDecimal.ROUND_HALF_UP)
            : BigDecimal.ZERO;

        return new DailySalesResponse(dateStr, dailySales, orderCount, averageOrderValue, LocalDateTime.now());
    }
}
