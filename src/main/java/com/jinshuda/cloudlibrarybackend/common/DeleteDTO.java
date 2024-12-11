package com.jinshuda.cloudlibrarybackend.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteDTO implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
