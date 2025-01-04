package com.jinshuda.cloudlibrarybackend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.jinshuda.cloudlibrarybackend.common.BaseResponse;
import com.jinshuda.cloudlibrarybackend.common.DeleteDTO;
import com.jinshuda.cloudlibrarybackend.common.ResultUtils;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto.SpaceUserAddDTO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto.SpaceUserEditDTO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto.SpaceUserQueryDTO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.po.SpaceUser;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.vo.SpaceUserVO;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import com.jinshuda.cloudlibrarybackend.exception.BusinessException;
import com.jinshuda.cloudlibrarybackend.exception.ErrorCode;
import com.jinshuda.cloudlibrarybackend.exception.ThrowUtils;
import com.jinshuda.cloudlibrarybackend.manager.auth.SpaceUserPermissionConstant;
import com.jinshuda.cloudlibrarybackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.jinshuda.cloudlibrarybackend.service.SpaceUserService;
import com.jinshuda.cloudlibrarybackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {  
  
    @Resource  
    private SpaceUserService spaceUserService;
  
    @Resource
    private UserService userService;
  
    /**  
     * 添加成员到空间  
     */  
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddDTO spaceUserAddDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddDTO == null, ErrorCode.PARAMS_ERROR);
        long id = spaceUserService.addSpaceUser(spaceUserAddDTO);
        return ResultUtils.success(id);
    }  
  
    /**  
     * 从空间移除成员  
     */  
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteDTO deleteDTO,
                                                 HttpServletRequest request) {  
        if (deleteDTO == null || deleteDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }  
        long id = deleteDTO.getId();
        // 判断是否存在  
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);  
        // 操作数据库  
        boolean result = spaceUserService.removeById(id);  
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);  
        return ResultUtils.success(true);  
    }  
  
    /**  
     * 查询某个成员在某个空间的信息  
     */  
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryDTO spaceUserQueryDTO) {
        // 参数校验  
        ThrowUtils.throwIf(spaceUserQueryDTO == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryDTO.getSpaceId();
        Long userId = spaceUserQueryDTO.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库  
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryDTO));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);  
        return ResultUtils.success(spaceUser);  
    }  
  
    /**  
     * 查询成员信息列表  
     */  
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryDTO spaceUserQueryDTO,
                                                         HttpServletRequest request) {  
        ThrowUtils.throwIf(spaceUserQueryDTO == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(  
                spaceUserService.getQueryWrapper(spaceUserQueryDTO)
        );  
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));  
    }  
  
    /**  
     * 编辑成员信息（设置权限）  
     */  
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditDTO spaceUserEditDTO,
                                               HttpServletRequest request) {  
        if (spaceUserEditDTO == null || spaceUserEditDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);  
        }  
        // 将实体类和 DTO 进行转换  
        SpaceUser spaceUser = new SpaceUser();  
        BeanUtils.copyProperties(spaceUserEditDTO, spaceUser);
        // 数据校验  
        spaceUserService.validSpaceUser(spaceUser, false);  
        // 判断是否存在  
        long id = spaceUserEditDTO.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);  
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);  
        // 操作数据库  
        boolean result = spaceUserService.updateById(spaceUser);  
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);  
        return ResultUtils.success(true);  
    }  
  
    /**  
     * 查询我加入的团队空间列表  
     */  
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryDTO spaceUserQueryDTO = new SpaceUserQueryDTO();
        spaceUserQueryDTO.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryDTO)
        );  
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));  
    }  
}
