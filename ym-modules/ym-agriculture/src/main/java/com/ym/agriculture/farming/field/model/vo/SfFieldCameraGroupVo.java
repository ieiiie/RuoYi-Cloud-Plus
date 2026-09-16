package com.ym.agriculture.farming.field.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 地块-摄像头分组视图对象。
 * <p>
 * 用于 {@code GET /smart-farming/field/cameras}：每个地块一个对象，{@link #cameras} 内为该地块绑定的
 * WVP 摄像头列表（按 {@code iot_hfzk_user_device.device_type='WVP_CAMERA'} 过滤）。
 *
 * @author ym-cloud
 */
@Data
public class SfFieldCameraGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 地块主键 */
    private Long fieldId;

    /** 地块名称 */
    private String fieldName;

    /** 地块编码 */
    private String fieldCode;

    /** 面积（亩） */
    private BigDecimal areaMu;

    /** 中心经度 */
    private BigDecimal centerLng;

    /** 中心纬度 */
    private BigDecimal centerLat;

    /** 该地块绑定的 WVP 摄像头集合（按 {@code device_sn} 升序） */
    private List<SfFieldCameraVo> cameras;
}
