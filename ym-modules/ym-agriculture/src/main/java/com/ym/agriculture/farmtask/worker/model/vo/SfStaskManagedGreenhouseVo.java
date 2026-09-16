package com.ym.agriculture.farmtask.worker.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * stask 小程序组长管理大棚视图。
 */
@Data
public class SfStaskManagedGreenhouseVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 大棚ID，对应 sf_field.field_id。
     */
    private Long greenhouseId;

    /**
     * 大棚名称。
     */
    private String greenhouseName;

    /**
     * 大棚编码。
     */
    private String greenhouseCode;
}
