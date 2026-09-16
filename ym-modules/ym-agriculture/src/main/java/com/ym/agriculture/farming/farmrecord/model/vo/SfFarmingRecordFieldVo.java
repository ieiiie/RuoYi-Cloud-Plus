package com.ym.agriculture.farming.farmrecord.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 农事记录地块明细快照出参。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmingRecordFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地块 ID。
     */
    private Long fieldId;

    /**
     * 地块编号快照。
     */
    private String fieldCodeSnapshot;

    /**
     * 地块名称快照。
     */
    private String fieldNameSnapshot;

    /**
     * 种植日期快照，格式：yyyy-MM-dd。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date sowingDateSnapshot;

    /**
     * 记录内排序，越小越靠前。
     */
    private Integer sortOrder;

    /**
     * 种植批次 ID；仅管理端农事记录详情读时按地块解析填充，移动端及其他接口为空。
     */
    private Long plantingBatchId;

    /**
     * 品种 ID；仅管理端农事记录详情读时填充。
     */
    private Long varietyId;

    /**
     * 品种名称；仅管理端农事记录详情读时填充。
     */
    private String varietyName;

    /**
     * 作物物种 ID；仅管理端农事记录详情读时填充。
     */
    private Long speciesId;

    /**
     * 作物物种名称（业务上即「是什么作物」）；仅管理端农事记录详情读时填充。
     */
    private String speciesName;

    /**
     * 作物物种地图图标 URL；仅管理端农事记录详情读时填充。
     */
    private String speciesImageUrl;
}
