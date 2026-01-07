package com.example.bigdatabackend;

import com.example.bigdatabackend.model.Product;
import com.example.bigdatabackend.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品查询服务单元测试
 * 注意：需要HBase和Redis服务运行
 */
@SpringBootTest
public class ProductServiceQueryTest {

    @Autowired
    private ProductService productService;

    /**
     * 测试商品查询基本功能
     */
    @Test
    public void testGetProduct() {
        // 测试查询存在的商品（需要确保HBase中有数据）
        // RowKey: 0001_00000001_1704625800000 → 接口商品ID: 000100000001
        String testProductId = "000100000001"; // 美的空调

        Product product = productService.getProduct(testProductId);

        // 验证查询结果
        assertNotNull(product, "商品查询结果不应为空");
        assertEquals(testProductId, product.getId(), "商品ID应该匹配");
        assertNotNull(product.getName(), "商品名称不应为空");
        assertNotNull(product.getPrice(), "商品价格不应为空");
    }

    /**
     * 测试查询不存在的商品
     */
    @Test
    public void testGetProductNotFound() {
        // 测试查询不存在的商品
        String nonExistentProductId = "999999999999"; // 12位不存在的商品ID

        Product product = productService.getProduct(nonExistentProductId);

        // 验证查询结果为空
        assertNull(product, "不存在的商品查询结果应该为空");
    }

    /**
     * 测试参数校验
     */
    @Test
    public void testGetProductWithNullId() {
        // 测试传入null参数
        Product product = productService.getProduct(null);

        // 验证查询结果为空
        assertNull(product, "null商品ID的查询结果应该为空");
    }

    /**
     * 测试缓存功能
     */
    @Test
    public void testCacheFunctionality() {
        String testProductId = "000100000001"; // 美的空调

        // 第一次查询（应该从HBase读取并缓存）
        Product product1 = productService.getProduct(testProductId);
        assertNotNull(product1, "第一次查询应该成功");

        // 第二次查询（应该从缓存读取）
        Product product2 = productService.getProduct(testProductId);
        assertNotNull(product2, "第二次查询应该成功");

        // 验证两次查询结果一致
        assertEquals(product1.getId(), product2.getId(), "两次查询结果ID应该相同");
        assertEquals(product1.getName(), product2.getName(), "两次查询结果名称应该相同");
    }
}
