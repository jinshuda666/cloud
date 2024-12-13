package com.jinshuda.cloudlibrarybackend.manager;

import com.jinshuda.cloudlibrarybackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

@Component
public class CosManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private COSClient cosClient;

    /**
     * 上传文件
     * @param path 唯一标识
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String path, File file) {
        return cosClient.putObject(cosClientConfig.getBucket(), path, file);
    }

    /**
     * 下载文件
     * @param path 文件唯一标识
     * @return
     */
    public COSObject getObject(String path) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), path);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片
     * @param path 文件唯一标识
     * @param file 图片文件
     * @return
     */
    public PutObjectResult putPictureObject(String path, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), path, file);
        // 对图片进行处理
        PicOperations picOperations = new PicOperations();
        // 1 标识返回原图信息
        picOperations.setIsPicInfo(1);
        // 构造参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }
}
