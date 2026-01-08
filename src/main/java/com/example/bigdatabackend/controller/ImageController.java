package com.example.bigdatabackend.controller;

import com.example.bigdatabackend.service.HdfsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 图片访问控制器
 * 提供HDFS图片的HTTP访问接口
 */
@RestController
@RequestMapping("/api/images")
@Api(tags = "图片访问管理")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    @Autowired
    private HdfsService hdfsService;

    /**
     * 获取商品图片
     * 从HDFS读取图片并以HTTP流返回
     *
     * @param filename 图片文件名（如：kongtiao.png）
     * @return 图片数据流
     */
    @GetMapping("/{filename}")
    @ApiOperation("获取商品图片")
    public ResponseEntity<byte[]> getImage(
            @ApiParam("图片文件名") @PathVariable String filename) {

        logger.info("Received request to get image: {}", filename);

        try {
            // 从HDFS读取图片数据
            byte[] imageData = hdfsService.readImage(filename);

            if (imageData == null || imageData.length == 0) {
                logger.warn("Image not found or empty: {}", filename);
                return ResponseEntity.notFound().build();
            }

            // 根据文件扩展名确定Content-Type
            String contentType = getContentType(filename);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(imageData.length);
            // 设置缓存控制，提高性能
            headers.setCacheControl("public, max-age=86400"); // 缓存1天

            logger.info("Successfully retrieved image: {}, size: {} bytes", filename, imageData.length);

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Failed to read image from HDFS: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error while getting image: {}", filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名确定Content-Type
     *
     * @param filename 文件名
     * @return Content-Type
     */
    private String getContentType(String filename) {
        String lowerFilename = filename.toLowerCase();

        if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerFilename.endsWith(".svg")) {
            return "image/svg+xml";
        } else {
            // 默认返回通用图片类型
            return "image/*";
        }
    }
}
