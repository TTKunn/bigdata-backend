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
            scan.setCacheBlocks(false); // 禁用缓存，确保读取最新数据

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

        if (order.getCancelTime() != null) {
            put.addColumn(cfBaseBytes, Bytes.toBytes("cancel_time"), Bytes.toBytes(order.getCancelTime().format(DATETIME_FORMATTER)));
        }

        if (order.getCompleteTime() != null) {
            put.addColumn(cfBaseBytes, Bytes.toBytes("complete_time"), Bytes.toBytes(order.getCompleteTime().format(DATETIME_FORMATTER)));
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

            // 解析基础信息 - 添加空值检查
            byte[] orderIdBytes = result.getValue(cfBaseBytes, Bytes.toBytes("order_id"));
            if (orderIdBytes == null) {
                logger.warn("Order ID is null in HBase result, skipping this order");
                return null;
            }
            order.setOrderId(Bytes.toString(orderIdBytes));

            byte[] userIdBytes = result.getValue(cfBaseBytes, Bytes.toBytes("user_id"));
            order.setUserId(userIdBytes != null ? Bytes.toString(userIdBytes) : "");

            // 解析金额字段 - 添加空值检查
            byte[] totalAmountBytes = result.getValue(cfBaseBytes, Bytes.toBytes("total_amount"));
            if (totalAmountBytes != null) {
                String totalAmountStr = Bytes.toString(totalAmountBytes);
                order.setTotalAmount(new java.math.BigDecimal(totalAmountStr));
            } else {
                logger.warn("Total amount is null for order: {}", order.getOrderId());
                order.setTotalAmount(java.math.BigDecimal.ZERO);
            }

            byte[] discountAmountBytes = result.getValue(cfBaseBytes, Bytes.toBytes("discount_amount"));
            if (discountAmountBytes != null) {
                String discountAmountStr = Bytes.toString(discountAmountBytes);
                order.setDiscountAmount(new java.math.BigDecimal(discountAmountStr));
            } else {
                order.setDiscountAmount(java.math.BigDecimal.ZERO);
            }

            byte[] actualAmountBytes = result.getValue(cfBaseBytes, Bytes.toBytes("actual_amount"));
            if (actualAmountBytes != null) {
                String actualAmountStr = Bytes.toString(actualAmountBytes);
                order.setActualAmount(new java.math.BigDecimal(actualAmountStr));
            } else {
                logger.warn("Actual amount is null for order: {}", order.getOrderId());
                order.setActualAmount(java.math.BigDecimal.ZERO);
            }

            // 解析订单状态
            byte[] statusBytes = result.getValue(cfBaseBytes, Bytes.toBytes("status"));
            if (statusBytes != null) {
                String statusStr = Bytes.toString(statusBytes);
                order.setStatus(OrderStatus.valueOf(statusStr));
            } else {
                logger.warn("Status is null for order: {}, defaulting to PENDING_PAYMENT", order.getOrderId());
                order.setStatus(OrderStatus.PENDING_PAYMENT);
            }

            // 解析创建时间
            byte[] createTimeBytes = result.getValue(cfBaseBytes, Bytes.toBytes("create_time"));
            if (createTimeBytes != null) {
                String createTimeStr = Bytes.toString(createTimeBytes);
                order.setCreateTime(LocalDateTime.parse(createTimeStr, DATETIME_FORMATTER));
            } else {
                logger.warn("Create time is null for order: {}", order.getOrderId());
                order.setCreateTime(LocalDateTime.now());
            }

            // 解析支付时间
            byte[] payTimeBytes = result.getValue(cfBaseBytes, Bytes.toBytes("pay_time"));
            if (payTimeBytes != null) {
                String payTimeStr = Bytes.toString(payTimeBytes);
                order.setPayTime(LocalDateTime.parse(payTimeStr, DATETIME_FORMATTER));
            }

            // 解析取消时间
            byte[] cancelTimeBytes = result.getValue(cfBaseBytes, Bytes.toBytes("cancel_time"));
            if (cancelTimeBytes != null) {
                String cancelTimeStr = Bytes.toString(cancelTimeBytes);
                order.setCancelTime(LocalDateTime.parse(cancelTimeStr, DATETIME_FORMATTER));
            }

            // 解析完成时间
            byte[] completeTimeBytes = result.getValue(cfBaseBytes, Bytes.toBytes("complete_time"));
            if (completeTimeBytes != null) {
                String completeTimeStr = Bytes.toString(completeTimeBytes);
                order.setCompleteTime(LocalDateTime.parse(completeTimeStr, DATETIME_FORMATTER));
            }

            // 解析收货地址 - 添加空值检查
            byte[] receiverBytes = result.getValue(cfAddressBytes, Bytes.toBytes("receiver"));
            order.setReceiver(receiverBytes != null ? Bytes.toString(receiverBytes) : "");

            byte[] phoneBytes = result.getValue(cfAddressBytes, Bytes.toBytes("phone"));
            order.setPhone(phoneBytes != null ? Bytes.toString(phoneBytes) : "");

            byte[] addressBytes = result.getValue(cfAddressBytes, Bytes.toBytes("address"));
            order.setAddress(addressBytes != null ? Bytes.toString(addressBytes) : "");

            byte[] postcodeBytes = result.getValue(cfAddressBytes, Bytes.toBytes("postcode"));
            order.setPostcode(postcodeBytes != null ? Bytes.toString(postcodeBytes) : "");

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

    /**
     * 更新订单（状态变更时使用）
     */
    public void updateOrder(Order order) throws IOException {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("订单或订单ID不能为空");
        }

        logger.info("Updating order in HBase: orderId={}, status={}",
            order.getOrderId(), order.getStatus());

        try (Table table = hBaseConnection.getTable(tableName)) {
            // 使用Scan找到原始行的RowKey
            Scan scan = new Scan();
            scan.setRowPrefixFilter(Bytes.toBytes(order.getOrderId().substring(0, 8))); // 使用日期前缀
            scan.setCacheBlocks(false); // 禁用缓存，确保读取最新数据

            String actualRowKey = null;
            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    String orderId = Bytes.toString(result.getValue(cfBaseBytes, Bytes.toBytes("order_id")));
                    if (order.getOrderId().equals(orderId)) {
                        actualRowKey = Bytes.toString(result.getRow());
                        logger.debug("Found existing row with RowKey: {}", actualRowKey);
                        break;
                    }
                }
            }

            if (actualRowKey == null) {
                throw new IllegalArgumentException("订单不存在: " + order.getOrderId());
            }

            // 使用找到的RowKey进行更新
            Put put = new Put(Bytes.toBytes(actualRowKey));

            // 更新订单状态
            put.addColumn(cfBaseBytes, Bytes.toBytes("status"),
                Bytes.toBytes(order.getStatus().name()));

            // 更新时间戳字段
            if (order.getPayTime() != null) {
                put.addColumn(cfBaseBytes, Bytes.toBytes("pay_time"),
                    Bytes.toBytes(order.getPayTime().format(DATETIME_FORMATTER)));
            }

            if (order.getCancelTime() != null) {
                put.addColumn(cfBaseBytes, Bytes.toBytes("cancel_time"),
                    Bytes.toBytes(order.getCancelTime().format(DATETIME_FORMATTER)));
            }

            if (order.getCompleteTime() != null) {
                put.addColumn(cfBaseBytes, Bytes.toBytes("complete_time"),
                    Bytes.toBytes(order.getCompleteTime().format(DATETIME_FORMATTER)));
            }

            table.put(put);
            logger.info("Successfully updated order in HBase: {}", order.getOrderId());
        } catch (IOException e) {
            logger.error("Failed to update order in HBase: {}", order.getOrderId(), e);
            throw e;
        }
    }
}
