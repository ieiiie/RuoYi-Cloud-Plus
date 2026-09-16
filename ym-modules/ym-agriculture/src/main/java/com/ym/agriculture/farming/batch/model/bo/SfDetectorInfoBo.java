package com.ym.agriculture.farming.batch.model.bo;

import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 检测仪信息查询条件。
 *
 * @author ym-cloud
 */
@Data
public class SfDetectorInfoBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 仪器种类筛选（精确匹配） */
    private String deviceCategory;

    /** 地块 ID 筛选 */
    private Long fieldId;

    /** 种植品种 ID 筛选 */
    private Long varietyId;

    /** 检测仪名称模糊查询 */
    private String deviceName;

    /** 数据日期下限（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date lastDataDateBegin;

    /** 数据日期上限（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date lastDataDateEnd;
}
