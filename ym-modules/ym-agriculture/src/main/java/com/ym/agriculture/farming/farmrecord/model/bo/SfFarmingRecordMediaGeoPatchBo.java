package com.ym.agriculture.farming.farmrecord.model.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 草稿或已提交记录下某条媒体「拍摄位置」局部更新。
 * <p>
 * 仅对请求体中出现的非空字段执行更新（未传或为 null 的字段保持原值）。
 * 三个字段均为空时服务端拒绝。
 * </p>
 */
@Data
public class SfFarmingRecordMediaGeoPatchBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 拍摄纬度，可选 */
    private BigDecimal lat;

    /** 拍摄经度，可选 */
    private BigDecimal lng;

    /** 拍摄时间，可选 */
    private Date capturedAt;

    /** 照片说明/配文，可选 */
    private String caption;
}
