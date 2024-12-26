package com.jinshuda.cloudlibrarybackend.entity.file.vo;

import lombok.Data;

@Data
public class ImageSearchVO {
  
    /**  
     * 缩略图地址  
     */  
    private String thumbUrl;  
  
    /**  
     * 来源地址  
     */  
    private String fromUrl;  
}
