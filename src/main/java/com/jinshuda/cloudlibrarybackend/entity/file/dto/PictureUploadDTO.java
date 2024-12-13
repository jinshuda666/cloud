package com.jinshuda.cloudlibrarybackend.entity.file.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadDTO implements Serializable {
  
    /**  
     * 图片 id（用于修改）  
     */  
    private Long id;  
  
    private static final long serialVersionUID = 1L;  
}
