package com.ym.agriculture.farmtask.employee.service.impl;

import cn.hutool.core.io.FileUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.core.utils.file.MimeTypeUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.employee.model.entity.SysInviteCode;
import com.ym.agriculture.farmtask.employee.service.ISysEmployeeWxFileService;
import com.ym.agriculture.farmtask.employee.service.ISysInviteCodeService;
import com.ym.agriculture.shared.support.AgricultureRemoteFileAccessor;
import com.ym.resource.api.domain.RemoteFileBatchUploadVo;
import com.ym.resource.api.domain.RemoteFileUploadFailVo;
import com.ym.resource.api.domain.RemoteFileUploadVo;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 人员小程序注册文件服务实现。
 */
@RequiredArgsConstructor
@Service
public class SysEmployeeWxFileServiceImpl implements ISysEmployeeWxFileService {

    /** 注册照片最大 5MB。 */
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private final ISysInviteCodeService inviteCodeService;
    private final AgricultureRemoteFileAccessor remoteFiles;

    @Override
    public RemoteFileUploadVo uploadRegisterPhoto(String openid, String inviteCode, MultipartFile file) {
        validateOpenidAndInviteCode(openid, inviteCode);
        validatePhotoFile(file);
        String tenantId = resolveTenantId(openid, inviteCode);
        return uploadToOss(tenantId, file);
    }

    @Override
    public RemoteFileBatchUploadVo uploadRegisterPhotos(String openid, String inviteCode, MultipartFile[] files) {
        validateOpenidAndInviteCode(openid, inviteCode);
        if (files == null || files.length == 0) {
            throw new ServiceException("上传文件不能为空");
        }
        if (files.length > MAX_REGISTER_PHOTOS) {
            throw new ServiceException("最多上传 " + MAX_REGISTER_PHOTOS + " 张");
        }
        String tenantId = resolveTenantId(openid, inviteCode);

        RemoteFileBatchUploadVo result = new RemoteFileBatchUploadVo();
        for (MultipartFile file : files) {
            String fileName = resolveFileName(file);
            try {
                validatePhotoFile(file);
                result.getSuccessList().add(uploadToOss(tenantId, file));
            } catch (ServiceException e) {
                result.getFailList().add(buildFailItem(fileName, e.getMessage()));
            }
        }
        return result;
    }

    private void validateOpenidAndInviteCode(String openid, String inviteCode) {
        if (StringUtils.isBlank(openid)) {
            throw new ServiceException("微信 openid 不能为空");
        }
        if (StringUtils.isBlank(inviteCode)) {
            throw new ServiceException("邀请码不能为空");
        }
    }

    private void validatePhotoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new ServiceException("照片大小不能超过 5MB");
        }
        String extension = FileUtil.extName(file.getOriginalFilename());
        if (!StringUtils.equalsAnyIgnoreCase(extension, MimeTypeUtils.IMAGE_EXTENSION)) {
            throw new ServiceException("文件格式不正确，请上传图片格式（bmp/gif/jpg/jpeg/png）");
        }
    }

    private String resolveTenantId(String openid, String inviteCode) {
        SysInviteCode code = TenantHelper.ignore(() -> inviteCodeService.requireValidRoleInviteCode(inviteCode, openid));
        String tenantId = code.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("邀请码未关联租户，无法上传");
        }
        return tenantId;
    }

    private RemoteFileUploadVo uploadToOss(String tenantId, MultipartFile file) {
        RemoteFile oss = remoteFiles.upload(tenantId, file, "employee-register-photo:" + inviteCodeKey(tenantId));
        RemoteFileUploadVo uploadVo = new RemoteFileUploadVo();
        uploadVo.setUrl(oss.getUrl());
        uploadVo.setFileName(oss.getOriginalName());
        uploadVo.setOssId(oss.getOssId().toString());
        return uploadVo;
    }

    private static String inviteCodeKey(String tenantId) {
        return StringUtils.blankToDefault(tenantId, "unknown");
    }

    private static String resolveFileName(MultipartFile file) {
        if (file == null) {
            return "";
        }
        return StringUtils.blankToDefault(file.getOriginalFilename(), "");
    }

    private static RemoteFileUploadFailVo buildFailItem(String fileName, String message) {
        RemoteFileUploadFailVo item = new RemoteFileUploadFailVo();
        item.setFileName(fileName);
        item.setMessage(message);
        return item;
    }
}
