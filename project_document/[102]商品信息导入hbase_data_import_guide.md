# HBase商品数据导入指南

## 1. 图片上传到HDFS

首先，将图片文件上传到HDFS的指定目录：

```bash
# 创建商品图片目录（如果不存在）
hdfs dfs -mkdir -p /product_images

# 上传图片文件到HDFS
hdfs dfs -put /root/picture/kongtiao.png /product_images/
hdfs dfs -put /root/picture/mate60pro.png /product_images/
hdfs dfs -put /root/picture/redmibuds6.jpg /product_images/
hdfs dfs -put /root/picture/xiaomi17ultra.png /product_images/

# 验证上传成功
hdfs dfs -ls /product_images/
```

## 2. HBase表结构确认

表名：`product_info`
RowKey格式：`{category_id}_{product_id}_{timestamp}`

列族结构：
- `cf_base`：基本信息
- `cf_detail`：详细信息
- `cf_stock`：库存信息
- `cf_stat`：统计信息

## 3. 进入HBase Shell

```bash
# 进入HBase Shell
hbase shell
```

## 4. 创建表（如果不存在）

```hbase
# 创建product_info表
create 'product_info', 'cf_base', 'cf_detail', 'cf_stock', 'cf_stat'
```

## 5. 插入商品数据

### 商品1：空调 (kongtiao.png)
```hbase
# RowKey: 0001_00000001_1704625800000 (分类0001, 商品00000001, 时间戳1704625800000)
put 'product_info', '0001_00000001_1704625800000', 'cf_base:name', '美的空调'
put 'product_info', '0001_00000001_1704625800000', 'cf_base:category', '0001'
put 'product_info', '0001_00000001_1704625800000', 'cf_base:brand', '美的'
put 'product_info', '0001_00000001_1704625800000', 'cf_base:price', '2999.00'
put 'product_info', '0001_00000001_1704625800000', 'cf_base:cost', '2500.00'
put 'product_info', '0001_00000001_1704625800000', 'cf_base:status', 'ACTIVE'
put 'product_info', '0001_00000001_1704625800000', 'cf_base:create_time', '2024-01-07 12:30:00'

put 'product_info', '0001_00000001_1704625800000', 'cf_detail:description', '美的变频空调，节能省电'
put 'product_info', '0001_00000001_1704625800000', 'cf_detail:spec', '{"power":"1.5P","energy":"一级能效","feature":"变频"}'
put 'product_info', '0001_00000001_1704625800000', 'cf_detail:image', '{"id":"hdfs://bigdata01:9000/product_images/kongtiao.png","type":"main","size":102400,"upload_time":"2024-01-07 12:30:00"}'
put 'product_info', '0001_00000001_1704625800000', 'cf_detail:tags', '["空调","节能","变频"]'

put 'product_info', '0001_00000001_1704625800000', 'cf_stock:total_stock', '500'
put 'product_info', '0001_00000001_1704625800000', 'cf_stock:warehouse_stock', '450'
put 'product_info', '0001_00000001_1704625800000', 'cf_stock:safe_stock', '50'
put 'product_info', '0001_00000001_1704625800000', 'cf_stock:lock_stock', '0'

put 'product_info', '0001_00000001_1704625800000', 'cf_stat:view_count', '0'
put 'product_info', '0001_00000001_1704625800000', 'cf_stat:sale_count', '0'
put 'product_info', '0001_00000001_1704625800000', 'cf_stat:collect_count', '0'
put 'product_info', '0001_00000001_1704625800000', 'cf_stat:update_time', '2024-01-07 12:30:00'
```

### 商品2：华为Mate60 Pro (mate60pro.png)
```hbase
# RowKey: 0001_00000002_1704625800001
put 'product_info', '0001_00000002_1704625800001', 'cf_base:name', '华为Mate60 Pro'
put 'product_info', '0001_00000002_1704625800001', 'cf_base:category', '0001'
put 'product_info', '0001_00000002_1704625800001', 'cf_base:brand', '华为'
put 'product_info', '0001_00000002_1704625800001', 'cf_base:price', '6999.00'
put 'product_info', '0001_00000002_1704625800001', 'cf_base:cost', '5500.00'
put 'product_info', '0001_00000002_1704625800001', 'cf_base:status', 'ACTIVE'
put 'product_info', '0001_00000002_1704625800001', 'cf_base:create_time', '2024-01-07 12:30:01'

put 'product_info', '0001_00000002_1704625800001', 'cf_detail:description', '华为旗舰智能手机，HarmonyOS 4.0'
put 'product_info', '0001_00000002_1704625800001', 'cf_detail:spec', '{"screen":"6.82英寸","processor":"麒麟9000S","memory":"12GB+512GB","camera":"5000万像素"}'
put 'product_info', '0001_00000002_1704625800001', 'cf_detail:image', '{"id":"hdfs://bigdata01:9000/product_images/mate60pro.png","type":"main","size":245760,"upload_time":"2024-01-07 12:30:01"}'
put 'product_info', '0001_00000002_1704625800001', 'cf_detail:tags', '["旗舰机","鸿蒙系统","5G"]'

put 'product_info', '0001_00000002_1704625800001', 'cf_stock:total_stock', '1000'
put 'product_info', '0001_00000002_1704625800001', 'cf_stock:warehouse_stock', '900'
put 'product_info', '0001_00000002_1704625800001', 'cf_stock:safe_stock', '100'
put 'product_info', '0001_00000002_1704625800001', 'cf_stock:lock_stock', '0'

put 'product_info', '0001_00000002_1704625800001', 'cf_stat:view_count', '0'
put 'product_info', '0001_00000002_1704625800001', 'cf_stat:sale_count', '0'
put 'product_info', '0001_00000002_1704625800001', 'cf_stat:collect_count', '0'
put 'product_info', '0001_00000002_1704625800001', 'cf_stat:update_time', '2024-01-07 12:30:01'
```

### 商品3：Redmi Buds 6 (redmibuds6.jpg)
```hbase
# RowKey: 0002_00000001_1704625800002
put 'product_info', '0002_00000001_1704625800002', 'cf_base:name', 'Redmi Buds 6'
put 'product_info', '0002_00000001_1704625800002', 'cf_base:category', '0002'
put 'product_info', '0002_00000001_1704625800002', 'cf_base:brand', 'Redmi'
put 'product_info', '0002_00000001_1704625800002', 'cf_base:price', '199.00'
put 'product_info', '0002_00000001_1704625800002', 'cf_base:cost', '150.00'
put 'product_info', '0002_00000001_1704625800002', 'cf_base:status', 'ACTIVE'
put 'product_info', '0002_00000001_1704625800002', 'cf_base:create_time', '2024-01-07 12:30:02'

put 'product_info', '0002_00000001_1704625800002', 'cf_detail:description', 'Redmi真无线耳机，40dB主动降噪'
put 'product_info', '0002_00000001_1704625800002', 'cf_detail:spec', '{"battery":"30小时","noise_cancellation":"40dB","codec":"LHDC"}'
put 'product_info', '0002_00000001_1704625800002', 'cf_detail:image', '{"id":"hdfs://bigdata01:9000/product_images/redmibuds6.jpg","type":"main","size":153600,"upload_time":"2024-01-07 12:30:02"}'
put 'product_info', '0002_00000001_1704625800002', 'cf_detail:tags', '["耳机","降噪","真无线"]'

put 'product_info', '0002_00000001_1704625800002', 'cf_stock:total_stock', '2000'
put 'product_info', '0002_00000001_1704625800002', 'cf_stock:warehouse_stock', '1800'
put 'product_info', '0002_00000001_1704625800002', 'cf_stock:safe_stock', '200'
put 'product_info', '0002_00000001_1704625800002', 'cf_stock:lock_stock', '0'

put 'product_info', '0002_00000001_1704625800002', 'cf_stat:view_count', '0'
put 'product_info', '0002_00000001_1704625800002', 'cf_stat:sale_count', '0'
put 'product_info', '0002_00000001_1704625800002', 'cf_stat:collect_count', '0'
put 'product_info', '0002_00000001_1704625800002', 'cf_stat:update_time', '2024-01-07 12:30:02'
```

### 商品4：小米17 Ultra (xiaomi17ultra.png)
```hbase
# RowKey: 0001_00000003_1704625800003
put 'product_info', '0001_00000003_1704625800003', 'cf_base:name', '小米17 Ultra'
put 'product_info', '0001_00000003_1704625800003', 'cf_base:category', '0001'
put 'product_info', '0001_00000003_1704625800003', 'cf_base:brand', '小米'
put 'product_info', '0001_00000003_1704625800003', 'cf_base:price', '5999.00'
put 'product_info', '0001_00000003_1704625800003', 'cf_base:cost', '4800.00'
put 'product_info', '0001_00000003_1704625800003', 'cf_base:status', 'ACTIVE'
put 'product_info', '0001_00000003_1704625800003', 'cf_base:create_time', '2024-01-07 12:30:03'

put 'product_info', '0001_00000003_1704625800003', 'cf_detail:description', '小米旗舰智能手机，骁龙8 Gen3'
put 'product_info', '0001_00000003_1704625800003', 'cf_detail:spec', '{"screen":"6.73英寸","processor":"骁龙8 Gen3","memory":"12GB+512GB","camera":"5000万像素"}'
put 'product_info', '0001_00000003_1704625800003', 'cf_detail:image', '{"id":"hdfs://bigdata01:9000/product_images/xiaomi17ultra.png","type":"main","size":204800,"upload_time":"2024-01-07 12:30:03"}'
put 'product_info', '0001_00000003_1704625800003', 'cf_detail:tags', '["旗舰机","骁龙8","5G"]'

put 'product_info', '0001_00000003_1704625800003', 'cf_stock:total_stock', '800'
put 'product_info', '0001_00000003_1704625800003', 'cf_stock:warehouse_stock', '700'
put 'product_info', '0001_00000003_1704625800003', 'cf_stock:safe_stock', '100'
put 'product_info', '0001_00000003_1704625800003', 'cf_stock:lock_stock', '0'

put 'product_info', '0001_00000003_1704625800003', 'cf_stat:view_count', '0'
put 'product_info', '0001_00000003_1704625800003', 'cf_stat:sale_count', '0'
put 'product_info', '0001_00000003_1704625800003', 'cf_stat:collect_count', '0'
put 'product_info', '0001_00000003_1704625800003', 'cf_stat:update_time', '2024-01-07 12:30:03'
```

## 6. 验证数据插入

```hbase
# 查看表中的数据
scan 'product_info', {LIMIT => 10}

# 查看特定商品
get 'product_info', '0001_00000001_1704625800000'

# 查看表结构
describe 'product_info'
```

## 7. 退出HBase Shell

```hbase
quit
```

## 注意事项

1. **RowKey设计**：使用 `{category_id}_{product_id}_{timestamp}` 格式，确保唯一性
2. **时间戳**：使用13位毫秒时间戳，确保不同商品的唯一性
3. **图片路径**：确保HDFS路径正确，格式为 `hdfs://bigdata01:9000/product_images/{filename}`
4. **JSON格式**：`spec`、`tags`、`images` 字段都是JSON字符串格式
5. **分类编码**：
   - 0001：手机数码
   - 0002：音频设备
   - 0003：家用电器

按照以上步骤执行，你就可以成功将4个商品数据写入HBase数据库了。
