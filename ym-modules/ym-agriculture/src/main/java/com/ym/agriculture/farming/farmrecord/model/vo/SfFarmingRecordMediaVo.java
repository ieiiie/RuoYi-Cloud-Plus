package com.ym.agriculture.farming.farmrecord.model.vo;

import com.ym.agriculture.farming.farmrecord.model.constant.FarmingMediaKind;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordMedia;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 农事记录媒体出参，与 {@code sf_farming_record_media} 对齐；含拍摄位置便于地图展示。
 */
@Data
@AutoMapper(target = SfFarmingRecordMedia.class)
public class SfFarmingRecordMediaVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 媒体主键 {@code media_id} */
    private Long mediaId;

    /** {@code IMAGE} | {@code VIDEO} | {@code AUDIO}，校验见 {@linkplain FarmingMediaKind} */
    private String kind;

    /** 媒体访问 URL（OSS/CDN） */
    private String url;

    /** 同一记录内的展示序号，越小越靠前 */
    private Integer seq;

    /** 纬度（WGS84）；未上报时为 null */
    private BigDecimal lat;

    /** 经度（WGS84）；未上报时为 null */
    private BigDecimal lng;

    /** 配文/标题 */
    private String caption;

    /** 拍摄时间；未上报时为 null */
    private Date capturedAt;
}
