package com.jinshuda.cloudlibrarybackend.entity.file.po;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {
    /**
     * 标签列表
     */
    List<String> tagList;

    /**
     * 分类列表
     */
    List<String> categoryList;

    private static final long serialVersionUID = 1L;
}
