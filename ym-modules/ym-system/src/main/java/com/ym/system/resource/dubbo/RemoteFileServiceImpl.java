package com.ym.system.resource.dubbo;

import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.oss.client.OssClient;
import com.ym.common.oss.factory.OssFactory;
import com.ym.common.oss.model.PutObjectResult;
import com.ym.resource.api.RemoteFileService;
import com.ym.resource.api.domain.RemoteFile;
import com.ym.resource.api.domain.RemoteFileBatchUploadVo;
import com.ym.resource.api.domain.RemoteFileUploadBo;
import com.ym.resource.api.domain.RemoteFileUploadFailVo;
import com.ym.resource.api.domain.RemoteFileUploadVo;
import com.ym.system.resource.domain.SysOssExt;
import com.ym.system.resource.domain.bo.SysOssBo;
import com.ym.system.resource.domain.vo.SysOssVo;
import com.ym.system.resource.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件请求处理
 *
 * @author Lion Li
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DubboService
public class RemoteFileServiceImpl implements RemoteFileService {

    private final ISysOssService sysOssService;

    /**
     * 通过ossId查询对应的url
     *
     * @param ossIds ossId串逗号分隔
     * @return url串逗号分隔
     */
    @Override
    public String selectUrlByIds(String ossIds) {
        return sysOssService.selectUrlByIds(ossIds);
    }

    /**
     * 通过ossId查询列表
     *
     * @param ossIds ossId串逗号分隔
     * @return 列表
     */
    @Override
    public List<RemoteFile> selectByIds(String ossIds) {
        List<SysOssVo> sysOssVos = sysOssService.listByIds(StringUtils.splitTo(ossIds, Convert::toLong));
        return MapstructUtils.convert(sysOssVos, RemoteFile.class);
    }

    @Override
    public RemoteFile getById(Long ossId) {
        return toRemote(sysOssService.getById(ossId));
    }

    @Override
    public RemoteFile findByUrl(String url) {
        if (StringUtils.isBlank(url)) return null;
        return sysOssService.queryPageList(
                new com.ym.system.resource.domain.bo.SysOssBo(),
                new com.ym.common.mybatis.core.page.PageQuery(100, 1))
            .getRows().stream().filter(row -> url.trim().equals(row.getUrl())).findFirst()
            .map(RemoteFileServiceImpl::toRemote).orElse(null);
    }

    @Override
    public RemoteFileUploadVo upload(RemoteFileUploadBo file) {
        validateUpload(file);
        SysOssVo saved = sysOssService.upload(new BytesMultipartFile(file));
        RemoteFileUploadVo result = new RemoteFileUploadVo();
        result.setUrl(saved.getUrl());
        result.setFileName(saved.getFileName());
        result.setOssId(String.valueOf(saved.getOssId()));
        return result;
    }

    @Override
    public RemoteFileBatchUploadVo uploadBatch(List<RemoteFileUploadBo> files) {
        RemoteFileBatchUploadVo result = new RemoteFileBatchUploadVo();
        for (RemoteFileUploadBo file : files == null ? List.<RemoteFileUploadBo>of() : files) {
            try {
                result.getSuccessList().add(upload(file));
            } catch (Exception ex) {
                RemoteFileUploadFailVo failure = new RemoteFileUploadFailVo();
                failure.setFileName(file == null ? "" : StringUtils.defaultIfBlank(file.getFileName(), ""));
                failure.setMessage(ex.getMessage());
                result.getFailList().add(failure);
            }
        }
        return result;
    }

    private static RemoteFile toRemote(SysOssVo source) {
        if (source == null) return null;
        RemoteFile target = MapstructUtils.convert(source, RemoteFile.class);
        target.setName(source.getFileName());
        target.setFileName(source.getFileName());
        return target;
    }

    private static void validateUpload(RemoteFileUploadBo file) {
        if (file == null || StringUtils.isBlank(file.getRequestId()) || StringUtils.isBlank(file.getBusinessId())
            || file.getContent() == null || file.getContent().length == 0) {
            throw new ServiceException("文件上传缺少幂等标识或文件内容");
        }
    }

    private static final class BytesMultipartFile implements MultipartFile {
        private final RemoteFileUploadBo source;
        private BytesMultipartFile(RemoteFileUploadBo source) { this.source = source; }
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return source.getFileName(); }
        @Override public String getContentType() { return source.getContentType(); }
        @Override public boolean isEmpty() { return source.getContent().length == 0; }
        @Override public long getSize() { return source.getContent().length; }
        @Override public byte[] getBytes() { return source.getContent(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(source.getContent()); }
        @Override public void transferTo(java.io.File dest) throws IOException { java.nio.file.Files.write(dest.toPath(), source.getContent()); }
    }
}
