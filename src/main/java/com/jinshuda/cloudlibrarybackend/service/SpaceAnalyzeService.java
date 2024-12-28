package com.jinshuda.cloudlibrarybackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinshuda.cloudlibrarybackend.entity.space.dto.SpaceAddDTO;
import com.jinshuda.cloudlibrarybackend.entity.space.dto.SpaceQueryDTO;
import com.jinshuda.cloudlibrarybackend.entity.space.dto.analyze.*;
import com.jinshuda.cloudlibrarybackend.entity.space.po.Space;
import com.jinshuda.cloudlibrarybackend.entity.space.vo.SpaceVO;
import com.jinshuda.cloudlibrarybackend.entity.space.vo.analyze.*;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 晋树达
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2024-12-23 21:13:07
*/
public interface SpaceAnalyzeService extends IService<Space> {
    SpaceUsageAnalyzeVO getSpaceUsageAnalyze(SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO, User loginUser);

    List<SpaceCategoryAnalyzeVO> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO, User loginUser);

    List<SpaceTagAnalyzeVO> getSpaceTagAnalyze(SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, User loginUser);

    List<SpaceSizeAnalyzeVO> getSpaceSizeAnalyze(SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, User loginUser);

    List<SpaceUserAnalyzeVO> getSpaceUserAnalyze(SpaceUserAnalyzeDTO spaceUserAnalyzeDTO, User loginUser);

    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeDTO spaceRankAnalyzeDTO, User loginUser);
}
