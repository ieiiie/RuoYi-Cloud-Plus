package com.ym.agriculture.shared.support;

import cn.hutool.crypto.digest.DigestUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.resource.api.RemoteFileService;
import com.ym.resource.api.domain.RemoteFile;
import com.ym.resource.api.domain.RemoteFileBatchUploadVo;
import com.ym.resource.api.domain.RemoteFileUploadBo;
import com.ym.resource.api.domain.RemoteFileUploadVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

/**
 * 农业域访问平台文件服务的唯一远程边界。
 *
 * <p>文件内容与业务场景一起生成稳定幂等号，可供资源服务执行去重与重试。</p>
 */
@Component
public class AgricultureRemoteFileAccessor {

    @DubboReference
    private RemoteFileService remoteFileService;

    public RemoteFile getById(String tenantId, Long ossId) {
        if (ossId == null) {
            return null;
        }
        return inTenant(tenantId, () -> remoteFileService.getById(ossId));
    }

    public RemoteFile findByUrl(String tenantId, String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        return inTenant(tenantId, () -> remoteFileService.findByUrl(url.trim()));
    }

    public List<RemoteFile> listByIds(String tenantId, Collection<Long> ossIds) {
        if (ossIds == null || ossIds.isEmpty()) {
            return List.of();
        }
        String ids = ossIds.stream().filter(java.util.Objects::nonNull)
            .map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        if (ids.isBlank()) {
            return List.of();
        }
        List<RemoteFile> rows = inTenant(tenantId, () -> remoteFileService.selectByIds(ids));
        return rows == null ? List.of() : rows;
    }

    public RemoteFile upload(String tenantId, File file, String businessId) {
        if (file == null || !file.isFile()) {
            throw new ServiceException("上传文件不存在");
        }
        try {
            String contentType = Files.probeContentType(file.toPath());
            return uploadBytes(tenantId, Files.readAllBytes(file.toPath()), file.getName(), contentType, businessId);
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败", e);
        }
    }

    public RemoteFile upload(String tenantId, MultipartFile file, String businessId) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        try {
            return uploadBytes(tenantId, file.getBytes(), file.getOriginalFilename(), file.getContentType(), businessId);
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败", e);
        }
    }

    public RemoteFile uploadBytes(String tenantId, byte[] content, String fileName, String contentType,
                                  String businessId) {
        RemoteFileUploadVo uploaded = inTenant(tenantId,
            () -> remoteFileService.upload(toUpload(content, fileName, contentType, businessId)));
        if (uploaded == null || StringUtils.isBlank(uploaded.getOssId())) {
            throw new ServiceException("资源服务未返回文件标识");
        }
        RemoteFile file = remoteFileService.getById(Long.valueOf(uploaded.getOssId()));
        if (file != null) {
            return file;
        }
        file = new RemoteFile();
        file.setOssId(Long.valueOf(uploaded.getOssId()));
        file.setUrl(uploaded.getUrl());
        file.setFileName(uploaded.getFileName());
        file.setOriginalName(fileName);
        return file;
    }

    public RemoteFileBatchUploadVo uploadBatch(String tenantId, MultipartFile[] files, String businessId) {
        if (files == null || files.length == 0) {
            return new RemoteFileBatchUploadVo();
        }
        List<RemoteFileUploadBo> commands = java.util.Arrays.stream(files)
            .map(file -> toUpload(file, businessId))
            .toList();
        return inTenant(tenantId, () -> remoteFileService.uploadBatch(commands));
    }

    private RemoteFileUploadBo toUpload(MultipartFile file, String businessId) {
        try {
            if (file == null || file.isEmpty()) {
                throw new ServiceException("上传文件不能为空");
            }
            return toUpload(file.getBytes(), file.getOriginalFilename(), file.getContentType(), businessId);
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败", e);
        }
    }

    private RemoteFileUploadBo toUpload(byte[] content, String fileName, String contentType, String businessId) {
        if (content == null || content.length == 0) {
            throw new ServiceException("上传文件内容不能为空");
        }
        String normalizedName = StringUtils.blankToDefault(fileName, "file.bin");
        String normalizedBusinessId = StringUtils.blankToDefault(businessId, "agriculture:file");
        String requestId = DigestUtil.sha256Hex(normalizedBusinessId + ':' + normalizedName + ':'
            + DigestUtil.sha256Hex(content));
        RemoteFileUploadBo command = new RemoteFileUploadBo();
        command.setRequestId(requestId);
        command.setBusinessId(normalizedBusinessId);
        command.setFileName(normalizedName);
        command.setContentType(contentType);
        command.setContent(content);
        return command;
    }

    private static <T> T inTenant(String tenantId, java.util.function.Supplier<T> action) {
        if (StringUtils.isBlank(tenantId)) {
            return action.get();
        }
        return TenantHelper.dynamic(tenantId, action);
    }
}
