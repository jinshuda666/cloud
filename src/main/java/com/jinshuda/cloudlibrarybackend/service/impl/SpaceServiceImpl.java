package com.jinshuda.cloudlibrarybackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinshuda.cloudlibrarybackend.entity.space.dto.SpaceAddDTO;
import com.jinshuda.cloudlibrarybackend.entity.space.dto.SpaceQueryDTO;
import com.jinshuda.cloudlibrarybackend.entity.space.po.Space;
import com.jinshuda.cloudlibrarybackend.entity.space.vo.SpaceVO;
import com.jinshuda.cloudlibrarybackend.entity.spaceuser.po.SpaceUser;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import com.jinshuda.cloudlibrarybackend.entity.user.vo.UserVO;
import com.jinshuda.cloudlibrarybackend.enums.SpaceLevelEnum;
import com.jinshuda.cloudlibrarybackend.enums.SpaceRoleEnum;
import com.jinshuda.cloudlibrarybackend.enums.SpaceTypeEnum;
import com.jinshuda.cloudlibrarybackend.exception.BusinessException;
import com.jinshuda.cloudlibrarybackend.exception.ErrorCode;
import com.jinshuda.cloudlibrarybackend.exception.ThrowUtils;
import com.jinshuda.cloudlibrarybackend.service.SpaceService;
import com.jinshuda.cloudlibrarybackend.mapper.SpaceMapper;
import com.jinshuda.cloudlibrarybackend.service.SpaceUserService;
import com.jinshuda.cloudlibrarybackend.service.UserService;
import jdk.jfr.Label;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 晋树达
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2024-12-23 21:13:07
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private SpaceUserService spaceUserService;

    /**
     * 校验空间参数
     *
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            // 如果是创建空间的话，就必须指定是私人空间还是团队空间
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }
        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 如果是修改数据的话
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您要创建的空间类型不存在");
        }
    }

    /**
     * 根据空间级别填充空间最大容量
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public long addSpace(SpaceAddDTO spaceAddDTO, User loginUser) {
        // 配置空间名称
        if (StrUtil.isEmpty(spaceAddDTO.getSpaceName())) {
            spaceAddDTO.setSpaceName("空间默认名称");
        }
        if (spaceAddDTO.getSpaceLevel() == null) {
            spaceAddDTO.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (spaceAddDTO.getSpaceType() == null) {
            spaceAddDTO.setSpaceType(SpaceLevelEnum.COMMON.getValue());
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddDTO, space);
        // 根据空间级别填充参数
        this.fillSpaceBySpaceLevel(space);
        // 校验参数
        this.validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 检测用户创建的空间是否合法
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您无权限创建指定级别的空间");
        }
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long spaceId = transactionTemplate.execute(status -> {
                // 从数据库中查看是否有该用户创建的空间
                LambdaQueryWrapper<Space> spaceLambdaQueryWrapper = new LambdaQueryWrapper<>();
                spaceLambdaQueryWrapper.eq(Space::getUserId, userId);
                spaceLambdaQueryWrapper.eq(Space::getSpaceType, spaceAddDTO.getSpaceType());
                boolean exists = this.exists(spaceLambdaQueryWrapper);
                if (exists) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "每个用户每种类型的空间只能创建一个");
                }
                // 像数据库中插入记录
                boolean res = this.save(space);
                if (!res) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败，请稍后重试");
                }
                // 向sapce_user表中插入管理员记录
                if (space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    spaceUser.setCreateTime(new Date());
                    spaceUser.setUpdateTime(new Date());
                    boolean save = spaceUserService.save(spaceUser);
                    if (!save) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存团队空间管理员记录失败");
                    }
                }
                return space.getId();
            });
            return spaceId;
        }
    }


    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO spaceQueryDTO) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryDTO == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryDTO.getId();
        Long userId = spaceQueryDTO.getUserId();
        String spaceName = spaceQueryDTO.getSpaceName();
        Integer spaceLevel = spaceQueryDTO.getSpaceLevel();
        String sortField = spaceQueryDTO.getSortField();
        String sortOrder = spaceQueryDTO.getSortOrder();
        Integer spaceType = spaceQueryDTO.getSpaceType();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        // 仅本人或管理员可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




