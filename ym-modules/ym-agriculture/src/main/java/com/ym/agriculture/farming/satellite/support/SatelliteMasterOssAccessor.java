package com.ym.agriculture.farming.satellite.support;

import com.ym.agriculture.shared.support.AgricultureRemoteFileAccessor;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 遥感模块访问平台 OSS 的远程边界。 */
@Component
@RequiredArgsConstructor
public class SatelliteMasterOssAccessor {

    private final AgricultureRemoteFileAccessor files;

    public RemoteFile uploadBytes(String tenantId, byte[] content, String originalFileName, String contentType) {
        return files.uploadBytes(tenantId, content, originalFileName, contentType, "satellite-result-image");
    }
}
