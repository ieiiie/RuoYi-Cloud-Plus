package com.ym.resource.api;

import com.ym.common.core.exception.ServiceException;
import com.ym.resource.api.domain.RemoteFile;
import com.ym.resource.api.domain.RemoteFileBatchUploadVo;
import com.ym.resource.api.domain.RemoteFileUploadBo;
import com.ym.resource.api.domain.RemoteFileUploadVo;

import java.util.List;

/**
 * 文件服务
 *
 * @author Lion Li
 */
public interface RemoteFileService {

    /**
     * 通过ossId查询对应的url
     *
     * @param ossIds ossId串逗号分隔
     * @return url串逗号分隔
     */
    String selectUrlByIds(String ossIds);

    /**
     * 通过ossId查询列表
     *
     * @param ossIds ossId串逗号分隔
     * @return 列表
     */
    List<RemoteFile> selectByIds(String ossIds);

    RemoteFile getById(Long ossId);

    RemoteFile findByUrl(String url);

    RemoteFileUploadVo upload(RemoteFileUploadBo file);

    RemoteFileBatchUploadVo uploadBatch(List<RemoteFileUploadBo> files);
}
