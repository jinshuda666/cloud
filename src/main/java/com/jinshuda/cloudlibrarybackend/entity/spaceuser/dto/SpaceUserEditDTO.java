package com.jinshuda.cloudlibrarybackend.entity.spaceuser.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间用户请求
 */
@Data
public class SpaceUserEditDTO implements Serializable {
  
    /**
     * id  
     */
    private Long id;
  
    /**
     * 空间角色：viewer/editor/admin  
     */
    private String spaceRole;
  
    private static final long serialVersionUID = 1L;
}
