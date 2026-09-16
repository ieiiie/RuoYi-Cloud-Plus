package com.ym.resource.api;

import lombok.extern.slf4j.Slf4j;
import com.ym.common.core.utils.StringUtils;
import com.ym.resource.api.domain.RemoteFile;
import com.ym.resource.api.domain.RemoteFileBatchUploadVo;
import com.ym.resource.api.domain.RemoteFileUploadBo;
import com.ym.resource.api.domain.RemoteFileUploadVo;

import java.util.List;

/**
 * 文件服务(降级处理)
 *
 * @author Lion Li
 */
@Slf4j
public class RemoteFileServiceMock implements RemoteFileService {

    /**
     * 通过ossId查询对应的url
     *
     * @param ossIds ossId串逗号分隔
     * @return url串逗号分隔
     */
    @Override
    public String selectUrlByIds(String ossIds) {
        log.warn("服务调用异常 -> 降级处理");
        return StringUtils.EMPTY;
    }

    /**
     * 通过ossId查询列表
     *
     * @param ossIds ossId串逗号分隔
     * @return 列表
     */
    @Override
    public List<RemoteFile> selectByIds(String ossIds) {
        log.warn("服务调用异常 -> 降级处理");
        return List.of();
    }

    @Override public RemoteFile getById(Long ossId) { return null; }
    @Override public RemoteFile findByUrl(String url) { return null; }
    @Override public RemoteFileUploadVo upload(RemoteFileUploadBo file) { return null; }
    @Override public RemoteFileBatchUploadVo uploadBatch(List<RemoteFileUploadBo> files) { return new RemoteFileBatchUploadVo(); }

}
