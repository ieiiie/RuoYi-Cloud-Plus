package com.ym.iot.fertilizer.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 施肥机-单次施肥流水实体，对应 {@code iot_fertilizer_record}。
 *
 * <p>来源：0x13 帧 payload 内每条 12 字节定长记录
 * （uint32 时间戳 + uint16 类型 + uint16 时长分钟值 + IEEE754 float 肥量）。
 *
 * <p>租户归属：以施肥机在 {@code iot_device} 中的档案租户为准。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("iot_fertilizer_record")
public class IotFertilizerRecord extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "record_id")
    private Long recordId;

    private Long deviceId;

    /** 设备编号（冗余，便于排障）。 */
    private String deviceCode;

    /** 本次施肥时间（来自 0x13 内 uint32 秒级时间戳）。 */
    private Date recordTime;

    /** 施肥类型（uint16 取低 8 位）。 */
    private Integer fertilizationType;

    /** 本次施肥时长（分钟；历史字段名保留为 seconds）。 */
    private Integer fertilizationSeconds;

    /** 本次施肥量（kg，3 位小数）。 */
    private BigDecimal fertilizationQuantity;

    /** 本条 12 字节原始 HEX，排障用。 */
    private String rawPayloadHex;

    @TableLogic
    private String delFlag;
}
