package com.jinshuda.cloudlibrarybackend.entity.file.dto;

import lombok.Data;

@Data
public class PictureUploadByBatchDTO {
  
    /**  
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

}
