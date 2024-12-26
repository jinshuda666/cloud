package com.jinshuda.cloudlibrarybackend.manager.upload;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.jinshuda.cloudlibrarybackend.config.CosClientConfig;
import com.jinshuda.cloudlibrarybackend.entity.file.vo.ImageSearchVO;
import com.jinshuda.cloudlibrarybackend.entity.file.vo.UploadPictureVO;
import com.jinshuda.cloudlibrarybackend.exception.BusinessException;
import com.jinshuda.cloudlibrarybackend.exception.ErrorCode;
import com.jinshuda.cloudlibrarybackend.manager.CosManager;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;
    @Resource
    protected CosClientConfig cosClientConfig;


    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureVO uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);
        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);
            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象，封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollectionUtil.isNotEmpty(objectList)) {
                // 获取压缩之后得到的文件信息
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 如果图片大小大于20k才会有缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(originFilename, compressedCiObject, thumbnailCiObject, imageInfo);
            }

            // 5. 封装返回结果
            return buildResult(originFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装返回结果
     *
     * @param originalFilename   原始文件名
     * @param compressedCiObject 压缩后的对象
     * @param thumbnailCiObject 缩略图对象
     * @return
     */
    private UploadPictureVO buildResult(String originalFilename, CIObject compressedCiObject,
                                        CIObject thumbnailCiObject, ImageInfo imageInfo) {
        // 计算宽高
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 封装返回结果
        UploadPictureVO uploadPictureVO = new UploadPictureVO();
        // 设置压缩后的原图地址
        uploadPictureVO.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureVO.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureVO.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureVO.setPicWidth(picWidth);
        uploadPictureVO.setPicHeight(picHeight);
        uploadPictureVO.setPicScale(picScale);
        uploadPictureVO.setPicColor(imageInfo.getAve());
        uploadPictureVO.setPicFormat(compressedCiObject.getFormat());
        // 设置缩略图地址
        uploadPictureVO.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        // 返回可访问的地址
        return uploadPictureVO;
    }

    /**
     * 封装返回结果
     */
    private UploadPictureVO buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureVO uploadPictureVO = new UploadPictureVO();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureVO.setPicName(FileUtil.mainName(originFilename));
        uploadPictureVO.setPicWidth(picWidth);
        uploadPictureVO.setPicHeight(picHeight);
        uploadPictureVO.setPicScale(picScale);
        uploadPictureVO.setPicFormat(imageInfo.getFormat());
        uploadPictureVO.setPicSize(FileUtil.size(file));
        uploadPictureVO.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureVO;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}

