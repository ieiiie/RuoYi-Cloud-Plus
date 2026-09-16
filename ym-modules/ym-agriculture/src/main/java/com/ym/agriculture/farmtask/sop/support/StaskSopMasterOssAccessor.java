package com.ym.agriculture.farmtask.sop.support;

import com.ym.agriculture.shared.support.AgricultureRemoteFileAccessor;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/** SOP 访问平台 OSS 元数据的远程边界。 */
@Component
@RequiredArgsConstructor
public class StaskSopMasterOssAccessor {

    private final AgricultureRemoteFileAccessor files;

    public RemoteFile getById(Long ossId) {
        return files.getById(TenantHelper.getTenantId(), ossId);
    }

    public List<RemoteFile> listByIds(Collection<Long> ossIds) {
        return files.listByIds(TenantHelper.getTenantId(), ossIds);
    }
}
