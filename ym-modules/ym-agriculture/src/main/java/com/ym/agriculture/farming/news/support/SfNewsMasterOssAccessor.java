package com.ym.agriculture.farming.news.support;

import com.ym.agriculture.shared.support.AgricultureRemoteFileAccessor;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
public class SfNewsMasterOssAccessor {
    private final AgricultureRemoteFileAccessor files;

    public RemoteFile upload(String tenantId, File file) {
        return files.upload(tenantId, file, "agriculture-news-media");
    }

    public RemoteFile get(String tenantId, Long ossId) {
        return files.getById(tenantId, ossId);
    }
}
