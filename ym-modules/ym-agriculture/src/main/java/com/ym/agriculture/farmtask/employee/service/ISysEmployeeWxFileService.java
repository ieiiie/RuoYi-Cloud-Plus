package com.ym.agriculture.farmtask.employee.service;

import com.ym.resource.api.domain.RemoteFileBatchUploadVo;
import com.ym.resource.api.domain.RemoteFileUploadVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 人员小程序注册相关文件服务。
 */
public interface ISysEmployeeWxFileService {

    /** 注册场景单次批量上传最多文件数。 */
    int MAX_REGISTER_PHOTOS = 30;

    /**
     * 注册场景上传人员照片（免登录）：校验 openid、6 位角色邀请码后写入 OSS。
     *
     * @param openid     微信 openid，非空
     * @param inviteCode 6 位角色邀请码，非空
     * @param file       图片文件，非空
     * @return 上传结果（url 用于 register 的 photo 字段）
     */
    RemoteFileUploadVo uploadRegisterPhoto(String openid, String inviteCode, MultipartFile file);

    /**
     * 注册场景批量上传人员照片（免登录）：邀请码与租户校验只做一次，逐张上传，部分失败不影响其它文件。
     *
     * @param openid     微信 openid，非空
     * @param inviteCode 6 位角色邀请码，非空
     * @param files      图片文件数组，非空，长度 1~{@link #MAX_REGISTER_PHOTOS}
     * @return 成功列表与失败列表
     */
    RemoteFileBatchUploadVo uploadRegisterPhotos(String openid, String inviteCode, MultipartFile[] files);

}
