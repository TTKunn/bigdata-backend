package com.example.bigdatabackend.config;

// import org.apache.hadoop.conf.Configuration; // 避免与Spring Configuration冲突
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;

/**
 * HDFS配置类
 */
@Configuration
public class HdfsConfig {

    private static final Logger logger = LoggerFactory.getLogger(HdfsConfig.class);

    @Value("${hdfs.namenode:hdfs://bigdata01:9000}")
    private String hdfsNameNode;

    @Value("${hdfs.replication:1}")
    private short replication;

    @Value("${hdfs.blocksize:67108864}")
    private long blockSize;

    /**
     * 配置HDFS FileSystem
     */
    @Bean
    public FileSystem hdfsFileSystem() throws IOException {
        logger.info("Initializing HDFS FileSystem with namenode: {}", hdfsNameNode);

        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();

        // 设置HDFS NameNode地址
        conf.set("fs.defaultFS", hdfsNameNode);

        // 设置副本数
        conf.set("dfs.replication", String.valueOf(replication));

        // 设置块大小（64MB）
        conf.set("dfs.blocksize", String.valueOf(blockSize));

        // 设置Hadoop用户（可选，如果需要特定的用户权限）
        System.setProperty("HADOOP_USER_NAME", "root");

        try {
            FileSystem fs = FileSystem.get(URI.create(hdfsNameNode), conf);
            logger.info("HDFS FileSystem initialized successfully");
            return fs;
        } catch (IOException e) {
            logger.error("Failed to initialize HDFS FileSystem", e);
            throw e;
        }
    }
}
