package com.ym.agriculture.farming.farmrecord.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 生长阶段预览结果。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmingGrowthStagePreviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 生长阶段编码。
     */
    private String code;

    /**
     * 生长阶段名称。
     */
    private String name;

    /**
     * 是否可靠；成功匹配物种默认阶段时为 true。
     */
    private Boolean reliable;

    /**
     * 是否为人工选择；预览计算结果固定为 false。
     */
    private Boolean manual;

    /**
     * 兼容旧字段；当前不再按日期推算，通常为空。
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date baseDate;

    /**
     * 当前物种配置的全部生长阶段，供移动端手动选择。
     */
    private List<Map<String, Object>> stages = new ArrayList<>();

    /**
     * 未匹配到生长阶段配置的地块 ID。
     */
    private List<Long> missingFieldIds = new ArrayList<>();

    /**
     * 未匹配到生长阶段配置的地块名称。
     */
    private List<String> missingFieldNames = new ArrayList<>();
}
