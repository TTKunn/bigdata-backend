package com.example.bigdatabackend.service;

import com.example.bigdatabackend.model.Product;
import com.example.bigdatabackend.model.ProductImage;
import com.example.bigdatabackend.model.ProductStatus;
import com.example.bigdatabackend.model.ProductStock;
import com.example.bigdatabackend.dto.ProductListQueryRequest;
import com.example.bigdatabackend.dto.ProductListResponse;
import com.example.bigdatabackend.dto.ProductSummaryDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
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

    private static final String CF_BASE = "cf_base";
    private static final String CF_DETAIL = "cf_detail";
    private static final String CF_STOCK = "cf_stock";
    private static final String CF_STAT = "cf_stat";

    @Value("${hbase.product.table.name}")
    private String tableNameConfig;

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
        // 注册Java Time模块以支持LocalDateTime等类型
        objectMapper.findAndRegisterModules();
        this.tableName = TableName.valueOf(tableNameConfig);
        cfBaseBytes = Bytes.toBytes(CF_BASE);
        cfDetailBytes = Bytes.toBytes(CF_DETAIL);
        cfStockBytes = Bytes.toBytes(CF_STOCK);
        cfStatBytes = Bytes.toBytes(CF_STAT);
        logger.info("ProductHBaseService initialized with table: {}", tableName.getNameAsString());
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
     * 注意：接口商品ID为12位格式：{category_id(4位)}{product_id(8位)}
     * HBase RowKey格式：{category_id}_{product_id}_{timestamp}
     */
    public Product getProduct(String productId) throws IOException {
        if (productId == null) {
            return null;
        }

        // 验证商品ID格式
        if (productId.length() != 12) {
            logger.warn("Invalid product ID format: {} (length: {})", productId, productId.length());
            return null;
        }

        try (Table table = hBaseConnection.getTable(tableName)) {
            // 从12位商品ID中解析category_id和product_id
            String categoryId = productId.substring(0, 4);
            String productIdPart = productId.substring(4, 12);

            // 构建RowKey前缀：{category_id}_{product_id}_
            String rowKeyPrefix = categoryId + "_" + productIdPart + "_";

            // 构建扫描器，查找指定商品ID的记录
            Scan scan = new Scan();
            scan.setRowPrefixFilter(Bytes.toBytes(rowKeyPrefix));

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
     * RowKey格式: {category_id}_{product_id}_{timestamp}
     * 注意：接口使用12位productId格式，但RowKey中只使用后8位
     */
    private String generateRowKey(Product product) {
        String categoryId = product.getCategory();  // 4位分类ID
        String fullProductId = product.getId();  // 12位完整商品ID

        // 从12位商品ID中提取后8位作为RowKey中的product_id部分
        // 如果不是12位则直接使用（兼容性处理）
        String productIdPart = fullProductId.length() == 12 ?
            fullProductId.substring(4, 12) : fullProductId;

        long timestamp = product.getCreateTime() != null ?
            product.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 :
            System.currentTimeMillis();

        return String.format("%s_%s_%013d", categoryId, productIdPart, timestamp);
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
            // 使用自定义格式解析：yyyy-MM-dd HH:mm:ss
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            product.setCreateTime(LocalDateTime.parse(timeStr, formatter));
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
            try {
                ProductImage image = objectMapper.readValue(imageBytes, ProductImage.class);
                product.setImage(image);
            } catch (Exception e) {
                logger.warn("Failed to parse image for product, skipping image: {}", e.getMessage());
                // 图片解析失败不影响商品其他信息的返回
            }
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
     * 查询商品列表（支持筛选，返回所有匹配商品）
     */
    public ProductListResponse getProductList(ProductListQueryRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("查询请求不能为空");
        }

        try (Table table = hBaseConnection.getTable(tableName)) {
            // 构建查询扫描器
            Scan scan = new Scan();

            // 设置筛选条件
            List<Filter> filters = buildListFilters(request);
            if (!filters.isEmpty()) {
                FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL, filters);
                scan.setFilter(filterList);
            }

            // 设置排序（通过RowKey自然排序）
            if ("desc".equalsIgnoreCase(request.getSortOrder())) {
                scan.setReversed(true); // 反转扫描，降序
            }

            // 执行查询，返回所有匹配的商品
            List<ProductSummaryDto> products = new ArrayList<>();
            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    if (!result.isEmpty()) {
                        ProductSummaryDto summary = parseProductSummary(result);
                        if (summary != null) {
                            products.add(summary);
                        }
                    }
                }
            }

            // 返回查询结果，total等于实际查询到的数量
            long total = products.size();
            return new ProductListResponse(total, 1, (int)total, products);

        } catch (IOException e) {
            logger.error("Failed to query product list", e);
            throw e;
        }
    }

    /**
     * 构建列表查询筛选条件
     */
    private List<Filter> buildListFilters(ProductListQueryRequest request) {
        List<Filter> filters = new ArrayList<>();

        // 分类筛选（通过RowKey前缀）
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            String prefix = request.getCategory();
            filters.add(new PrefixFilter(Bytes.toBytes(prefix)));
        }

        // 状态筛选
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            SingleColumnValueFilter statusFilter = new SingleColumnValueFilter(
                cfBaseBytes, Bytes.toBytes("status"), CompareOperator.EQUAL,
                Bytes.toBytes(request.getStatus()));
            statusFilter.setFilterIfMissing(true);
            filters.add(statusFilter);
        }

        // 品牌筛选
        if (request.getBrand() != null && !request.getBrand().trim().isEmpty()) {
            SingleColumnValueFilter brandFilter = new SingleColumnValueFilter(
                cfBaseBytes, Bytes.toBytes("brand"), CompareOperator.EQUAL,
                new BinaryComparator(Bytes.toBytes(request.getBrand())));
            brandFilter.setFilterIfMissing(true);
            filters.add(brandFilter);
        }

        return filters;
    }

    /**
     * 获取总数（简化实现）
     */
    private long getTotalCount(ProductListQueryRequest request) {
        try (Table table = hBaseConnection.getTable(tableName)) {
            Scan scan = new Scan();

            // 设置筛选条件
            List<Filter> filters = buildListFilters(request);
            if (!filters.isEmpty()) {
                FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL, filters);
                scan.setFilter(filterList);
            }

            long count = 0;
            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    if (!result.isEmpty()) {
                        count++;
                    }
                }
            }
            return count;
        } catch (Exception e) {
            logger.warn("Failed to count products", e);
            return 0;
        }
    }

    /**
     * 解析Result为ProductSummaryDto
     */
    private ProductSummaryDto parseProductSummary(Result result) throws IOException {
        if (result == null || result.isEmpty()) {
            return null;
        }

        ProductSummaryDto summary = new ProductSummaryDto();

        // 从RowKey解析商品ID
        String rowKey = Bytes.toString(result.getRow());
        String productId = parseProductIdFromRowKey(rowKey);
        summary.setId(productId);

        // 解析基本信息
        byte[] nameBytes = result.getValue(cfBaseBytes, Bytes.toBytes("name"));
        if (nameBytes != null) {
            summary.setName(Bytes.toString(nameBytes));
        }

        byte[] categoryBytes = result.getValue(cfBaseBytes, Bytes.toBytes("category"));
        if (categoryBytes != null) {
            summary.setCategory(Bytes.toString(categoryBytes));
        }

        byte[] brandBytes = result.getValue(cfBaseBytes, Bytes.toBytes("brand"));
        if (brandBytes != null) {
            summary.setBrand(Bytes.toString(brandBytes));
        }

        byte[] priceBytes = result.getValue(cfBaseBytes, Bytes.toBytes("price"));
        if (priceBytes != null) {
            try {
                summary.setPrice(new java.math.BigDecimal(Bytes.toString(priceBytes)));
            } catch (NumberFormatException e) {
                logger.warn("Invalid price format for product: {}", productId);
            }
        }

        byte[] statusBytes = result.getValue(cfBaseBytes, Bytes.toBytes("status"));
        if (statusBytes != null) {
            summary.setStatus(Bytes.toString(statusBytes));
        }

        // 解析商品介绍（从cf_detail列族）
        byte[] descriptionBytes = result.getValue(cfDetailBytes, Bytes.toBytes("description"));
        if (descriptionBytes != null) {
            summary.setDescription(Bytes.toString(descriptionBytes));
        }

        byte[] timeBytes = result.getValue(cfBaseBytes, Bytes.toBytes("create_time"));
        if (timeBytes != null) {
            String timeStr = Bytes.toString(timeBytes);
            if (timeStr.endsWith("Z")) {
                timeStr = timeStr.substring(0, timeStr.length() - 1);
            }
            try {
                summary.setCreateTime(java.time.LocalDateTime.parse(timeStr,
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (Exception e) {
                logger.warn("Invalid time format for product: {}", productId);
            }
        }

        return summary;
    }

    /**
     * 从RowKey解析商品ID
     * RowKey格式: {category_id}_{product_id}_{timestamp}
     */
    private String parseProductIdFromRowKey(String rowKey) {
        if (rowKey == null || !rowKey.contains("_")) {
            return null;
        }

        String[] parts = rowKey.split("_");
        if (parts.length >= 2) {
            String categoryId = parts[0];
            String productId = parts[1];
            // 返回12位格式：category_id(4位) + product_id(8位)
            return String.format("%-4s%-8s", categoryId, productId).replace(' ', '0');
        }

        return null;
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
