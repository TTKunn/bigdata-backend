package com.example.bigdatabackend.config;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * HBase配置类
 */
@Configuration
public class HBaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(HBaseConfig.class);

    @Value("${hbase.zookeeper.quorum}")
    private String zookeeperQuorum;

    @Value("${hbase.zookeeper.property.clientPort:2181}")
    private String zookeeperPort;

    @Value("${hbase.client.retries.number:3}")
    private String retryNumber;

    @Value("${hbase.rpc.timeout:30000}")
    private String rpcTimeout;

    @Value("${hbase.client.operation.timeout:30000}")
    private String operationTimeout;

    @Value("${hbase.client.scanner.timeout.period:30000}")
    private String scannerTimeout;

    private Connection connection;

    @Bean
    public Connection hBaseConnection() throws IOException {
        // Set system properties to suppress Hadoop warnings on Windows
        System.setProperty("hadoop.home.dir", System.getProperty("java.io.tmpdir"));

        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();

        // 设置Zookeeper集群地址
        config.set("hbase.zookeeper.quorum", zookeeperQuorum);
        config.set("hbase.zookeeper.property.clientPort", zookeeperPort);

        // 设置重试次数
        config.set("hbase.client.retries.number", retryNumber);

        // 设置超时时间
        config.set("hbase.rpc.timeout", rpcTimeout);
        config.set("hbase.client.operation.timeout", operationTimeout);
        config.set("hbase.client.scanner.timeout.period", scannerTimeout);

        // 设置客户端配置
        config.set("hbase.client.pause", "100");
        config.set("hbase.client.max.total.tasks", "100");
        config.set("hbase.client.max.perserver.tasks", "5");
        config.set("hbase.client.max.perregion.tasks", "2");

        try {
            connection = ConnectionFactory.createConnection(config);
            logger.info("HBase connection established successfully to: {}", zookeeperQuorum);
            return connection;
        } catch (IOException e) {
            logger.error("Failed to establish HBase connection", e);
            throw e;
        }
    }

    @PreDestroy
    public void destroy() {
        if (connection != null && !connection.isClosed()) {
            try {
                connection.close();
                logger.info("HBase connection closed successfully");
            } catch (IOException e) {
                logger.error("Error closing HBase connection", e);
            }
        }
    }
}

