package com.jinshuda.cloudlibrarybackend.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinshuda.cloudlibrarybackend.entity.space.po.Space;
import com.jinshuda.cloudlibrarybackend.entity.space.vo.SpaceVO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto.SpaceUserAddDTO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto.SpaceUserQueryDTO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.po.SpaceUser;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.vo.SpaceUserVO;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import com.jinshuda.cloudlibrarybackend.entity.user.vo.UserVO;
import com.jinshuda.cloudlibrarybackend.enums.SpaceRoleEnum;
import com.jinshuda.cloudlibrarybackend.exception.BusinessException;
import com.jinshuda.cloudlibrarybackend.exception.ErrorCode;
import com.jinshuda.cloudlibrarybackend.exception.ThrowUtils;
import com.jinshuda.cloudlibrarybackend.service.SpaceService;
import com.jinshuda.cloudlibrarybackend.service.SpaceUserService;
import com.jinshuda.cloudlibrarybackend.mapper.SpaceUserMapper;
import com.jinshuda.cloudlibrarybackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 26641
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-01-03 20:36:03
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;

    @Override
    public long addSpaceUser(SpaceUserAddDTO spaceUserAddDTO) {
        // 校验参数
        if (spaceUserAddDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数能为空");
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddDTO, spaceUser);
        validSpaceUser(spaceUser, true);
        boolean save = this.save(spaceUser);
        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间角色失败");
        }
        Long spaceUserId = spaceUser.getId();
        return spaceUserId;
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDTO spaceUserQueryDTO) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryDTO == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryDTO.getId();
        Long spaceId = spaceUserQueryDTO.getSpaceId();
        Long userId = spaceUserQueryDTO.getUserId();
        String spaceRole = spaceUserQueryDTO.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        if (spaceUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 查询关联的用户信息
        Long userId = spaceUserVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        // 查询关联的空间信息
        Long spaceId = spaceUserVO.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollectionUtil.isEmpty(spaceUserList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        Set<Long> userIdSet = spaceUserVOList.stream().map(SpaceUserVO::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserVOList.stream().map(SpaceUserVO::getSpaceId).collect(Collectors.toSet());
        Map<Long, List<User>> userMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceMap = spaceService.listByIds(spaceIdSet).stream().collect(Collectors.groupingBy(Space::getId));
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            User user = null;
            if (userMap.containsKey(userId)) {
                user = userMap.get(userId).get(0);
            }
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
            Long spaceId = spaceUserVO.getSpaceId();
            Space space = null;
            if (spaceMap.containsKey(spaceId)) {
                space = spaceMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }
}




