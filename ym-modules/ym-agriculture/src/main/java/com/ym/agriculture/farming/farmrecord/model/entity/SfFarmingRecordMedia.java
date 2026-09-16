package com.ym.agriculture.farming.farmrecord.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 农事记录媒体，表 {@code sf_farming_record_media}。
 *
 * @author ym-cloud
 */
@Data
@TableName("sf_farming_record_media")
public class SfFarmingRecordMedia implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 媒体主键 */
    @TableId("media_id")
    private Long mediaId;

    /** 租户编号（与上级记录一致） */
    private String tenantId;

    /** 父记录 {@code record_id} */
    private Long recordId;

    /** IMAGE / VIDEO / AUDIO */
    private String kind;

    /** 附件访问 URL（或协议内 OSS 前缀） */
    private String url;

    /** 同记录内序号 */
    private Integer seq;

    /** 纬度 WGS84（可空） */
    private BigDecimal lat;

    /** 经度 WGS84（可空） */
    private BigDecimal lng;

    /** 配文/标题文本 */
    private String caption;

    /** 客户端上报的拍摄时刻 */
    private Date capturedAt;

    /** 创建人 ID */
    private Long createBy;

    /** 创建时间 */
    private Date createTime;
}
