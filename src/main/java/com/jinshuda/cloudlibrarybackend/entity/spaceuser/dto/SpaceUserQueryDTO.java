package com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询空间用户请求
 */
@Data
public class SpaceUserQueryDTO implements Serializable {
  
    /**
     * ID
     */
    private Long id;
  
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
