package com.jinshuda.cloudlibrarybackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinshuda.cloudlibrarybackend.entity.file.dto.PictureQueryDTO;
import com.jinshuda.cloudlibrarybackend.entity.file.dto.PictureReviewDTO;
import com.jinshuda.cloudlibrarybackend.entity.file.dto.PictureUploadByBatchDTO;
import com.jinshuda.cloudlibrarybackend.entity.file.dto.PictureUploadDTO;
import com.jinshuda.cloudlibrarybackend.entity.file.po.Picture;
import com.jinshuda.cloudlibrarybackend.entity.file.vo.PictureVO;
import com.jinshuda.cloudlibrarybackend.entity.file.vo.UploadPictureVO;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import com.jinshuda.cloudlibrarybackend.entity.user.vo.UserVO;
import com.jinshuda.cloudlibrarybackend.enums.PictureReviewStatusEnum;
import com.jinshuda.cloudlibrarybackend.exception.BusinessException;
import com.jinshuda.cloudlibrarybackend.exception.ErrorCode;
import com.jinshuda.cloudlibrarybackend.exception.ThrowUtils;
import com.jinshuda.cloudlibrarybackend.manager.CosManager;
import com.jinshuda.cloudlibrarybackend.manager.FileManager;
import com.jinshuda.cloudlibrarybackend.manager.upload.FilePictureUpload;
import com.jinshuda.cloudlibrarybackend.manager.upload.UrlPictureUpload;
import com.jinshuda.cloudlibrarybackend.manager.upload.PictureUploadTemplate;
import com.jinshuda.cloudlibrarybackend.mapper.PictureMapper;
import com.jinshuda.cloudlibrarybackend.service.PictureService;
import com.jinshuda.cloudlibrarybackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 26641
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2024-12-12 17:57:15
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService {
    @Resource
    private FileManager fileManager;
    @Resource
    private UserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadDTO pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑图片
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 上传图片，得到图片信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureVO uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        boolean result = this.saveOrUpdate(picture);
        // 可自行实现，如果是更新，可以清理图片资源
        // this.clearPictureFile(oldPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
        return PictureVO.objToVo(picture);
    }

    // @Override
    // public PictureVO uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, User loginUser) {
    //     if (inputSource == null) {
    //         throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
    //     }
    //     // 校验参数
    //     ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
    //     // 判断是新增还是删除图片
    //     Long pictureId = null;
    //     if (pictureUploadDTO != null) {
    //         pictureId = pictureUploadDTO.getId();
    //     }
    //     // 如果是更新图片，需要校验图片是不是存在
    //     if (pictureId != null) {
    //         Picture oldPicture = this.getById(pictureId);
    //         ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
    //         // 仅本人和管理员进行修改
    //         if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
    //             throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    //         }
    //     }
    //     // 上传图片，得到信息
    //     // 按照用户id划分目录
    //     String uploadPathPrefix = String.format("public/%s", loginUser.getId());
    //     PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
    //     if (inputSource instanceof String) {
    //         pictureUploadTemplate = urlPictureUpload;
    //     }
    //     UploadPictureVO uploadPictureVO = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
    //     Picture picture = new Picture();
    //     if (pictureId != null) {
    //         // 如果是更新，补充信息
    //         picture.setId(pictureId);
    //         picture.setEditTime(new Date());
    //     }
    //     picture.setUrl(uploadPictureVO.getUrl());
    //     // 图片名称
    //     String picName = uploadPictureVO.getPicName();
    //     if (pictureUploadDTO != null && StrUtil.isNotBlank(pictureUploadDTO.getPicName())) {
    //         picName = pictureUploadDTO.getPicName();
    //     }
    //     picture.setName(picName);
    //     picture.setPicSize(uploadPictureVO.getPicSize());
    //     picture.setPicWidth(uploadPictureVO.getPicWidth());
    //     picture.setPicHeight(uploadPictureVO.getPicHeight());
    //     picture.setPicScale(uploadPictureVO.getPicScale());
    //     picture.setPicFormat(uploadPictureVO.getPicFormat());
    //     picture.setUserId(loginUser.getId());
    //     picture.setThumbnailUrl(uploadPictureVO.getThumbnailUrl());
    //     // 补充审核参数
    //     this.fileReviewParams(picture, loginUser);
    //     if (pictureId != null) {
    //         picture.setEditTime(new Date());
    //         picture.setId(pictureId);
    //     }
    //     if (pictureId == null) {
    //         picture.setCreateTime(new Date());
    //     }
    //     picture.setUpdateTime(new Date());
    //     picture.setIsDelete(0);
    //     boolean result = this.saveOrUpdate(picture);
    //     // 删除旧图片资源
    //     // this.clearPictureFile(old);
    //     ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
    //     return PictureVO.objToVo(picture);
    // }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryDTO == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryDTO.getId();
        String name = pictureQueryDTO.getName();
        String introduction = pictureQueryDTO.getIntroduction();
        String category = pictureQueryDTO.getCategory();
        List<String> tags = pictureQueryDTO.getTags();
        Long picSize = pictureQueryDTO.getPicSize();
        Integer picWidth = pictureQueryDTO.getPicWidth();
        Integer picHeight = pictureQueryDTO.getPicHeight();
        Double picScale = pictureQueryDTO.getPicScale();
        String picFormat = pictureQueryDTO.getPicFormat();
        String searchText = pictureQueryDTO.getSearchText();
        Long userId = pictureQueryDTO.getUserId();
        String sortField = pictureQueryDTO.getSortField();
        String sortOrder = pictureQueryDTO.getSortOrder();
        Integer reviewStatus = pictureQueryDTO.getReviewStatus();
        Long reviewerId = pictureQueryDTO.getReviewerId();
        String reviewMessage = pictureQueryDTO.getReviewMessage();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表转化为封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void doPictureReview(PictureReviewDTO pictureReviewDTO, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(ObjUtil.isNull(pictureReviewDTO), ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewDTO.getId();
        Integer reviewStatus = pictureReviewDTO.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewDTO.getReviewMessage();
        if (id == null || reviewStatusEnum == null || reviewStatusEnum.REVIEWING.equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断图片是否存在
        Picture oldPicture = getById(pictureReviewDTO.getId());
        ThrowUtils.throwIf(ObjUtil.isNull(oldPicture), ErrorCode.NOT_FOUND_ERROR);
        // 校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 数据库操作
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewDTO, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean b = this.updateById(updatePicture);
        if (!b) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fileReviewParams(Picture picture, User loginUser) {
        // 如果是管理员自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO, User loginUser) {
        String searchText = pictureUploadByBatchDTO.getSearchText();
        String namePrefix = pictureUploadByBatchDTO.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 格式化数量
        Integer count = pictureUploadByBatchDTO.getCount();
        if (count > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多上传30张图片");
        }
        // 要抓取的图片地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("图片抓取失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取网页失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjectUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String imgUrl = imgElement.attr("src");
            if (StrUtil.isBlank(imgUrl)) {
                log.info("当前链接为空，已跳过：{}", imgUrl);
                continue;
            }
            int questionMarkIndex = imgUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                imgUrl = imgUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadDTO pictureUploadDTO = new PictureUploadDTO();
            pictureUploadDTO.setFileUrl(imgUrl);
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadDTO.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(imgUrl, pictureUploadDTO, loginUser);
                log.info("图片上传成功，数据库中图片id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("上传文件失败，e = {}", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断改图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }


    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，无论是编辑还是创建默认都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }
}




