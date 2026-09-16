package com.ym.agriculture.farming.farmrecord.model.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 农事附件（图片/音视频）一行；写入草稿或修改草稿时与子表一致。
 */
@Data
public class SfFarmingRecordMediaItemBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** {@code IMAGE} / {@code VIDEO} / {@code AUDIO}。 */
    @NotBlank(message = "媒体类型 kind 不能为空")
    private String kind;

    /** 可访问 URL 或可解析 OSS key。 */
    @NotBlank(message = "媒体 URL 不能为空")
    private String url;

    /** 同一记录内展示顺序；可空等价于按提交顺序后置。 */
    private Integer seq;

    /** 拍摄位置纬度（WGS84）；可空表示不更新 */
    private BigDecimal lat;

    /** 拍摄位置经度；可空 */
    private BigDecimal lng;

    /** 配文；可空 */
    private String caption;

    /** 拍摄时间戳；可空 */
    private Date capturedAt;
}
