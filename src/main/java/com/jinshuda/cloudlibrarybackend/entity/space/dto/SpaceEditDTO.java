package com.jinshuda.cloudlibrarybackend.entity.space.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceEditDTO implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}
