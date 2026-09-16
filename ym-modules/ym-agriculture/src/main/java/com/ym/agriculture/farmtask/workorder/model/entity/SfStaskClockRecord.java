package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * stask 到岗打卡记录，表 {@code sf_stask_clock_record}。
 */
@Data
@TableName("sf_stask_clock_record")
public class SfStaskClockRecord {

    /**
     * 打卡记录主键。
     */
    @TableId("clock_id")
    private Long clockId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 工单ID。
     */
    private Long orderId;

    /**
     * 组长员工ID。
     */
    private Long leaderId;

    /**
     * 打卡类型：GPS/PHOTO。
     */
    private String clockType;

    /**
     * 经度。
     */
    private BigDecimal longitude;

    /**
     * 纬度。
     */
    private BigDecimal latitude;

    /**
     * 证明照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String proofPhotos;

    /**
     * 与租户打卡地点中心点距离，单位：米；无坐标或未配置打卡地点时为 null。
     */
    private Integer distanceMeters;

    /**
     * 打卡时间。
     */
    private Date clockTime;

    /**
     * 创建时间。
     */
    private Date createTime;
}
