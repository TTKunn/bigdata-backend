// 此文件仅用于环境联调测试，测试完成后请删除
package com.example.bigdatabackend.service;

import com.example.bigdatabackend.model.TestProduct;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * HBase测试服务类 - 仅用于环境联调测试
 * 测试完成后请删除此文件
 */
@Service
public class TestHBaseService {

    private static final Logger logger = LoggerFactory.getLogger(TestHBaseService.class);

    @Autowired
    private Connection hBaseConnection;

    // Hardcoded for testing - will be moved to config later
    private final String tableName = "env_test_product";
    private final String columnFamily = "cf_info";

    // Constructor to ensure initialization
    public TestHBaseService() {
        logger.info("TestHBaseService constructor called");
    }

    private TableName tableNameObj;
    private byte[] columnFamilyBytes;

    @PostConstruct
    public void init() {
        logger.info("TestHBaseService @PostConstruct called");
        initializeTableInfo();
    }

    /**
     * Initialize table information
     */
    private void initializeTableInfo() {
        if (this.tableNameObj == null) {
            logger.info("Initializing table information: table={}, columnFamily={}", tableName, columnFamily);
            this.tableNameObj = TableName.valueOf(tableName);
            this.columnFamilyBytes = Bytes.toBytes(columnFamily);
            logger.info("Table information initialized: tableNameObj={}, columnFamilyBytes length={}",
                       tableNameObj, columnFamilyBytes.length);
        }
    }

    /**
     * 创建测试表（如果不存在）
     */
    public boolean createTestTable() {
        logger.info("Attempting to create test table: {}", tableName);

        // Ensure table information is initialized
        initializeTableInfo();

        logger.info("TableName object: {}", tableNameObj);

        if (tableNameObj == null) {
            logger.error("TableName object is null - initialization failed");
            return false;
        }

        try (Admin admin = hBaseConnection.getAdmin()) {
            logger.info("Admin object created successfully");
            if (!admin.tableExists(tableNameObj)) {
                TableDescriptorBuilder tableDescriptorBuilder = TableDescriptorBuilder.newBuilder(tableNameObj);
                ColumnFamilyDescriptor columnFamilyDescriptor = ColumnFamilyDescriptorBuilder
                    .newBuilder(columnFamilyBytes)
                    .build();
                tableDescriptorBuilder.setColumnFamily(columnFamilyDescriptor);

                TableDescriptor tableDescriptor = tableDescriptorBuilder.build();
                admin.createTable(tableDescriptor);

                logger.info("Test table created successfully: {}", tableName);
                return true;
            } else {
                logger.info("Test table already exists: {}", tableName);
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to create test table", e);
            return false;
        }
    }

    /**
     * 写入商品数据到HBase
     */
    public boolean saveProduct(TestProduct product) {
        if (product == null || product.getId() == null) {
            logger.error("Product or product ID is null");
            return false;
        }

        try (Table table = hBaseConnection.getTable(tableNameObj)) {
            Put put = new Put(Bytes.toBytes(product.getId()));

            // 写入各个字段
            if (product.getName() != null) {
                put.addColumn(columnFamilyBytes, Bytes.toBytes("name"), Bytes.toBytes(product.getName()));
            }
            if (product.getCategory() != null) {
                put.addColumn(columnFamilyBytes, Bytes.toBytes("category"), Bytes.toBytes(product.getCategory()));
            }
            if (product.getPrice() != null) {
                put.addColumn(columnFamilyBytes, Bytes.toBytes("price"), Bytes.toBytes(product.getPrice().toString()));
            }
            if (product.getDescription() != null) {
                put.addColumn(columnFamilyBytes, Bytes.toBytes("description"), Bytes.toBytes(product.getDescription()));
            }
            if (product.getCreateTime() != null) {
                String timeStr = product.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
                put.addColumn(columnFamilyBytes, Bytes.toBytes("create_time"), Bytes.toBytes(timeStr));
            }

            table.put(put);
            logger.info("Product data written to HBase successfully, rowKey: {}", product.getId());
            return true;

        } catch (IOException e) {
            logger.error("Failed to save product to HBase", e);
            return false;
        }
    }

    /**
     * 从HBase查询商品数据
     */
    public TestProduct getProduct(String productId) {
        if (productId == null) {
            logger.error("Product ID is null");
            return null;
        }

        try (Table table = hBaseConnection.getTable(tableNameObj)) {
            Get get = new Get(Bytes.toBytes(productId));
            Result result = table.get(get);

            if (result.isEmpty()) {
                logger.info("No data found for product ID: {}", productId);
                return null;
            }

            TestProduct product = new TestProduct();
            product.setId(productId);

            // 读取各个字段
            byte[] nameBytes = result.getValue(columnFamilyBytes, Bytes.toBytes("name"));
            if (nameBytes != null) {
                product.setName(Bytes.toString(nameBytes));
            }

            byte[] categoryBytes = result.getValue(columnFamilyBytes, Bytes.toBytes("category"));
            if (categoryBytes != null) {
                product.setCategory(Bytes.toString(categoryBytes));
            }

            byte[] priceBytes = result.getValue(columnFamilyBytes, Bytes.toBytes("price"));
            if (priceBytes != null) {
                product.setPrice(new BigDecimal(Bytes.toString(priceBytes)));
            }

            byte[] descBytes = result.getValue(columnFamilyBytes, Bytes.toBytes("description"));
            if (descBytes != null) {
                product.setDescription(Bytes.toString(descBytes));
            }

            byte[] timeBytes = result.getValue(columnFamilyBytes, Bytes.toBytes("create_time"));
            if (timeBytes != null) {
                String timeStr = Bytes.toString(timeBytes);
                // 移除Z后缀进行解析
                if (timeStr.endsWith("Z")) {
                    timeStr = timeStr.substring(0, timeStr.length() - 1);
                }
                product.setCreateTime(LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            logger.info("Product data retrieved from HBase successfully, rowKey: {}", productId);
            return product;

        } catch (IOException e) {
            logger.error("Failed to get product from HBase", e);
            return null;
        }
    }

    /**
     * 检查HBase连接是否正常
     */
    public boolean checkConnection() {
        try {
            Admin admin = hBaseConnection.getAdmin();
            boolean isConnected = admin != null && !hBaseConnection.isClosed();
            if (isConnected) {
                logger.info("HBase connection is healthy");
            } else {
                logger.error("HBase connection is not healthy");
            }
            return isConnected;
        } catch (IOException e) {
            logger.error("Failed to check HBase connection", e);
            return false;
        }
    }

    /**
     * 删除测试表（清理方法）
     */
    public boolean dropTestTable() {
        try (Admin admin = hBaseConnection.getAdmin()) {
            if (admin.tableExists(tableNameObj)) {
                admin.disableTable(tableNameObj);
                admin.deleteTable(tableNameObj);
                logger.info("Test table dropped successfully: {}", tableName);
                return true;
            } else {
                logger.info("Test table does not exist: {}", tableName);
                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to drop test table", e);
            return false;
        }
    }
}
