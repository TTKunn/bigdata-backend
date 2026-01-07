# [107] 购物车Redis+HBase双重存储技术方案文档

## 文档信息

| 文档编号 | [107] |
|----------|-------|
| 文档名称 | 购物车Redis+HBase双重存储技术方案文档 |
| 版本号 | 1.0 |
| 创建日期 | 2026-01-07 |
| 作者 | 系统架构师 |
| 项目名称 | 大数据商城后端系统 |
| 背景 | 解决购物车数据仅存储在Redis导致数据丢失的问题，引入HBase持久化存储 |

## 目录

1. [问题分析](#问题分析)
2. [解决方案设计](#解决方案设计)
3. [HBase表结构设计](#HBase表结构设计)
4. [数据同步策略](#数据同步策略)
5. [服务层架构修改](#服务层架构修改)
6. [性能优化策略](#性能优化策略)
7. [异常处理和容错](#异常处理和容错)
8. [实施计划](#实施计划)

## 问题分析

### 当前问题

1. **数据不持久化**：购物车数据仅存储在Redis中，重启服务器或Redis故障会导致数据丢失
2. **用户体验差**：用户添加的购物车商品可能会在系统维护或故障后丢失
3. **业务不合理**：购物车作为核心功能，其数据应该具备持久化特性

### 业务影响

- **用户流失**：频繁的购物车数据丢失会降低用户购物意愿
- **商业损失**：购物车是电商系统的核心转化环节，数据丢失直接影响销售额
- **运维风险**：Redis单点故障可能导致大规模数据丢失

### 技术债务

- **架构不完整**：缺少数据持久化层的架构设计
- **容错性不足**：无法应对缓存层故障的场景
- **扩展性受限**：纯内存存储无法支持大数据量的历史分析

## 解决方案设计

### 整体架构

```
用户操作 → Controller → CartService → 双重存储层
                                    ├── Redis缓存层（快速读写）
                                    └── HBase持久化层（数据持久化）

数据流向：
写入：Redis ←→ HBase（异步同步）
读取：Redis → HBase（缓存穿透）
删除：Redis + HBase（同步删除）
```

### 存储策略

#### 1. Redis缓存层
- **作用**：提供快速的数据读写访问
- **数据结构**：保持现有的Hash结构不变
- **过期策略**：7天过期（活跃数据）
- **优势**：读写性能高，适合频繁操作

#### 2. HBase持久化层
- **作用**：提供数据的持久化存储
- **数据结构**：结构化存储，支持复杂查询
- **存储期限**：长期保存，支持历史数据分析
- **优势**：高可用，高容错，适合海量数据

### 双重存储模式

#### 读操作策略（Cache-Aside模式）
```
1. 优先从Redis读取数据
2. Redis缓存命中：直接返回
3. Redis缓存未命中：从HBase加载数据
4. 将HBase数据回填到Redis缓存
5. 返回数据
```

#### 写操作策略（Write-Through模式）
```
1. 同时写入Redis和HBase
2. Redis写入成功后，异步写入HBase
3. 保证数据最终一致性
4. 异常情况下有补偿机制
```

#### 删除操作策略（同步删除）
```
1. 同时从Redis和HBase删除数据
2. 保证两层数据的一致性
```

## HBase表结构设计

### 表基本信息

**表名**：`cart_data`

**设计原则**：
- 支持按用户查询购物车数据
- 支持商品级别的增删改操作
- 支持数据版本控制和审计

### RowKey设计

**格式**：`{user_id}`
- user_id：用户ID（12位，默认000000000001）

**设计优势**：
- 一个用户只有一行记录，避免数据冗余
- 直接通过RowKey精确查询，性能最优
- 更新操作直接覆盖，无需扫描多行
- 简化数据管理和维护

**设计理由**：
- 购物车是用户维度的数据，一个用户只需要一个购物车
- 使用HBase的Put操作天然支持覆盖更新
- 历史版本通过HBase的VERSIONS机制保留（可选）

**示例**：
```
000000000001  # 用户000000000001的购物车（唯一记录）
000000000002  # 用户000000000002的购物车（唯一记录）
```

**注意事项**：
- 如果需要保留购物车历史记录，可以设置列族VERSIONS > 1
- 当前方案设置VERSIONS=3，保留最近3次修改记录

### 列族设计

#### 列族：cf_items（商品明细）
```
cf_items:
├── product_{productId}: 商品详细信息JSON
├── update_time: 最后更新时间戳
├── version: 数据版本号（用于乐观锁）
```

#### 商品明细数据结构
```json
{
  "productId": "000100000002",
  "productName": "华为Mate60 Pro",
  "category": "0001",
  "brand": "华为",
  "price": 6999.00,
  "quantity": 2,
  "selected": true,
  "addTime": 1704611400000
}
```

#### 列族：cf_meta（元数据）
```
cf_meta:
├── create_time: 购物车创建时间
├── update_time: 最后更新时间
├── total_items: 商品总数量
├── total_amount: 商品总金额
├── status: 购物车状态（ACTIVE/INACTIVE）
```

### 表创建脚本

```hbase
# 创建表
create 'cart_data', 'cf_items', 'cf_meta'

# 设置列族属性
alter 'cart_data', {NAME => 'cf_items', VERSIONS => 3, COMPRESSION => 'SNAPPY'}
alter 'cart_data', {NAME => 'cf_meta', VERSIONS => 1, COMPRESSION => 'SNAPPY'}
```

**说明**：
- `VERSIONS => 3`: 保留最近3个版本，支持数据回滚和审计
- `COMPRESSION => 'SNAPPY'`: 启用Snappy压缩，节省存储空间
- 不设置TTL，购物车数据永久保存

## 数据同步策略

### 同步模式选择

#### 方案一：同步双写（Write-Through）
```
优点：数据一致性强，读取性能高
缺点：写入性能较低，增加故障风险
适用场景：对一致性要求极高的场景
```

#### 方案二：异步双写（Write-Behind）
```
优点：写入性能高，用户体验好
缺点：存在数据不一致窗口期
适用场景：允许短暂不一致的业务场景
```

#### 方案三：缓存预热（Cache Warming）
```
优点：启动时预热热点数据
缺点：启动时间长，冷数据处理不佳
适用场景：数据量可控的场景
```

### 推荐方案：同步双写（参考商品模块）

#### 写入流程（简单直接）
```
1. 数据同步写入Redis（缓存层）
2. 数据同步写入HBase（持久化层）
3. 两者都成功后返回成功响应
4. 任一失败则回滚并返回错误
```

**设计理由**：
- 参考商品模块的成熟实现方式
- 逻辑简单,易于理解和维护
- 数据强一致性,无需补偿机制
- 性能足够(HBase写入<10ms)

#### 读取流程（Cache-Aside模式）
```
1. 优先从Redis读取（缓存命中）
2. Redis未命中，从HBase读取
3. 将HBase数据回填到Redis
4. 返回数据给用户
```

**设计理由**：
- 与商品模块查询逻辑一致
- 充分利用Redis缓存提升性能
- HBase作为数据源保证数据不丢失

#### 数据一致性保障

##### 1. 强一致性保证
- 同步双写,保证Redis和HBase数据实时一致
- 写入失败时事务回滚,不会出现数据不一致
- 无需补偿机制,降低系统复杂度

##### 2. 异常处理策略
- Redis写入失败：直接返回错误,不写HBase
- HBase写入失败：删除Redis数据,返回错误
- 保证"要么都成功,要么都失败"

##### 3. 降级策略
- Redis故障时：直接读写HBase,性能稍降但功能可用
- HBase故障时：仅使用Redis,提示用户数据暂不持久化

## 服务层架构修改

### 新增服务类

#### CartHBaseService（参考ProductHBaseService）
```java
@Service
public class CartHBaseService {

    @Autowired
    private Connection hBaseConnection;

    private static final String TABLE_NAME = "cart_data";
    private static final byte[] CF_ITEMS = Bytes.toBytes("cf_items");
    private static final byte[] CF_META = Bytes.toBytes("cf_meta");

    private final Gson gson = new Gson();

    /**
     * 保存购物车到HBase（同步操作）
     */
    public void saveCart(String userId, List<CartItem> items) throws IOException {
        String rowKey = userId;

        try (Table table = hBaseConnection.getTable(TableName.valueOf(TABLE_NAME))) {
            Put put = new Put(Bytes.toBytes(rowKey));

            // 写入商品明细
            for (CartItem item : items) {
                String column = "product_" + item.getProductId();
                put.addColumn(CF_ITEMS, Bytes.toBytes(column),
                    Bytes.toBytes(gson.toJson(item)));
            }

            // 写入元数据
            put.addColumn(CF_META, Bytes.toBytes("update_time"),
                Bytes.toBytes(System.currentTimeMillis()));
            put.addColumn(CF_META, Bytes.toBytes("total_items"),
                Bytes.toBytes(items.size()));

            table.put(put);
            logger.info("Successfully saved cart to HBase: userId={}", userId);
        }
    }

    /**
     * 从HBase加载购物车（同步操作）
     */
    public List<CartItem> loadCart(String userId) throws IOException {
        String rowKey = userId;

        try (Table table = hBaseConnection.getTable(TableName.valueOf(TABLE_NAME))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addFamily(CF_ITEMS);

            Result result = table.get(get);
            if (result.isEmpty()) {
                return new ArrayList<>();
            }

            List<CartItem> items = new ArrayList<>();
            for (Cell cell : result.listCells()) {
                String qualifier = Bytes.toString(CellUtil.cloneQualifier(cell));
          if (qualifier.startsWith("product_")) {
                    String json = Bytes.toString(CellUtil.cloneValue(cell));
                    CartItem item = gson.fromJson(json, CartItem.class);
                    items.add(item);
                }
            }

            logger.info("Successfully loaded cart from HBase: userId={}, items={}",
                userId, items.size());
            return items;
        }
    }

    /**
     * 删除购物车数据（同步操作）
     */
    public void deleteCart(String userId) throws IOException {
        String rowKey = userId;

        try (Table table = hBaseConnection.getTable(TableName.valueOf(TABLE_NAME))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
            logger.info("Successfully deleted cart from HBase: userId={}", userId);
        }
    }
}
```

**设计说明**：
- 完全参考ProductHBaseService的实现方式
- 所有操作都是同步的,无异步处理
- 使用Gson序列化,与商品模块保持一致
- 异常直接抛出,由上层统一处理

### 修改现有服务

#### CartService接口（保持不变）
```java
public interface CartService {
    void addItem(String productId, Integer quantity);
    CartResponse getCart();
    void updateItem(String productId, Integer quantity);
    void removeItems(List<String> productIds);
    void clearCart();
}
```

#### CartServiceImpl修改（参考ProductService）
```java
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private CartHBaseService cartHBaseService;

    @Autowired
    private ProductService productService;

    private static final String DEFAULT_USER_ID = "000000000001";
    private static final String CART_KEY_PREFIX = "cart:";

    private final Gson gson = new Gson();

    /**
     * 添加商品到购物车（同步双写）
     */
    @Override
    @Transactional
    public void addItem(String productId, Integer quantity) {
        logger.info("Adding item to cart: productId={}, quantity={}", productId, quantity);

        // 1. 验证商品和库存（不变）
        Product product = productService.getProduct(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        Integer stock = redisService.getStock(productId);
        if (stock == null || stock < quantity) {
            throw new IllegalArgumentException("商品库存不足");
        }

        // 2. 更新Redis缓存
        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;
        String field = productId;

        String existingItem = redisService.hget(cartKey, field);
        CartItem cartItem;

        if (existingItem != null) {
            cartItem = gson.fromJson(existingItem, CartItem.class);
            int newQuantity = cartItem.getQuantity() + quantity;
            if (stock < newQuantity) {
                throw new IllegalArgumentException("商品库存不足");
            }
            cartItem.setQuantity(newQuantity);
        } else {
            cartItem = new CartItem();
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setAddTime(System.currentTimeMillis());
            cartItem.setSelected(true);
        }

        redisService.hset(cartKey, field, gson.toJson(cartItem));
        redisService.expire(cartKey, 604800); // 7天

        // 3. 同步写入HBase
        try {
            List<CartItem> allItems = getAllCartItems();
            cartHBaseService.saveCart(DEFAULT_USER_ID, allItems);
            logger.info("Successfully added item and synced to HBase");
        } catch (IOException e) {
            // HBase写入失败,回滚Redis
            logger.error("Failed to save cart to HBase, rolling back Redis", e);
            redisService.hdel(cartKey, field);
            throw new RuntimeException("购物车保存失败", e);
        }
    }

    /**
     * 查询购物车（缓存穿透）
     */
    @Override
    public CartResponse getCart() {
        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;

        // 1. 优先从Redis读取
        Map<String, String> cartData = redisService.hgetAll(cartKey);

        if (cartData != null && !cartData.isEmpty()) {
            logger.debug("Cart found in Redis");
            return buildCartResponse(cartData);
        }

        // 2. Redis未命中,从HBase加载
        logger.debug("Cart not found in Redis, loading from HBase");
        try {
            List<CartItem> items = cartHBaseService.loadCart(DEFAULT_USER_ID);

            if (!items.isEmpty()) {
                // 3. 回填到Redis
                for (CartItem item : items) {
                    redisService.hset(cartKey, item.getProductId(),
                        gson.toJson(item));
                }
                redisService.expire(cartKey, 604800);
                logger.info("Cart loaded from HBase and cached to Redis");
            }

            return buildCartResponseFromItems(items);
        } catch (IOException e) {
            logger.error("Failed to load cart from HBase", e);
            return createEmptyCartResponse();
        }
    }

    /**
     * 清空购物车（同步删除）
     */
    @Override
    @Transactional
    public void clearCart() {
        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;

        // 1. 删除Redis
        redisService.del(cartKey);

        // 2. 同步删除HBase
        try {
            cartHBaseService.deleteCart(DEFAULT_USER_ID);
            logger.info("Successfully cleared cart from both Redis and HBase");
        } catch (IOException e) {
            logger.error("Failed to delete cart from HBase", e);
            // HBase删除失败不影响用户使用,仅记录日志
        }
    }

    /**
     * 获取购物车所有商品（内部方法）
     */
    private List<CartItem> getAllCartItems() {
        String cartKey = CART_KEY_PREFIX + DEFAULT_USER_ID;
        Map<String, String> cartData = redisService.hgetAll(cartKey);

        List<CartItem> items = new ArrayList<>();
        if (cartData != null) {
            for (String json : cartData.values()) {
                items.add(gson.fromJson(json, CartItem.class));
            }
        }
        return items;
    }
}
```

**设计说明**：
- 完全参考ProductService的实现模式
- 写入操作：同步双写Redis和HBase
- 读取操作：优先Redis,未命中则从HBase加载
- 删除操作：同步删除Redis和HBase
- 使用@Transactional保证操作原子性
- HBase写入失败时回滚Redis数据

## 性能优化策略

### 1. 缓存优化

#### Redis缓存策略
- **缓存键设计**：`cart:{userId}` 保持不变
- **过期时间**：7天，定期自动清理
- **内存优化**：使用压缩存储大JSON对象
- **连接池**：复用Redis连接，提高性能

#### 缓存更新策略
- **懒加载**：查询时才加载到缓存
- **预热机制**：系统启动时预热活跃用户数据
- **智能过期**：基于访问频率调整过期时间

### 2. HBase查询优化

#### RowKey优化
- 使用用户ID作为RowKey，支持精确查询（Get操作）
- 避免使用Scan操作，提高查询效率
- 单用户单行设计，避免数据分散

#### 查询优化
- 使用Get操作直接获取用户购物车，响应时间<10ms
- 指定列族查询，减少数据传输量
- 使用批量Get支持多用户查询（如果需要）

#### 写入优化
- 使用Put操作覆盖更新，无需先查询
- 批量Put操作提高写入效率
- 启用WAL（Write-Ahead Log）保证数据可靠性

### 3. 批量操作优化

#### 批量写入优化
- 购物车更新时一次性写入所有商品
- 使用HBase的Put操作覆盖整行
- 减少网络往返次数

#### 批量读取优化
- 一次性读取用户的完整购物车
- 使用Get操作精确查询
- 避免多次查询

### 4. 监控和告警

#### 性能监控
- Redis命中率监控
- HBase查询响应时间
- 异步任务处理延迟

#### 业务监控
- 购物车操作成功率
- 数据同步成功率
- 用户活跃度统计

## 异常处理和容错

### 异常类型

#### 缓存层异常
```java
public class CacheException extends RuntimeException {
    // Redis连接失败、序列化失败等
}
```

#### 持久化层异常
```java
public class PersistenceException extends RuntimeException {
    // HBase连接失败、写入失败等
}
```

#### 数据同步异常
```java
public class DataSyncException extends RuntimeException {
    // 异步同步失败、补偿失败等
}
```

### 容错策略

#### 1. 降级处理
- **Redis故障**：直接读HBase，返回数据（性能稍差）
- **HBase故障**：启用只读模式，只允许查询操作
- **同步失败**：记录失败任务，稍后补偿

#### 2. 数据补偿
- **定时任务**：每5分钟检查并修复数据不一致
- **手动补偿**：提供管理接口手动触发数据修复
- **日志记录**：详细记录所有数据操作，便于问题排查

#### 3. 事务控制
- **本地事务**：使用Spring @Transactional控制
- **分布式事务**：重要操作使用Saga模式保证一致性
- **回滚机制**：操作失败时自动回滚已修改的数据

## 实施计划

### 第一阶段：HBase表准备（1天）

#### 任务清单
- [ ] 创建cart_data表
- [ ] 配置列族属性
- [ ] 验证表结构正确性
- [ ] 准备测试数据

### 第二阶段：核心服务开发（2天）

#### Day 1：HBase服务层
- [ ] 实现CartHBaseService
- [ ] 实现saveCart方法（保存购物车）
- [ ] 实现loadCart方法（加载购物车）
- [ ] 实现deleteCart方法（删除购物车）
- [ ] 测试HBase基本操作

#### Day 2：CartService重构
- [ ] 修改CartServiceImpl实现双重存储
- [ ] 修改addItem方法（同步双写）
- [ ] 修改getCart方法（缓存穿透）
- [ ] 修改updateItem方法（同步双写）
- [ ] 修改removeItems方法（同步双写）
- [ ] 修改clearCart方法（同步删除）
- [ ] 测试购物车基本操作

### 第三阶段：测试和优化（2天）

#### Day 3：功能测试
- [ ] 测试双重存储功能
- [ ] 测试缓存穿透机制
- [ ] 测试数据一致性
- [ ] 测试异常场景（Redis故障、HBase故障）
- [ ] 性能测试

#### Day 4：系统集成测试
- [ ] 与订单系统集成测试
- [ ] 数据一致性验证
- [ ] 完整业务流程测试
- [ ] 文档更新

### 第四阶段：部署和监控（1天）

#### Day 5：部署和上线
- [ ] 生产环境部署
- [ ] 监控系统配置
- [ ] 告警规则设置
- [ ] 运维文档编写

### 总计时间：5天

### 风险控制

#### 1. 技术风险
- **HBase性能问题**：通过优化RowKey和使用Get操作解决
- **数据一致性问题**：通过同步双写和事务控制解决
- **回滚复杂度**：HBase写入失败时需要回滚Redis

#### 2. 业务风险
- **数据丢失**：通过双重存储保障,Redis和HBase互为备份
- **服务不可用**：通过降级策略保证基本功能可用
- **性能下降**：HBase写入增加约5-10ms响应时间

#### 3. 实施风险
- **学习成本**：HBase操作相对简单,参考商品模块即可
- **测试难度**：需要测试Redis和HBase的各种组合场景
- **回滚难度**：修改范围适中,可以分步实施

### 验收标准

#### 1. 功能验收
- [ ] 购物车数据持久化存储
- [ ] Redis缓存正常工作
- [ ] 数据同步机制正常
- [ ] 异常情况正确处理

#### 2. 性能验收
- [ ] 购物车操作响应时间<100ms
- [ ] 支持100+并发操作
- [ ] 数据一致性>99.9%
- [ ] 系统可用性>99.9%

#### 3. 质量验收
- [ ] 单元测试覆盖率>85%
- [ ] 集成测试通过率100%
- [ ] 代码审查通过
- [ ] 文档完整准确

---

**文档状态**：完成
**审核状态**：待审核
**版本控制**：
- v1.0 (2026-01-07)：完成购物车Redis+HBase双重存储技术方案设计
