package com.ym.resource.api.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息
 *
 * @author ruoyi
 */
@Data
public class RemoteFile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * oss主键
     */
    private Long ossId;

    /**
     * 文件名称
     */
    private String name;

    private String fileName;

    /**
     * 文件地址
     */
    private String url;

    /**
     * 原名
     */
    private String originalName;

    /**
     * 文件后缀名
     */
    private String fileSuffix;

    /**
     * 扩展字段
     */
    private String ext1;

    private LocalDateTime createTime;

    private Long createBy;

    private String createByName;

    private String service;

}
