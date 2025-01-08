package com.jinshuda.cloudlibrarybackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jinshuda.cloudlibrarybackend.entity.space.po.Space;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.po.SpaceUser;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import com.jinshuda.cloudlibrarybackend.enums.SpaceRoleEnum;
import com.jinshuda.cloudlibrarybackend.enums.SpaceTypeEnum;
import com.jinshuda.cloudlibrarybackend.manager.auth.model.SpaceUserAuthConfig;
import com.jinshuda.cloudlibrarybackend.manager.auth.model.SpaceUserPermissionConstant;
import com.jinshuda.cloudlibrarybackend.manager.auth.model.SpaceUserRole;
import com.jinshuda.cloudlibrarybackend.service.SpaceUserService;
import com.jinshuda.cloudlibrarybackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    // 根据角色获取权限列表
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (spaceUserRole == null) {
            return new ArrayList<>();
        }
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }

    /**
     * 获取用户权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        List<String> adminPermissionList = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 如果space是空的话，那么就是查询公共图库
        if (space == null) {
            // 如果是管理员的话，返回所有的权限
            if (userService.isAdmin(loginUser)) {
                return adminPermissionList;
            } else {
                // 只有查看的权限
                return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
            }
        }
        // 根据空间类型获取空间是哪一类
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }


        switch (spaceTypeEnum) {
            // 如果是私有空间的话只有本人和空间管理员有全部权限，其他用户什么权限也没有
            case PRIVATE:
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return adminPermissionList;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                LambdaQueryWrapper<SpaceUser> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SpaceUser::getSpaceId, space.getId()).eq(SpaceUser::getUserId, loginUser.getId());
                SpaceUser spaceUser = spaceUserService.getOne(wrapper);
                // 如果没查到说明该用户不在当前团队空间中，也就是没有权限
                if (spaceUser == null) {
                    return new ArrayList<>();
                }
                List<String> userRole = getPermissionsByRole(spaceUser.getSpaceRole());
                return userRole;
        }
        return new ArrayList<>();
    }
}
