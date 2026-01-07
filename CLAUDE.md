# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 通用规则要求
1. 任何时候都需要阅读并遵守本节所有要求，并且在每次对话开头输出一遍。
2. 在所有流程、任务与对话过程中，使用 **`memory` MCP** 记录交互节点与关键决策，确保完整可追溯。
3. 所有回答均需使用简体中文。
4. 如有需要可查阅线上资料以辅助开发决策。
5. 非经明确要求禁止编写测试文件与说明文件；若确实需要测试文件，须放入新建的 `test/` 目录且仅允许保留一个长期测试文件，其余临时测试文件在验证通过后必须删除。
6. 当需要删除本地文件（或运行会执行删除操作的脚本）时，必须先说明“我想要删除[文件名]，这些文件原本用于[用途]，现在由于[原因]已经不需要了”或“我要运行[文件名]，这将删除[所有删除的文件名]，这些文件原本用于[用途]，现在由于[原因]已经不需要了”，并获得许可后才能执行。
7. 所有代码中的注释需使用中文进行讲解。
8. 每次工作流完成后，都要查看并更新 `[000]功能模块文档.md`，及时同步功能模块信息。
9. 对于每个完整任务（无论大小）需严格遵循 RIPER-5 阶段性工作流开发规范，分阶段进行内容总结汇报；若仅为普通问答可直接回复。
10. 遵照时间戳原则，将最终确认的详细计划（具体到每一步的详细规划和操作而不止是总结） (todolist) 保存为独立文件至 `/project_document/` 目录中。文件名必须包含唯一标识和简要信息，格式为 `[编号]简要任务描述.md` (例如: `[001]用户登录功能开发.md`)。 需要注意的是，编号为000-099的文档为核心文档，例如api文档、数据库设计文档、项目架构文档等；编号为100-199的文档为阶段性文档，主要包括项目中各个阶段模块的实施方案、技术方案、实现报告之类的内容；编号为200-299的文档为测试修复完善阶段文档，主要为各个模块的功能测试内容或者是bug修复方案、模块完善升级方案、修复报告之类的内容；编号300-399为技术专题文档，包括对某些技术使用的说明教学等内容。对于开发文档内容，在开头你需要标注背景与目标方便翻阅，还要写上目录，如果是开发文档，你还需要将todolist写进去。
12. 每个完整任务结束后，如项目使用 Git 管理，需要创建提交记录以便追踪。

## 项目概述

这是一个基于大数据技术栈的分布式商城后端系统学习项目，主要技术栈包括：
- **后端框架**: Spring Boot 4.0.1 + Java 17
- **大数据存储**: HBase 2.4.17 (商品、订单数据)
- **缓存层**: Redis 6.x (库存、购物车、排行榜)
- **分布式文件系统**: HDFS (商品图片存储)
- **集群协调**: Zookeeper 3.x

**重要说明**: 本项目侧重大数据技术学习与应用验证，简化了用户认证、商品完整管理等功能，使用单一默认用户，商品创建接口仅在开发环境暴露。

## 开发命令

### 构建和运行
```bash
# 编译项目
./mvnw clean compile

# 运行应用（开发环境）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 打包
./mvnw clean package

# 运行测试
./mvnw test
```

### Maven依赖管理
```bash
# 查看依赖树
./mvnw dependency:tree

# 更新依赖
./mvnw clean install
```

### 大数据环境检查
```bash
# 检查服务健康状态（应用运行后）
curl http://localhost:8080/api/dev/product/health

# Windows环境可以使用批处理测试
test_product_creation.bat
```

## 核心架构

### 三层架构设计

```
Controller (ProductController)
    ↓
Service (ProductService) - 业务协调层
    ↓
├── ProductHBaseService - HBase数据持久化
├── RedisService - 缓存和实时数据
└── HdfsService - 图片文件存储
```

### 数据流向

**商品创建流程**:
1. Controller接收请求（支持JSON和Form Data两种格式）
2. Service层参数校验
3. HdfsService上传图片到HDFS并返回路径
4. 构建Product对象（包含HDFS图片路径）
5. ProductHBaseService保存到HBase
6. RedisService初始化缓存（商品信息+库存）
7. 返回响应（包含图片元数据）

**商品查询流程**:
1. 先从Redis缓存读取
2. 缓存未命中则从HBase读取
3. 回填缓存并返回

**库存扣减流程**:
1. Redis原子扣减库存
2. 异步同步到HBase（TODO）

### 关键设计点

#### HBase RowKey设计
```
商品表: {category_id}_{product_id}_{timestamp}
- category_id: 4位分类ID
- product_id: 8位商品ID
- timestamp: 13位时间戳
- 总计: 12位业务ID（不含timestamp）
```

#### Redis数据结构
```redis
# 商品信息缓存 (Hash)
product:cache:{productId} → {name, price, category, brand, status}

# 库存 (String)
stock:{productId} → quantity

# 购物车 (Hash)
cart:{userId} → {product_id: {quantity, add_time, selected}}

# 排行榜 (Sorted Set)
rank:daily:sale → {score: productId}
```

#### HDFS存储路径
```
商品图片: /product_images/{category_id}/{product_id}/{timestamp}_{filename}
每个商品限制: 单张主图，最大10MB
```

## 配置管理

### 环境配置文件
- `application.properties` - 通用配置（HBase/Redis/HDFS连接信息）
- `application-dev.properties` - 开发环境配置（启用dev接口、详细日志）

### 关键配置项
```properties
# Profile激活
spring.profiles.active=dev

# 大数据集群地址
hbase.zookeeper.quorum=192.168.32.200:2181,192.168.32.201:2181,192.168.32.202:2181
redis.host=192.168.32.200
hdfs.namenode=hdfs://192.168.32.200:9000

# 开发环境表名
hbase.product.table.name=product_info_dev  # dev环境
hbase.product.table.name=product_info      # 生产环境

# 文件上传限制
spring.servlet.multipart.max-file-size=10MB
```

## 代码结构

### 包组织
```
com.example.bigdatabackend/
├── config/          - 配置类（HBase, Redis, HDFS, Swagger）
├── controller/      - REST控制器（仅@Profile("dev")）
├── service/         - 业务服务层
├── model/           - 领域模型（Product, ProductStock, ProductImage等）
└── dto/             - 数据传输对象（请求/响应）
```

### 核心类说明

**ProductService**:
- 业务协调者，编排HBase/Redis/HDFS三个组件的操作
- 提供事务管理（@Transactional）
- 图片上传失败时自动清理（cleanupUploadedImages）

**ProductHBaseService**:
- 封装HBase Table API
- 列族结构: cf_base, cf_detail, cf_stock, cf_stat
- 使用Gson序列化复杂对象（spec, tags, images）

**RedisService**:
- 使用Jedis客户端
- 提供原子操作（deductStock使用Lua脚本）
- 缓存过期时间可配置

**HdfsService**:
- 管理FileSystem实例
- 处理图片上传（Base64解码、路径生成）
- 返回HDFS完整路径作为图片ID

## 开发规范

### API接口限制
- **所有接口仅在dev环境启用**: 使用`@Profile("dev")`注解
- **路径前缀**: `/api/dev/product`
- **不要在生产环境暴露商品创建接口**

### 数据校验
- 使用Jakarta Validation注解（@Valid）
- 商品ID必须12位
- 价格必须 > 0.01
- 图片文件最大10MB

### 错误处理
- 参数错误返回400
- 资源不存在返回404
- 资源冲突（如商品ID重复、库存不足）返回409
- 系统错误返回500

### 日志规范
```java
logger.info("Starting product creation for productId: {}", request.getId());
logger.debug("Request validation passed for productId: {}", request.getId());
logger.warn("Invalid request parameters for product creation: {}", e.getMessage());
logger.error("Failed to create product: {}", request.getId(), e);
```

## 测试

### 接口测试工具
推荐使用**ApiFox**或**Postman**

### 创建商品测试（Form Data方式）
```
POST http://localhost:8080/api/dev/product
Content-Type: multipart/form-data

Form Data:
- id: 00010001
- name: 华为Mate60 Pro
- category: 0001
- brand: 华为
- price: 6999.00
- cost: 5500.00
- description: 华为旗舰智能手机
- spec: {"screen":"6.82英寸","processor":"麒麟9000S"}
- tags: ["旗舰机","鸿蒙系统","5G"]
- stock: {"total":1000,"safe":100,"warehouse":"BJ001"}
- image: [选择图片文件]
- imageType: main
```

### 查询商品
```bash
GET http://localhost:8080/api/dev/product/00010001
```

### 扣减库存
```bash
PUT http://localhost:8080/api/dev/product/00010001/stock?quantity=1
```

## 环境依赖

### 大数据集群要求
需要预先搭建并启动3节点集群：
- **BigData01** (192.168.32.200): NameNode, HBase Master, Zookeeper, Redis
- **Hadoop-Slave01** (192.168.32.201): DataNode, RegionServer, Zookeeper
- **Hadoop-Slave02** (192.168.32.202): DataNode, RegionServer, Zookeeper

### 启动前检查
```bash
# 检查HBase表是否存在（开发环境使用product_info_dev）
# 检查Redis连接: redis-cli -h 192.168.32.200 -a 123456
# 检查HDFS: hdfs dfs -ls /
```

### HBase表初始化
需要手动创建表（如果不存在）：
```bash
create 'product_info_dev', 'cf_base', 'cf_detail', 'cf_stock', 'cf_stat'
```

## 技术注意事项

### Hadoop依赖版本兼容性
- HBase 2.4.17 + Hadoop 2.10.2（严格匹配）
- 已排除冲突的slf4j-log4j12依赖
- 使用Hadoop用户名: root (HADOOP_USER_NAME环境变量)

### Redis连接池配置
```properties
redis.pool.max-total=20      # 最大连接数
redis.pool.max-idle=10       # 最大空闲连接
redis.pool.min-idle=5        # 最小空闲连接
redis.pool.test-on-borrow=true  # 获取连接时测试
```

### 文件上传处理
- 支持两种格式: `application/json` (Base64) 和 `multipart/form-data` (文件流)
- Form Data会自动转换为标准请求对象（convertFormDataToRequest）
- 使用Gson解析JSON字符串字段

### 事务和回滚
- @Transactional仅作为标记，实际未启用JPA事务
- 图片上传失败时手动调用cleanupUploadedImages回滚HDFS文件
- Redis缓存失败不影响主流程（catch并log）

## 常见问题

### 1. HBase连接超时
- 检查虚拟机防火墙是否关闭
- 检查Windows hosts文件是否配置主机名映射
- 检查Zookeeper服务是否正常

### 2. HDFS图片上传失败
- 检查HDFS目录权限: `hdfs dfs -chmod -R 777 /product_images`
- 检查NameNode服务状态
- 检查网络连通性

### 3. Redis缓存未生效
- 检查Redis服务状态: `redis-cli -h 192.168.32.200 ping`
- 检查密码配置是否正确
- 查看日志确认缓存初始化是否成功

### 4. 商品ID格式错误
- 必须是12位字符: 前4位分类ID + 后8位商品ID
- 示例: `00010001` (分类0001, 商品00000001)

## 扩展开发指南

### 添加新的API接口
1. 在ProductController添加方法并使用@Profile("dev")
2. 在ProductService实现业务逻辑
3. 根据需要调用HBase/Redis/HDFS服务
4. 更新Swagger注解
5. 在API文档中添加接口说明

### 实现库存同步
当前库存扣减仅更新Redis，HBase同步标记为TODO：
```java
// ProductService.updateProductStock
// TODO: 异步更新HBase中的库存信息
// 建议使用消息队列（如Kafka）实现最终一致性
```

### 添加新的HBase列族
1. 在HBase shell中alter表结构
2. 在Product模型类添加对应字段
3. 在ProductHBaseService的序列化/反序列化逻辑中处理新列族
4. 更新Redis缓存逻辑（如需要）
