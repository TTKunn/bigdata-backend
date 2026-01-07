package com.example.bigdatabackend.service;

import com.example.bigdatabackend.model.Product;
import com.example.bigdatabackend.model.ProductImage;
import com.example.bigdatabackend.model.ProductStatus;
import com.example.bigdatabackend.model.ProductStock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HBase商品服务类 - 处理商品数据的HBase操作
 */
@Service
public class ProductHBaseService {

    private static final Logger logger = LoggerFactory.getLogger(ProductHBaseService.class);

    private static final String TABLE_NAME = "product_info";
    private static final String CF_BASE = "cf_base";
    private static final String CF_DETAIL = "cf_detail";
    private static final String CF_STOCK = "cf_stock";
    private static final String CF_STAT = "cf_stat";

    @Autowired
    private Connection hBaseConnection;

    private ObjectMapper objectMapper;
    private TableName tableName;
    private byte[] cfBaseBytes;
    private byte[] cfDetailBytes;
    private byte[] cfStockBytes;
    private byte[] cfStatBytes;

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        tableName = TableName.valueOf(TABLE_NAME);
        cfBaseBytes = Bytes.toBytes(CF_BASE);
        cfDetailBytes = Bytes.toBytes(CF_DETAIL);
        cfStockBytes = Bytes.toBytes(CF_STOCK);
        cfStatBytes = Bytes.toBytes(CF_STAT);
    }

    /**
     * 保存商品到HBase
     */
    public boolean saveProduct(Product product) throws IOException {
        if (product == null || product.getId() == null) {
            logger.error("Product or product ID is null");
            return false;
        }

        String rowKey = generateRowKey(product);

        try (Table table = hBaseConnection.getTable(tableName)) {
            Put put = new Put(Bytes.toBytes(rowKey));

            // 写入基础信息列族
            writeBaseInfo(put, product);

            // 写入详细信息列族
            writeDetailInfo(put, product);

            // 写入库存信息列族
            writeStockInfo(put, product);

            // 写入统计信息列族
            writeStatInfo(put, product);

            table.put(put);
            logger.info("Successfully saved product to HBase: {}", product.getId());
            return true;
        } catch (IOException e) {
            logger.error("Failed to save product to HBase: {}", product.getId(), e);
            throw e;
        }
    }

    /**
     * 从HBase获取商品
     * 注意：由于RowKey设计较为复杂，这里使用扫描的方式查找商品
     */
    public Product getProduct(String productId) throws IOException {
        if (productId == null) {
            return null;
        }

        try (Table table = hBaseConnection.getTable(tableName)) {
            // 构建扫描器，查找指定商品ID的记录
            Scan scan = new Scan();
            // 设置过滤器：RowKey以商品ID开头
            String prefix = productId + "_";
            scan.setRowPrefixFilter(Bytes.toBytes(prefix));

            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    if (!result.isEmpty()) {
                        return parseProduct(result, productId);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to get product from HBase: {}", productId, e);
            throw e;
        }

        logger.debug("Product not found in HBase: {}", productId);
        return null;
    }

    /**
     * 生成RowKey
     */
    private String generateRowKey(Product product) {
        // {category_id}_{product_id}_{timestamp}
        String categoryId = product.getCategory();
        String productId = product.getId();
        long timestamp = product.getCreateTime() != null ?
            product.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 :
            System.currentTimeMillis();

        return String.format("%s_%s_%013d", categoryId, productId, timestamp);
    }


    /**
     * 写入基础信息
     */
    private void writeBaseInfo(Put put, Product product) {
        // 商品ID
        put.addColumn(cfBaseBytes, Bytes.toBytes("id"), Bytes.toBytes(product.getId()));

        // 商品名称
        if (product.getName() != null) {
            put.addColumn(cfBaseBytes, Bytes.toBytes("name"), Bytes.toBytes(product.getName()));
        }

        // 分类
        if (product.getCategory() != null) {
            put.addColumn(cfBaseBytes, Bytes.toBytes("category"), Bytes.toBytes(product.getCategory()));
        }

        // 品牌
        if (product.getBrand() != null) {
            put.addColumn(cfBaseBytes, Bytes.toBytes("brand"), Bytes.toBytes(product.getBrand()));
        }

        // 价格
        if (product.getPrice() != null) {
            put.addColumn(cfBaseBytes, Bytes.toBytes("price"), Bytes.toBytes(product.getPrice().toString()));
        }

        // 成本价
        if (product.getCost() != null) {
            put.addColumn(cfBaseBytes, Bytes.toBytes("cost"), Bytes.toBytes(product.getCost().toString()));
        }

        // 状态
        ProductStatus status = product.getStatus() != null ? product.getStatus() : ProductStatus.ACTIVE;
        put.addColumn(cfBaseBytes, Bytes.toBytes("status"), Bytes.toBytes(status.getCode()));

        // 创建时间
        if (product.getCreateTime() != null) {
            String timeStr = product.getCreateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            put.addColumn(cfBaseBytes, Bytes.toBytes("create_time"), Bytes.toBytes(timeStr));
        }
    }

    /**
     * 写入详细信息
     */
    private void writeDetailInfo(Put put, Product product) throws IOException {
        // 描述
        if (product.getDescription() != null) {
            put.addColumn(cfDetailBytes, Bytes.toBytes("description"), Bytes.toBytes(product.getDescription()));
        }

        // 规格参数
        if (product.getSpec() != null && !product.getSpec().isEmpty()) {
            String specJson = objectMapper.writeValueAsString(product.getSpec());
            put.addColumn(cfDetailBytes, Bytes.toBytes("spec"), Bytes.toBytes(specJson));
        }

        // 标签
        if (product.getTags() != null && !product.getTags().isEmpty()) {
            String tagsJson = objectMapper.writeValueAsString(product.getTags());
            put.addColumn(cfDetailBytes, Bytes.toBytes("tags"), Bytes.toBytes(tagsJson));
        }

        // 图片信息
        if (product.getImage() != null) {
            String imageJson = objectMapper.writeValueAsString(product.getImage());
            put.addColumn(cfDetailBytes, Bytes.toBytes("image"), Bytes.toBytes(imageJson));
        }
    }

    /**
     * 写入库存信息
     */
    private void writeStockInfo(Put put, Product product) {
        if (product.getStock() != null) {
            ProductStock stock = product.getStock();

            // 总库存
            put.addColumn(cfStockBytes, Bytes.toBytes("total_stock"), Bytes.toBytes(String.valueOf(stock.getTotal())));

            // 仓库库存
            put.addColumn(cfStockBytes, Bytes.toBytes("warehouse_stock"), Bytes.toBytes(String.valueOf(stock.getTotal())));

            // 安全库存
            put.addColumn(cfStockBytes, Bytes.toBytes("safe_stock"), Bytes.toBytes(String.valueOf(stock.getSafe())));

            // 锁定库存
            put.addColumn(cfStockBytes, Bytes.toBytes("lock_stock"), Bytes.toBytes(String.valueOf(stock.getLock())));

            // 仓库信息
            if (stock.getWarehouse() != null) {
                put.addColumn(cfStockBytes, Bytes.toBytes("warehouse"), Bytes.toBytes(stock.getWarehouse()));
            }
        }
    }

    /**
     * 写入统计信息
     */
    private void writeStatInfo(Put put, Product product) {
        // 初始化统计信息（创建时为0）
        put.addColumn(cfStatBytes, Bytes.toBytes("view_count"), Bytes.toBytes("0"));
        put.addColumn(cfStatBytes, Bytes.toBytes("sale_count"), Bytes.toBytes("0"));
        put.addColumn(cfStatBytes, Bytes.toBytes("collect_count"), Bytes.toBytes("0"));

        // 更新时间
        String updateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        put.addColumn(cfStatBytes, Bytes.toBytes("update_time"), Bytes.toBytes(updateTime));
    }

    /**
     * 解析Result为Product对象
     */
    private Product parseProduct(Result result, String productId) throws IOException {
        Product product = new Product();
        product.setId(productId);

        // 解析基础信息
        parseBaseInfo(result, product);

        // 解析详细信息
        parseDetailInfo(result, product);

        // 解析库存信息
        parseStockInfo(result, product);

        return product;
    }

    /**
     * 解析基础信息
     */
    private void parseBaseInfo(Result result, Product product) {
        // 名称
        byte[] nameBytes = result.getValue(cfBaseBytes, Bytes.toBytes("name"));
        if (nameBytes != null) {
            product.setName(Bytes.toString(nameBytes));
        }

        // 分类
        byte[] categoryBytes = result.getValue(cfBaseBytes, Bytes.toBytes("category"));
        if (categoryBytes != null) {
            product.setCategory(Bytes.toString(categoryBytes));
        }

        // 品牌
        byte[] brandBytes = result.getValue(cfBaseBytes, Bytes.toBytes("brand"));
        if (brandBytes != null) {
            product.setBrand(Bytes.toString(brandBytes));
        }

        // 价格
        byte[] priceBytes = result.getValue(cfBaseBytes, Bytes.toBytes("price"));
        if (priceBytes != null) {
            product.setPrice(new BigDecimal(Bytes.toString(priceBytes)));
        }

        // 成本价
        byte[] costBytes = result.getValue(cfBaseBytes, Bytes.toBytes("cost"));
        if (costBytes != null) {
            product.setCost(new BigDecimal(Bytes.toString(costBytes)));
        }

        // 状态
        byte[] statusBytes = result.getValue(cfBaseBytes, Bytes.toBytes("status"));
        if (statusBytes != null) {
            product.setStatus(ProductStatus.fromCode(Bytes.toString(statusBytes)));
        }

        // 创建时间
        byte[] timeBytes = result.getValue(cfBaseBytes, Bytes.toBytes("create_time"));
        if (timeBytes != null) {
            String timeStr = Bytes.toString(timeBytes);
            if (timeStr.endsWith("Z")) {
                timeStr = timeStr.substring(0, timeStr.length() - 1);
            }
            product.setCreateTime(LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
    }

    /**
     * 解析详细信息
     */
    private void parseDetailInfo(Result result, Product product) throws IOException {
        // 描述
        byte[] descBytes = result.getValue(cfDetailBytes, Bytes.toBytes("description"));
        if (descBytes != null) {
            product.setDescription(Bytes.toString(descBytes));
        }

        // 规格参数
        byte[] specBytes = result.getValue(cfDetailBytes, Bytes.toBytes("spec"));
        if (specBytes != null) {
            Map<String, Object> spec = objectMapper.readValue(specBytes, new TypeReference<Map<String, Object>>() {});
            product.setSpec(spec);
        }

        // 标签
        byte[] tagsBytes = result.getValue(cfDetailBytes, Bytes.toBytes("tags"));
        if (tagsBytes != null) {
            List<String> tags = objectMapper.readValue(tagsBytes, new TypeReference<List<String>>() {});
            product.setTags(tags);
        }

        // 图片信息
        byte[] imageBytes = result.getValue(cfDetailBytes, Bytes.toBytes("image"));
        if (imageBytes != null) {
            ProductImage image = objectMapper.readValue(imageBytes, ProductImage.class);
            product.setImage(image);
        }
    }

    /**
     * 解析库存信息
     */
    private void parseStockInfo(Result result, Product product) {
        ProductStock stock = new ProductStock();

        // 总库存
        byte[] totalBytes = result.getValue(cfStockBytes, Bytes.toBytes("total_stock"));
        if (totalBytes != null) {
            stock.setTotal(Integer.parseInt(Bytes.toString(totalBytes)));
        }

        // 安全库存
        byte[] safeBytes = result.getValue(cfStockBytes, Bytes.toBytes("safe_stock"));
        if (safeBytes != null) {
            stock.setSafe(Integer.parseInt(Bytes.toString(safeBytes)));
        }

        // 锁定库存
        byte[] lockBytes = result.getValue(cfStockBytes, Bytes.toBytes("lock_stock"));
        if (lockBytes != null) {
            stock.setLock(Integer.parseInt(Bytes.toString(lockBytes)));
        }

        // 仓库信息
        byte[] warehouseBytes = result.getValue(cfStockBytes, Bytes.toBytes("warehouse"));
        if (warehouseBytes != null) {
            stock.setWarehouse(Bytes.toString(warehouseBytes));
        }

        product.setStock(stock);
    }

    /**
     * 检查HBase连接
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
