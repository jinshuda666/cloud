package com.jinshuda.cloudlibrarybackend.entity.file.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByColorDTO implements Serializable {
  
    /**
     * 图片主色调  
     */
    private String picColor;
  
    /**
     * 空间 id  
     */
    private Long spaceId;
  
    private static final long serialVersionUID = 1L;
}
