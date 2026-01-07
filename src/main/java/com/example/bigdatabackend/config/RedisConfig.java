package com.example.bigdatabackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * Redis配置类
 */
@Configuration
public class RedisConfig {

    @Value("${redis.host:192.168.32.200}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.password:123456}")
    private String redisPassword;

    @Value("${redis.database:0}")
    private int redisDatabase;

    @Value("${redis.timeout:2000}")
    private int redisTimeout;

    @Value("${redis.pool.max-total:20}")
    private int maxTotal;

    @Value("${redis.pool.max-idle:10}")
    private int maxIdle;

    @Value("${redis.pool.min-idle:5}")
    private int minIdle;

    @Value("${redis.pool.max-wait-millis:3000}")
    private long maxWaitMillis;

    @Value("${redis.pool.test-on-borrow:true}")
    private boolean testOnBorrow;

    @Value("${redis.pool.test-on-return:false}")
    private boolean testOnReturn;

    @Value("${redis.pool.test-while-idle:true}")
    private boolean testWhileIdle;

    /**
     * 配置Jedis连接池
     */
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        // 禁用JMX监控以避免重复注册问题
        poolConfig.setJmxEnabled(false);

        // 最大连接数
        poolConfig.setMaxTotal(maxTotal);

        // 最大空闲连接数
        poolConfig.setMaxIdle(maxIdle);

        // 最小空闲连接数
        poolConfig.setMinIdle(minIdle);

        // 最大等待时间
        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMillis));

        // 获取连接时的验证
        poolConfig.setTestOnBorrow(testOnBorrow);

        // 归还连接时的验证
        poolConfig.setTestOnReturn(testOnReturn);

        // 空闲连接检测
        poolConfig.setTestWhileIdle(testWhileIdle);

        return new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, redisPassword, redisDatabase);
    }
}
