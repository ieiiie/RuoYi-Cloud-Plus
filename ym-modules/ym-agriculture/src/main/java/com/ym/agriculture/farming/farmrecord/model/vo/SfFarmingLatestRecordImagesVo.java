package com.ym.agriculture.farming.farmrecord.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 最新一条含现场图片的已提交农事记录及其图片列表。
 */
@Data
public class SfFarmingLatestRecordImagesVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 农事记录主键；无匹配记录时为 null */
    private Long recordId;

    /**
     * 农事发生时间，格式：yyyy-MM-dd HH:mm:ss。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date happenedAt;

    /**
     * 提交时间，格式：yyyy-MM-dd HH:mm:ss。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 今日小结（农事总结），可能为空字符串 */
    private String summary;

    /** 关联地块快照 */
    private List<SfFarmingRecordFieldVo> fields = new ArrayList<>();

    /** 现场图片列表（{@code kind=IMAGE}），按记录内 {@code seq} 升序 */
    private List<SfFarmingRecordMediaVo> images = new ArrayList<>();
}
