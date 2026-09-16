package com.ym.agriculture.farming.algback.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.Date;

/**
 * 算法中台客户号与租户绑定，对应 {@code sf_ai_customer_binding}。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_ai_customer_binding")
public class SfAlgBackCustomerBinding extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";

    @TableId(value = "binding_id", type = IdType.ASSIGN_ID)
    private Long bindingId;

    /** 中台客户号；新建租户时为空，配置后用于回调路由与建任务。 */
    private String customerNo;

    /** {@link #STATUS_PENDING} / {@link #STATUS_ACTIVE} */
    private String status;

    @TableLogic
    private String delFlag;

    private Long createDept;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private String remark;
}
