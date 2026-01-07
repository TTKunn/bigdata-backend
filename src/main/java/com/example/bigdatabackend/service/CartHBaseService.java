package com.example.bigdatabackend.service;

import com.example.bigdatabackend.model.CartItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车HBase服务类 - 处理购物车数据的HBase持久化操作
 */
@Service
public class CartHBaseService {

    private static final Logger logger = LoggerFactory.getLogger(CartHBaseService.class);

    private static final String TABLE_NAME = "cart_data";
    private static final String CF_ITEMS = "cf_items";
    private static final String CF_META = "cf_meta";

    @Autowired
    private Connection hBaseConnection;

    private ObjectMapper objectMapper;
    private TableName tableName;
    private byte[] cfItemsBytes;
    private byte[] cfMetaBytes;

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        // 注册Java Time模块以支持LocalDateTime等类型
        objectMapper.findAndRegisterModules();
        this.tableName = TableName.valueOf(TABLE_NAME);
        cfItemsBytes = Bytes.toBytes(CF_ITEMS);
        cfMetaBytes = Bytes.toBytes(CF_META);
        logger.info("CartHBaseService initialized with table: {}", TABLE_NAME);
    }

    /**
     * 保存购物车到HBase（同步操作）
     * @param userId 用户ID
     * @param items 购物车商品列表
     * @throws IOException HBase操作异常
     */
    public void saveCart(String userId, List<CartItem> items) throws IOException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        logger.info("Saving cart to HBase: userId={}, itemCount={}", userId, items != null ? items.size() : 0);

        String rowKey = userId;

        try (Table table = hBaseConnection.getTable(tableName)) {
            // 先删除 cf_items 列族中的所有旧数据，确保数据一致性
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            delete.addFamily(cfItemsBytes);
            table.delete(delete);
            logger.debug("Deleted old cart items from HBase: userId={}", userId);

            // 创建新的Put操作
            Put put = new Put(Bytes.toBytes(rowKey));

            // 写入商品明细到 cf_items 列族
            if (items != null && !items.isEmpty()) {
                for (CartItem item : items) {
                    String column = "product_" + item.getProductId();
                    String itemJson = objectMapper.writeValueAsString(item);
                    put.addColumn(cfItemsBytes, Bytes.toBytes(column), Bytes.toBytes(itemJson));
                }
            }

            // 写入元数据到 cf_meta 列族
            long currentTime = System.currentTimeMillis();
            put.addColumn(cfMetaBytes, Bytes.toBytes("update_time"), Bytes.toBytes(currentTime));
            put.addColumn(cfMetaBytes, Bytes.toBytes("total_items"), Bytes.toBytes(items != null ? items.size() : 0));

            // 计算总金额（这里只记录商品数量，金额在查询时计算）
            put.addColumn(cfMetaBytes, Bytes.toBytes("status"), Bytes.toBytes("ACTIVE"));

            table.put(put);
            logger.info("Successfully saved cart to HBase: userId={}, itemCount={}", userId, items != null ? items.size() : 0);
        } catch (IOException e) {
            logger.error("Failed to save cart to HBase: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 从HBase加载购物车（同步操作）
     * @param userId 用户ID
     * @return 购物车商品列表
     * @throws IOException HBase操作异常
     */
    public List<CartItem> loadCart(String userId) throws IOException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        logger.info("Loading cart from HBase: userId={}", userId);

        String rowKey = userId;

        try (Table table = hBaseConnection.getTable(tableName)) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(cfItemsBytes);

            Result result = table.get(get);
            if (result.isEmpty()) {
                logger.info("Cart not found in HBase: userId={}", userId);
                return new ArrayList<>();
            }

            List<CartItem> items = new ArrayList<>();

            // 遍历 cf_items 列族中的所有列
            result.getFamilyMap(cfItemsBytes).forEach((qualifier, value) -> {
                String qualifierStr = Bytes.toString(qualifier);
                if (qualifierStr.startsWith("product_")) {
                    try {
                        String itemJson = Bytes.toString(value);
                        CartItem item = objectMapper.readValue(itemJson, CartItem.class);
                        items.add(item);
                    } catch (Exception e) {
                        logger.error("Failed to parse cart item: qualifier={}", qualifierStr, e);
                    }
                }
            });

            logger.info("Successfully loaded cart from HBase: userId={}, itemCount={}", userId, items.size());
            return items;
        } catch (IOException e) {
            logger.error("Failed to load cart from HBase: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 删除购物车数据（同步操作）
     * @param userId 用户ID
     * @throws IOException HBase操作异常
     */
    public void deleteCart(String userId) throws IOException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        logger.info("Deleting cart from HBase: userId={}", userId);

        String rowKey = userId;

        try (Table table = hBaseConnection.getTable(tableName)) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
            logger.info("Successfully deleted cart from HBase: userId={}", userId);
        } catch (IOException e) {
            logger.error("Failed to delete cart from HBase: userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 检查HBase连接
     * @return 连接是否正常
     */
    public boolean checkConnection() {
        try (Admin admin = hBaseConnection.getAdmin()) {
            return admin != null && !hBaseConnection.isClosed();
        } catch (IOException e) {
            logger.error("HBase connection check failed", e);
            return false;
        }
    }
}
