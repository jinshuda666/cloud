package com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间用户请求
 */
@Data
public class SpaceUserAddDTO implements Serializable {
  
    /**
     * 空间 ID  
     */
    private Long spaceId;
  
    /**
     * 用户 ID  
     */
    private Long userId;
  
    /**
     * 空间角色：viewer/editor/admin  
     */
    private String spaceRole;
  
    private static final long serialVersionUID = 1L;
}
