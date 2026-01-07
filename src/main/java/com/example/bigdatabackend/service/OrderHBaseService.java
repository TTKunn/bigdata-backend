package com.example.bigdatabackend.service;

import com.example.bigdatabackend.constants.OrderConstants;
import com.example.bigdatabackend.model.Order;
import com.example.bigdatabackend.model.OrderStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * HBase订单服务类 - 处理订单数据的HBase操作
 */
@Service
public class OrderHBaseService {

    private static final Logger logger = LoggerFactory.getLogger(OrderHBaseService.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private Connection hBaseConnection;

    private Gson gson;
    private TableName tableName;
    private byte[] cfBaseBytes;
    private byte[] cfAddressBytes;
    private byte[] cfItemsBytes;

    @PostConstruct
    public void init() {
        this.gson = new Gson();
        this.tableName = TableName.valueOf(OrderConstants.ORDER_TABLE_NAME);
        this.cfBaseBytes = Bytes.toBytes(OrderConstants.CF_BASE);
        this.cfAddressBytes = Bytes.toBytes(OrderConstants.CF_ADDRESS);
        this.cfItemsBytes = Bytes.toBytes(OrderConstants.CF_ITEMS);
        logger.info("OrderHBaseService initialized with table: {}", tableName.getNameAsString());
    }

    /**
     * 保存订单到HBase
     */
    public boolean saveOrder(Order order) throws IOException {
        if (order == null || order.getOrderId() == null) {
            logger.error("Order or order ID is null");
            return false;
        }

        String rowKey = generateRowKey(order);
        logger.debug("Generated RowKey for order: {}", rowKey);

        try (Table table = hBaseConnection.getTable(tableName)) {
            Put put = new Put(Bytes.toBytes(rowKey));

            // 写入基础信息列族
            writeBaseInfo(put, order);

            // 写入收货地址列族
            writeAddressInfo(put, order);

            // 写入商品明细列族
            writeItemsInfo(put, order);

            table.put(put);
            logger.info("Successfully saved order to HBase: {}", order.getOrderId());
            return true;
        } catch (IOException e) {
            logger.error("Failed to save order to HBase: {}", order.getOrderId(), e);
            throw e;
        }
    }

    /**
     * 根据订单号查询订单详情
     */
    public Order getOrderById(String orderId) throws IOException {
        if (orderId == null) {
            logger.warn("Order ID is null");
            return null;
        }

        try (Table table = hBaseConnection.getTable(tableName)) {
            // 使用订单号作为前缀扫描
            Scan scan = new Scan();
            scan.setRowPrefixFilter(Bytes.toBytes(orderId.substring(0, 8))); // 使用日期前缀

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    Order order = parseOrder(result);
                    if (order != null && orderId.equals(order.getOrderId())) {
                        logger.debug("Found order: {}", orderId);
                        return order;
                    }
                }
            }

            logger.debug("Order not found: {}", orderId);
            return null;
        } catch (IOException e) {
            logger.error("Failed to get order: {}", orderId, e);
            throw e;
        }
    }

    /**
     * 查询订单列表（支持分页和状态筛选）
     */
    public List<Order> getOrders(String status, int page, int size) throws IOException {
        List<Order> orders = new ArrayList<>();

        try (Table table = hBaseConnection.getTable(tableName)) {
            Scan scan = new Scan();

            // 如果指定了状态，添加过滤器
            if (status != null && !status.isEmpty()) {
                Filter filter = new SingleColumnValueFilter(
                        cfBaseBytes,
                        Bytes.toBytes("status"),
                        CompareFilter.CompareOp.EQUAL,
                        Bytes.toBytes(status)
                );
                scan.setFilter(filter);
            }

            // 设置扫描范围（简化实现，实际应该按日期范围扫描）
            scan.setReversed(true); // 倒序扫描，最新的订单在前

            int count = 0;
            int skip = (page - 1) * size;

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    if (count < skip) {
                        count++;
                        continue;
                    }

                    if (orders.size() >= size) {
                        break;
                    }

                    Order order = parseOrder(result);
                    if (order != null) {
                        orders.add(order);
                    }
                    count++;
                }
            }

            logger.info("Retrieved {} orders from HBase (page={}, size={})", orders.size(), page, size);
            return orders;
        } catch (IOException e) {
            logger.error("Failed to get orders from HBase", e);
            throw e;
        }
    }

    /**
     * 生成RowKey
     * 格式：{order_date}_{order_seq}_{timestamp}
     */
    private String generateRowKey(Order order) {
        LocalDateTime createTime = order.getCreateTime();
        String orderDate = createTime.format(DATE_FORMATTER);
        String orderId = order.getOrderId();

        // 从订单号中提取序列号（后6位）
        String sequence = orderId.substring(orderId.length() - 6);

        // 时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());

        return orderDate + "_" + sequence + "_" + timestamp;
    }

    /**
     * 写入基础信息列族
     */
    private void writeBaseInfo(Put put, Order order) {
        put.addColumn(cfBaseBytes, Bytes.toBytes("order_id"), Bytes.toBytes(order.getOrderId()));
        put.addColumn(cfBaseBytes, Bytes.toBytes("user_id"), Bytes.toBytes(order.getUserId()));
        put.addColumn(cfBaseBytes, Bytes.toBytes("total_amount"), Bytes.toBytes(order.getTotalAmount().toString()));
        put.addColumn(cfBaseBytes, Bytes.toBytes("discount_amount"), Bytes.toBytes(order.getDiscountAmount().toString()));
        put.addColumn(cfBaseBytes, Bytes.toBytes("actual_amount"), Bytes.toBytes(order.getActualAmount().toString()));
        put.addColumn(cfBaseBytes, Bytes.toBytes("status"), Bytes.toBytes(order.getStatus().name()));
        put.addColumn(cfBaseBytes, Bytes.toBytes("create_time"), Bytes.toBytes(order.getCreateTime().format(DATETIME_FORMATTER)));

        if (order.getPayTime() != null) {
            put.addColumn(cfBaseBytes, Bytes.toBytes("pay_time"), Bytes.toBytes(order.getPayTime().format(DATETIME_FORMATTER)));
        }
    }

    /**
     * 写入收货地址列族
     */
    private void writeAddressInfo(Put put, Order order) {
        put.addColumn(cfAddressBytes, Bytes.toBytes("receiver"), Bytes.toBytes(order.getReceiver()));
        put.addColumn(cfAddressBytes, Bytes.toBytes("phone"), Bytes.toBytes(order.getPhone()));
        put.addColumn(cfAddressBytes, Bytes.toBytes("address"), Bytes.toBytes(order.getAddress()));
        put.addColumn(cfAddressBytes, Bytes.toBytes("postcode"), Bytes.toBytes(order.getPostcode()));
    }

    /**
     * 写入商品明细列族
     */
    private void writeItemsInfo(Put put, Order order) {
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            // 将商品明细列表序列化为JSON
            String itemsJson = gson.toJson(order.getItems());
            put.addColumn(cfItemsBytes, Bytes.toBytes("items_json"), Bytes.toBytes(itemsJson));

            // 同时存储商品数量，方便查询
            put.addColumn(cfItemsBytes, Bytes.toBytes("item_count"), Bytes.toBytes(String.valueOf(order.getItems().size())));
        }
    }

    /**
     * 解析HBase Result为Order对象
     */
    private Order parseOrder(Result result) {
        if (result == null || result.isEmpty()) {
            return null;
        }

        try {
            Order order = new Order();

            // 解析基础信息
            order.setOrderId(Bytes.toString(result.getValue(cfBaseBytes, Bytes.toBytes("order_id"))));
            order.setUserId(Bytes.toString(result.getValue(cfBaseBytes, Bytes.toBytes("user_id"))));
            order.setTotalAmount(new java.math.BigDecimal(Bytes.toString(result.getValue(cfBaseBytes, Bytes.toBytes("total_amount")))));
            order.setDiscountAmount(new java.math.BigDecimal(Bytes.toString(result.getValue(cfBaseBytes, Bytes.toBytes("discount_amount")))));
            order.setActualAmount(new java.math.BigDecimal(Bytes.toString(result.getValue(cfBaseBytes, Bytes.toBytes("actual_amount")))));

            String statusStr = Bytes.toString(result.getValue(cfBaseBytes, Bytes.toBytes("status")));
            order.setStatus(OrderStatus.valueOf(statusStr));

            String createTimeStr = Bytes.toString(result.getValue(cfBaseBytes, Bytes.toBytes("create_time")));
            order.setCreateTime(LocalDateTime.parse(createTimeStr, DATETIME_FORMATTER));

            byte[] payTimeBytes = result.getValue(cfBaseBytes, Bytes.toBytes("pay_time"));
            if (payTimeBytes != null) {
                String payTimeStr = Bytes.toString(payTimeBytes);
                order.setPayTime(LocalDateTime.parse(payTimeStr, DATETIME_FORMATTER));
            }

            // 解析收货地址
            order.setReceiver(Bytes.toString(result.getValue(cfAddressBytes, Bytes.toBytes("receiver"))));
            order.setPhone(Bytes.toString(result.getValue(cfAddressBytes, Bytes.toBytes("phone"))));
            order.setAddress(Bytes.toString(result.getValue(cfAddressBytes, Bytes.toBytes("address"))));
            order.setPostcode(Bytes.toString(result.getValue(cfAddressBytes, Bytes.toBytes("postcode"))));

            // 解析商品明细
            byte[] itemsJsonBytes = result.getValue(cfItemsBytes, Bytes.toBytes("items_json"));
            if (itemsJsonBytes != null) {
                String itemsJson = Bytes.toString(itemsJsonBytes);
                List<Order.OrderItem> items = gson.fromJson(itemsJson, new TypeToken<List<Order.OrderItem>>(){}.getType());
                order.setItems(items);
            }

            return order;
        } catch (Exception e) {
            logger.error("Failed to parse order from HBase result", e);
            return null;
        }
    }
}
