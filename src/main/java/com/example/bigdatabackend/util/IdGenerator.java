package com.example.bigdatabackend.util;

import com.example.bigdatabackend.constants.OrderConstants;
import com.example.bigdatabackend.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单号生成器
 * 格式：yyyyMMddHHmmss + 6位序列号
 * 示例：20260107103000000001
 */
@Component
public class IdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(IdGenerator.class);

    @Autowired
    private RedisService redisService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成分布式唯一订单号
     *
     * @return 20位订单号
     */
    public String generateOrderId() {
        // 获取当前日期时间（精确到秒）
        String dateTime = LocalDateTime.now().format(FORMATTER);

        // 生成序列号（每秒重置）
        String sequence = generateSequence(dateTime);

        String orderId = dateTime + sequence;
        logger.debug("Generated order ID: {}", orderId);

        return orderId;
    }

    /**
     * 生成6位序列号
     * 使用Redis原子递增，每秒重置
     *
     * @param dateTime 日期时间字符串（yyyyMMddHHmmss）
     * @return 6位序列号字符串
     */
    private String generateSequence(String dateTime) {
        String key = OrderConstants.ORDER_SEQ_KEY_PREFIX + dateTime;

        try {
            // Redis原子递增
            Long seq = redisService.incr(key);

            if (seq == null) {
                logger.error("Failed to generate sequence: Redis incr returned null");
                // 降级方案：使用当前毫秒数的后6位
                return String.format("%06d", System.currentTimeMillis() % 1000000);
            }

            // 设置过期时间为2秒，确保每秒重置
            redisService.expire(key, 2);

            // 格式化为6位数字，不足补0
            return String.format("%06d", seq);

        } catch (Exception e) {
            logger.error("Failed to generate sequence from Redis", e);
            // 降级方案：使用当前毫秒数的后6位
            return String.format("%06d", System.currentTimeMillis() % 1000000);
        }
    }
}
