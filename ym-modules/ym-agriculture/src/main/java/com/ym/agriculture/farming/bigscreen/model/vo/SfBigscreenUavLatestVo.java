package com.ym.agriculture.farming.bigscreen.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 大屏最近无人机航拍任务。
 */
@Data
public class SfBigscreenUavLatestVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 飞行任务主键 */
    private Long taskId;

    /** 任务名称 */
    private String taskName;

    /** 地块名称 */
    private String fieldName;

    /** 影像数量 */
    private Integer mediaCount;

    /** 任务完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completedAt;

    /** 非连续抽取的 3 张缩略图 */
    private List<SfBigscreenUavPhotoVo> photos = new ArrayList<>();
}
