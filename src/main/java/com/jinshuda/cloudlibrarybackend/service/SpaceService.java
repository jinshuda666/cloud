package com.jinshuda.cloudlibrarybackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinshuda.cloudlibrarybackend.entity.space.dto.SpaceAddDTO;
import com.jinshuda.cloudlibrarybackend.entity.space.dto.SpaceQueryDTO;
import com.jinshuda.cloudlibrarybackend.entity.space.po.Space;
import com.jinshuda.cloudlibrarybackend.entity.space.vo.SpaceVO;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;

import javax.servlet.http.HttpServletRequest;

/**
* @author 晋树达
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2024-12-23 21:13:07
*/
public interface SpaceService extends IService<Space> {

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    long addSpace(SpaceAddDTO spaceAddRequest, User loginUser);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询对象
     *
     * @param spaceQueryDTO
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO spaceQueryDTO);

    void checkSpaceAuth(Space space, User loginUser);
}
