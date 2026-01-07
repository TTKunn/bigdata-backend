package com.example.bigdatabackend.constants;

/**
 * 订单相关常量
 */
public class OrderConstants {

    // 默认用户信息
    public static final String DEFAULT_USER_ID = "000000000001";
    public static final String DEFAULT_RECEIVER = "默认用户";
    public static final String DEFAULT_PHONE = "13800138000";
    public static final String DEFAULT_ADDRESS = "北京市朝阳区某某大厦1001室";
    public static final String DEFAULT_POSTCODE = "100000";

    // Redis Key前缀
    public static final String ORDER_SEQ_KEY_PREFIX = "order:seq:";

    // HBase表名和列族
    public static final String ORDER_TABLE_NAME = "order_history";
    public static final String CF_BASE = "cf_base";
    public static final String CF_ADDRESS = "cf_address";
    public static final String CF_ITEMS = "cf_items";
    public static final String CF_LOGISTICS = "cf_logistics";

    private OrderConstants() {
        // 私有构造函数，防止实例化
    }
}
