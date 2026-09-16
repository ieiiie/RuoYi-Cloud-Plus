package com.ym.agriculture.farmtask.inspection.support;

import com.ym.agriculture.shared.support.AgricultureRemoteFileAccessor;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/** 抽检业务访问平台 OSS 元数据的远程边界。 */
@Component
@RequiredArgsConstructor
public class StaskInspectionMasterOssAccessor {

    private final AgricultureRemoteFileAccessor files;

    public List<RemoteFile> listByIds(String tenantId, Collection<Long> ossIds) {
        return files.listByIds(tenantId, ossIds);
    }

    public RemoteFile findByUrl(String tenantId, String url) {
        return files.findByUrl(tenantId, url);
    }
}
