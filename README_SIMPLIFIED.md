# 大数据商城后端系统 - 简化版启动指南

## 快速启动

### 1. 启动应用（最简单方式）
```bash
mvn spring-boot:run
```

### 2. 测试商品查询接口
```bash
# 查询商品（美的空调）
curl http://localhost:8080/api/product/000100000001

# 健康检查
curl http://localhost:8080/api/product/health
```

## 接口路径说明

**简化前**：需要激活dev profile
- 商品查询：`GET /api/dev/product/{id}`
- 前提：启动时需要 `-Dspring-boot.run.profiles=dev`

**简化后**：直接可用，无需配置
- 商品查询：`GET /api/product/{id}`
- 前提：直接 `mvn spring-boot:run` 启动即可

## 测试数据

基于HBase实际存储的商品数据：

| 商品ID | 商品名称 | HBase RowKey |
|--------|----------|--------------|
| 000100000001 | 美的空调 | 0001_00000001_* |
| 000100000002 | 华为Mate60 Pro | 0001_00000002_* |
| 000100000003 | 小米17 Ultra | 0001_00000003_* |
| 000200000001 | Redmi Buds 6 | 0002_00000001_* |

## 注意事项

1. **商品ID格式**：必须是12位字符，前4位分类ID，后8位商品ID
2. **HBase/Redis服务**：确保大数据集群服务正常运行
3. **端口占用**：确保8080端口未被占用

## 常见问题

### Q: 返回404错误
A: 检查应用是否正常启动，访问 `http://localhost:8080/api/product/health` 确认

### Q: 返回500错误
A: 检查HBase和Redis连接，查看应用日志中的错误信息

### Q: 商品查询为空
A: 确认商品ID格式正确，且HBase中存在对应数据
