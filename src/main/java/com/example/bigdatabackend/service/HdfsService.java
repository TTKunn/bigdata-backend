package com.example.bigdatabackend.service;

import com.example.bigdatabackend.dto.CreateProductImageRequest;
import com.example.bigdatabackend.model.ProductImage;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * HDFS服务类 - 处理图片上传和管理
 */
@Service
public class HdfsService {

    private static final Logger logger = LoggerFactory.getLogger(HdfsService.class);

    @Autowired
    private FileSystem hdfsFileSystem;

    /**
     * 上传商品图片到HDFS
     *
     * @param request 图片上传请求
     * @param productId 商品ID
     * @return ProductImage对象
     */
    public ProductImage uploadProductImage(CreateProductImageRequest request, String productId) throws IOException {
        if (request == null || request.getFile() == null) {
            throw new IllegalArgumentException("图片文件不能为空");
        }

        // 解析图片数据
        byte[] imageData = parseImageData(request.getFile());

        // 生成HDFS路径
        String hdfsPath = generateHdfsPath(productId, request.getType(), request.getFilename());

        // 上传到HDFS
        uploadToHdfs(hdfsPath, imageData);

        // 创建ProductImage对象
        ProductImage productImage = new ProductImage();
        productImage.setId(hdfsPath);
        productImage.setType(request.getType());
        productImage.setFilename(request.getFilename());
        productImage.setSize(imageData.length);
        // 使用String格式存储时间，与HBase中的格式保持一致
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        productImage.setUploadTime(LocalDateTime.now().format(formatter));

        logger.info("Successfully uploaded image to HDFS: {}", hdfsPath);
        return productImage;
    }

    /**
     * 解析图片数据（支持Base64和直接二进制）
     */
    private byte[] parseImageData(String fileData) {
        // 检查是否为Base64格式
        if (fileData.startsWith("data:image/")) {
            // 处理Base64格式：data:image/jpeg;base64,/9j/4AAQ...
            int commaIndex = fileData.indexOf(",");
            if (commaIndex > 0) {
                String base64Data = fileData.substring(commaIndex + 1);
                return Base64.getDecoder().decode(base64Data);
            }
        }

        // 尝试直接作为Base64解码
        try {
            return Base64.getDecoder().decode(fileData);
        } catch (IllegalArgumentException e) {
            // 如果不是Base64，假设是二进制数据的字符串表示（这种情况较少见）
            logger.warn("Failed to decode as Base64, treating as string data");
            return fileData.getBytes();
        }
    }

    /**
     * 生成HDFS存储路径
     * 格式：/product_images/{category_id}/{product_id}/{timestamp}_{type}_{filename}
     */
    private String generateHdfsPath(String productId, String type, String filename) {
        // 从productId中提取category_id (前4位)
        String categoryId = productId.substring(0, 4);

        // 生成时间戳
        long timestamp = System.currentTimeMillis();

        // 构建路径
        String fileExtension = getFileExtension(filename);
        String newFilename = timestamp + "_" + type + "." + fileExtension;

        return String.format("/product_images/%s/%s/%s",
                categoryId, productId, newFilename);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "jpg"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 上传数据到HDFS
     */
    private void uploadToHdfs(String hdfsPath, byte[] data) throws IOException {
        Path path = new Path(hdfsPath);

        // 确保父目录存在
        Path parentDir = path.getParent();
        if (!hdfsFileSystem.exists(parentDir)) {
            hdfsFileSystem.mkdirs(parentDir);
            logger.info("Created HDFS directory: {}", parentDir);
        }

        // 上传文件
        try (FSDataOutputStream output = hdfsFileSystem.create(path, true)) {
            output.write(data);
            logger.info("Uploaded {} bytes to HDFS path: {}", data.length, hdfsPath);
        }
    }

    /**
     * 删除HDFS文件
     */
    public boolean deleteFile(String hdfsPath) {
        try {
            Path path = new Path(hdfsPath);
            boolean result = hdfsFileSystem.delete(path, false);
            if (result) {
                logger.info("Successfully deleted HDFS file: {}", hdfsPath);
            } else {
                logger.warn("Failed to delete HDFS file (file not found): {}", hdfsPath);
            }
            return result;
        } catch (IOException e) {
            logger.error("Failed to delete HDFS file: {}", hdfsPath, e);
            return false;
        }
    }

    /**
     * 从HDFS读取图片数据
     *
     * @param filename 图片文件名（如：kongtiao.png）
     * @return 图片字节数组
     * @throws IOException 读取失败时抛出异常
     */
    public byte[] readImage(String filename) throws IOException {
        // 构建HDFS完整路径
        String hdfsPath = "/product_images/" + filename;
        Path path = new Path(hdfsPath);

        logger.info("Reading image from HDFS: {}", hdfsPath);

        // 检查文件是否存在
        if (!hdfsFileSystem.exists(path)) {
            logger.warn("Image file not found in HDFS: {}", hdfsPath);
            return null;
        }

        // 读取文件内容
        try (org.apache.hadoop.fs.FSDataInputStream inputStream = hdfsFileSystem.open(path)) {
            // 获取文件大小
            long fileSize = hdfsFileSystem.getFileStatus(path).getLen();

            // 读取文件内容到字节数组
            byte[] imageData = new byte[(int) fileSize];
            inputStream.readFully(imageData);

            logger.info("Successfully read image from HDFS: {}, size: {} bytes", hdfsPath, fileSize);
            return imageData;
        } catch (IOException e) {
            logger.error("Failed to read image from HDFS: {}", hdfsPath, e);
            throw e;
        }
    }

    /**
     * 检查HDFS连接是否正常
     */
    public boolean checkConnection() {
        try {
            // 尝试列出根目录来检查连接
            hdfsFileSystem.listStatus(new org.apache.hadoop.fs.Path("/"));
            return true;
        } catch (Exception e) {
            logger.error("HDFS connection check failed", e);
            return false;
        }
    }
}
