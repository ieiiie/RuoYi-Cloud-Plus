package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 无人机航拍缩略图。
 */
@Data
public class SfBigscreenUavPhotoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 媒体文件 ID */
    private Long mediaId;

    /** 缩略图 URL */
    private String thumbUrl;

    /** 原图 URL */
    private String imageUrl;

    /** 展示标签 */
    private String label;
}
