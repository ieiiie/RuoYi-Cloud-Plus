package com.ym.agriculture.farmtask.inspectionphotoarchive.support;

import com.ym.agriculture.shared.support.AgricultureRemoteFileAccessor;
import com.ym.resource.api.domain.RemoteFileBatchUploadVo;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/** 巡查图片归档访问平台 OSS 的远程边界。 */
@Component
@RequiredArgsConstructor
public class SfInspectionPhotoArchiveMasterOssAccessor {

    private final AgricultureRemoteFileAccessor files;

    public RemoteFileBatchUploadVo uploadBatch(String tenantId, MultipartFile[] uploadFiles) {
        return files.uploadBatch(tenantId, uploadFiles, "inspection-photo-archive");
    }

    public List<RemoteFile> listByIds(String tenantId, Collection<Long> ossIds) {
        return files.listByIds(tenantId, ossIds);
    }

    /** 保留历史方法签名；签名地址由资源服务按存储策略生成。 */
    public List<RemoteFile> listByIds(String tenantId, Collection<Long> ossIds, Duration signedUrlTtl) {
        return files.listByIds(tenantId, ossIds);
    }
}
