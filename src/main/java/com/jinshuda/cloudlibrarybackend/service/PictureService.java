package com.jinshuda.cloudlibrarybackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinshuda.cloudlibrarybackend.entity.file.dto.PictureQueryDTO;
import com.jinshuda.cloudlibrarybackend.entity.file.dto.PictureReviewDTO;
import com.jinshuda.cloudlibrarybackend.entity.file.dto.PictureUploadByBatchDTO;
import com.jinshuda.cloudlibrarybackend.entity.file.dto.PictureUploadDTO;
import com.jinshuda.cloudlibrarybackend.entity.file.po.Picture;
import com.jinshuda.cloudlibrarybackend.entity.file.vo.PictureVO;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author 晋树达
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2024-12-12 17:57:15
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadDTO
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadDTO pictureUploadDTO,
                            User loginUser);

    /**
     * 构建查询参数
     *
     * @param pictureQueryDTO
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO);

    /**
     * 获取单个图片
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 修改图片校验
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 分页获取图片信息
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片审核
     *
     * @param pictureReviewDTO
     * @param loginUser
     */
    void doPictureReview(PictureReviewDTO pictureReviewDTO, User loginUser);

    void fileReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchDTO
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO,
            User loginUser);

    /**
     * 清理图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);
}
