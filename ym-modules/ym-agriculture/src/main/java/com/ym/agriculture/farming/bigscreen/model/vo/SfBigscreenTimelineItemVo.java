package com.ym.agriculture.farming.bigscreen.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 大屏农事操作时间轴单项。
 */
@Data
public class SfBigscreenTimelineItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 农事记录主键 */
    private Long recordId;

    /** 发生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date happenedAt;

    /** 执行人 */
    private String executor;

    /** 农事项目/操作类型 */
    private String operationType;

    /** 地块名称 */
    private String fieldName;

    /** 详情摘要 */
    private String detail;

    /** 数据来源，固定 farming_record */
    private String source;
}
