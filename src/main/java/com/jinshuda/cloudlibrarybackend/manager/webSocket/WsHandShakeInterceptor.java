package com.jinshuda.cloudlibrarybackend.manager.webSocket;

import cn.hutool.core.util.StrUtil;
import com.jinshuda.cloudlibrarybackend.entity.file.po.Picture;
import com.jinshuda.cloudlibrarybackend.entity.space.po.Space;
import com.jinshuda.cloudlibrarybackend.entity.user.po.User;
import com.jinshuda.cloudlibrarybackend.enums.SpaceTypeEnum;
import com.jinshuda.cloudlibrarybackend.manager.auth.SpaceUserAuthManager;
import com.jinshuda.cloudlibrarybackend.manager.auth.model.SpaceUserPermissionConstant;
import com.jinshuda.cloudlibrarybackend.manager.webSocket.enums.PictureEditActionEnum;
import com.jinshuda.cloudlibrarybackend.service.PictureService;
import com.jinshuda.cloudlibrarybackend.service.SpaceService;
import com.jinshuda.cloudlibrarybackend.service.SpaceUserService;
import com.jinshuda.cloudlibrarybackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 在握手之前判断用户是否有编辑图片的权限
 */
@Component
@Slf4j
public class WsHandShakeInterceptor implements HandshakeInterceptor {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 在握手之前校验用户是否有编辑该图片的权限
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String pictureId = servletRequest.getParameter("pictureId");
            // 判断pictureId是不是空，如果是空的话，拒绝握手
            if (StrUtil.isEmpty(pictureId)) {
                log.error("图片参数为空，拒绝握手");
                return false;
            }
            // 获取当前登录用户
            User loginUser = userService.getLoginUser(servletRequest);
            if (loginUser == null) {
                log.error("当前用户未登录，拒绝握手");
                return false;
            }
            // 根据图片id获取图片
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            // 获取图片所在的空间
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    // 说明空间就不存在
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                // 如果不是团队空间的话，也要拒绝握手
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("当前空间不是团队空间，拒绝握手");
                    return false;
                }
            }
            // 判断用户是否有编辑该图片的权限
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("当前用户没有该图片的编辑权限，拒绝握手");
                return false;
            }
            attributes.put("pictureId", Long.valueOf(pictureId));
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
