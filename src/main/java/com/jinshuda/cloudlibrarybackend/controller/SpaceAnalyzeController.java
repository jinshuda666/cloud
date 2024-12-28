package com.jinshuda.cloudlibrarybackend.controller;

import com.jinshuda.cloudlibrarybackend.common.BaseResponse;
import com.jinshuda.cloudlibrarybackend.common.ResultUtils;
import com.jinshuda.cloudlibrarybackend.entity.space.dto.analyze.*;
import com.jinshuda.cloudlibrarybackend.entity.space.po.Space;
import com.jinshuda.cloudlibrarybackend.entity.space.vo.analyze.*;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import com.jinshuda.cloudlibrarybackend.exception.ErrorCode;
import com.jinshuda.cloudlibrarybackend.exception.ThrowUtils;
import com.jinshuda.cloudlibrarybackend.service.SpaceAnalyzeService;
import com.jinshuda.cloudlibrarybackend.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private UserService userService;
    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    /**
     * 获取空间的使用状态
     *
     * @param spaceUsageAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeVO> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO,
            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeVO spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    /**
     * 获取空间图片分类分析
     *
     * @param spaceCategoryAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeVO>> getSpaceCategoryAnalyze(
            @RequestBody SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO,
            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeVO> spaceCategoryAnalyze = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceCategoryAnalyze);
    }

    /**
     * 获取空间图片标签分析
     *
     * @param spaceTagAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeVO>> getSpaceTagAnalyze(
            @RequestBody SpaceTagAnalyzeDTO spaceTagAnalyzeDTO,
            HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeVO> spaceTagAnalyze = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceTagAnalyze);
    }

    /**
     * 获取空间图片大小分析
     *
     * @param spaceSizeAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeVO>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO,
                                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeVO> resultList = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeDTO, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 获取空间用户行为分析
     *
     * @param spaceUserAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeVO>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeDTO spaceUserAnalyzeDTO,
                                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeVO> resultList = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeDTO, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 获取空间使用排行分析
     *
     * @param spaceRankAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeDTO spaceRankAnalyzeDTO,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<Space> resultList = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeDTO, loginUser);
        return ResultUtils.success(resultList);
    }
}
