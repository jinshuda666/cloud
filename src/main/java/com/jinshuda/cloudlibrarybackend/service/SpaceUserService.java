package com.jinshuda.cloudlibrarybackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto.SpaceUserAddDTO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto.SpaceUserQueryDTO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.po.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.vo.SpaceUserVO;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 26641
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-01-03 20:36:03
*/
@Service
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddDTO spaceUserAddDTO);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDTO spaceUserQueryDTO);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
