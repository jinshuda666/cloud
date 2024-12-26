package com.jinshuda.cloudlibrarybackend.api;

import com.jinshuda.cloudlibrarybackend.entity.file.vo.ImageSearchVO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {  
  
    /**  
     * 搜索图片  
     *  
     * @param imageUrl  
     * @return  
     */  
    public static List<ImageSearchVO> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);  
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);  
        List<ImageSearchVO> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;  
    }
}
