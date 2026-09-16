package com.ym.agriculture.farmtask.employee.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 携带邀请码参数的小程序码返回结果。
 */
@Data
public class MiniProgramCodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邀请码ID。
     */
    private Long codeId;

    /**
     * 租户ID。
     */
    private String tenantId;

    /**
     * 邀请码。
     */
    private String inviteCode;

    /**
     * 小程序入口页面路径。
     */
    private String page;

    /**
     * 小程序码 scene 参数，例如 {@code i:483729,t:000000}（与微信扫码落地一致，≤32 字符）。
     */
    private String scene;

    /**
     * 小程序环境版本：release/trial/develop。
     */
    private String envVersion;

    /**
     * 小程序码图片 Base64，不包含 data:image 前缀。
     */
    private String imageBase64;
}
