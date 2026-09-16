package com.ym.agriculture.farming.news.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farming.news.config.SfNewsMediaProperties;
import com.ym.agriculture.farming.news.dao.SfNewsMediaTransferMapper;
import com.ym.agriculture.farming.news.dao.SfNewsRevisionMapper;
import com.ym.agriculture.farming.news.model.constants.SfNewsConstants;
import com.ym.agriculture.farming.news.model.entity.SfNewsMediaTransfer;
import com.ym.agriculture.farming.news.model.entity.SfNewsRevision;
import com.ym.agriculture.farming.news.model.vo.SfNewsMediaItemVo;
import com.ym.agriculture.farming.news.service.ISfNewsMediaService;
import com.ym.agriculture.farming.news.support.SfNewsContentResult;
import com.ym.agriculture.farming.news.support.SfNewsContentSupport;
import com.ym.agriculture.farming.news.support.SfNewsMasterOssAccessor;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SfNewsMediaServiceImpl implements ISfNewsMediaService {
    private final SfNewsMediaTransferMapper mediaMapper;
    private final SfNewsRevisionMapper revisionMapper;
    private final SfNewsContentSupport contentSupport;
    private final SfNewsMasterOssAccessor ossAccessor;
    private final SfNewsMediaProperties properties;
    private final SfNewsMediaWorker worker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EnqueueResult reconcileRevision(SfNewsRevision revision) {
        Document doc = Jsoup.parseBodyFragment(revision.getSanitizedHtml() == null ? "" : revision.getSanitizedHtml());
        List<MediaRef> refs = extractExternal(doc);
        int skipped = Math.max(0, refs.size() - properties.getMaxItemsPerRevision());
        refs = refs.stream().limit(properties.getMaxItemsPerRevision()).toList();
        Set<String> activeKeys = new HashSet<>();
        Set<String> activeOssIds = new HashSet<>();
        doc.select("[data-oss-id]").forEach(e -> activeOssIds.add(e.attr("data-oss-id")));
        doc.select("[data-poster-oss-id]").forEach(e -> activeOssIds.add(e.attr("data-poster-oss-id")));
        int queued = 0;
        for (MediaRef ref : refs) {
            String hash = DigestUtil.sha256Hex(ref.url());
            activeKeys.add(ref.type() + ":" + hash);
            SfNewsMediaTransfer existing = mediaMapper.selectOne(Wrappers.<SfNewsMediaTransfer>lambdaQuery()
                .eq(SfNewsMediaTransfer::getRevisionId, revision.getRevisionId())
                .eq(SfNewsMediaTransfer::getMediaType, ref.type())
                .eq(SfNewsMediaTransfer::getSourceUrlSha256, hash));
            if (existing == null) {
                SfNewsMediaTransfer task = new SfNewsMediaTransfer();
                task.setTaskId(IdWorker.getId()); task.setRevisionId(revision.getRevisionId());
                task.setMediaType(ref.type()); task.setSourceUrl(ref.url()); task.setSourceUrlSha256(hash);
                task.setTransferStatus("PENDING"); task.setAttemptCount(0);
                task.setCreateTime(new Date()); task.setUpdateTime(new Date());
                mediaMapper.insert(task); queued++;
            } else if ("CANCELLED".equals(existing.getTransferStatus())) {
                mediaMapper.update(null, Wrappers.<SfNewsMediaTransfer>lambdaUpdate()
                    .eq(SfNewsMediaTransfer::getTaskId, existing.getTaskId())
                    .set(SfNewsMediaTransfer::getTransferStatus, "PENDING")
                    .set(SfNewsMediaTransfer::getNextRetryAt, null));
                queued++;
            }
        }
        for (SfNewsMediaTransfer task : listTasks(revision.getRevisionId())) {
            boolean sourceStillPresent = activeKeys.contains(task.getMediaType() + ":" + task.getSourceUrlSha256());
            boolean ossStillPresent = task.getOssId() != null && activeOssIds.contains(String.valueOf(task.getOssId()));
            if (!sourceStillPresent && !ossStillPresent
                && Set.of("PENDING", "PROCESSING", "FAILED", "SUCCESS").contains(task.getTransferStatus())) {
                mediaMapper.update(null, Wrappers.<SfNewsMediaTransfer>lambdaUpdate()
                    .eq(SfNewsMediaTransfer::getTaskId, task.getTaskId())
                    .set(SfNewsMediaTransfer::getTransferStatus, "CANCELLED")
                    .set(SfNewsMediaTransfer::getCompletedAt, new Date()));
            }
        }
        refreshRevisionStatus(revision.getRevisionId());
        return new EnqueueResult(queued, skipped);
    }

    @Override
    public int processPending() {
        if (!properties.isEnabled()) return 0;
        Date now = new Date();
        Date stale = new Date(now.getTime() - properties.getCallTimeoutSeconds() * 2000L);
        int processed = 0;
        for (SfNewsMediaTransfer candidate : mediaMapper.selectRunnable(properties.getBatchSize(), now, stale)) {
            if (!mediaMapper.claim(candidate.getTaskId(), now, stale)) continue;
            candidate = mediaMapper.selectById(candidate.getTaskId());
            try {
                transfer(candidate);
            } catch (TransferException e) {
                markFailure(candidate, e);
            } catch (Exception e) {
                log.warn("农业资讯媒体转存异常 taskId={}", candidate.getTaskId(), e);
                markFailure(candidate, new TransferException("TRANSFER_ERROR", "媒体转存失败", false));
            }
            refreshRevisionStatus(candidate.getRevisionId());
            processed++;
        }
        return processed;
    }

    private void transfer(SfNewsMediaTransfer task) throws Exception {
        long already = mediaMapper.selectList(Wrappers.<SfNewsMediaTransfer>lambdaQuery()
                .eq(SfNewsMediaTransfer::getRevisionId, task.getRevisionId())
                .eq(SfNewsMediaTransfer::getTransferStatus, "SUCCESS"))
            .stream().map(SfNewsMediaTransfer::getFileSize).filter(v -> v != null).mapToLong(Long::longValue).sum();
        Downloaded downloaded = download(task);
        if (already + downloaded.size() > properties.getRevisionMaxBytes()) {
            Files.deleteIfExists(downloaded.path());
            throw new TransferException("REVISION_TOO_LARGE", "单篇文章媒体总大小超过500MB", true);
        }
        try {
            RemoteFile oss = ossAccessor.upload(properties.getPlatformTenantId(), downloaded.path().toFile());
            if (!replaceHtml(task, oss)) {
                mediaMapper.update(null, Wrappers.<SfNewsMediaTransfer>lambdaUpdate()
                    .eq(SfNewsMediaTransfer::getTaskId, task.getTaskId())
                    .set(SfNewsMediaTransfer::getTransferStatus, "CANCELLED")
                    .set(SfNewsMediaTransfer::getCompletedAt, new Date()));
                return;
            }
            mediaMapper.update(null, Wrappers.<SfNewsMediaTransfer>lambdaUpdate()
                .eq(SfNewsMediaTransfer::getTaskId, task.getTaskId())
                .eq(SfNewsMediaTransfer::getTransferStatus, "PROCESSING")
                .set(SfNewsMediaTransfer::getTransferStatus, "SUCCESS")
                .set(SfNewsMediaTransfer::getOssId, oss.getOssId())
                .set(SfNewsMediaTransfer::getOssUrl, oss.getUrl())
                .set(SfNewsMediaTransfer::getContentType, downloaded.contentType())
                .set(SfNewsMediaTransfer::getFileSize, downloaded.size())
                .set(SfNewsMediaTransfer::getErrorCode, null)
                .set(SfNewsMediaTransfer::getErrorMessage, null)
                .set(SfNewsMediaTransfer::getCompletedAt, new Date()));
        } finally {
            Files.deleteIfExists(downloaded.path());
        }
    }

    private Downloaded download(SfNewsMediaTransfer task) throws Exception {
        Files.createDirectories(Path.of(properties.getTempDir()));
        OkHttpClient client = new OkHttpClient.Builder().followRedirects(false)
            .connectTimeout(properties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(properties.getReadTimeoutSeconds(), TimeUnit.SECONDS)
            .callTimeout(properties.getCallTimeoutSeconds(), TimeUnit.SECONDS).build();
        URI uri = URI.create(task.getSourceUrl());
        Response response = null;
        for (int redirect = 0; redirect <= properties.getMaxRedirects(); redirect++) {
            List<InetAddress> pinnedAddresses = validatePublicHttps(uri);
            String pinnedHost = uri.getHost();
            OkHttpClient pinnedClient = client.newBuilder().dns(hostname -> {
                if (hostname.equalsIgnoreCase(pinnedHost)) return pinnedAddresses;
                throw new UnknownHostException("unvalidated host");
            }).build();
            response = pinnedClient.newCall(new Request.Builder().url(uri.toString()).get().build()).execute();
            int code = response.code();
            if (code >= 300 && code < 400) {
                String location = response.header("Location"); response.close();
                if (location == null || redirect == properties.getMaxRedirects())
                    throw new TransferException("REDIRECT_INVALID", "媒体重定向地址无效或次数过多", true);
                uri = uri.resolve(location); continue;
            }
            break;
        }
        if (response == null) throw new TransferException("NO_RESPONSE", "媒体服务器无响应", false);
        Response finalResponse = response;
        try (finalResponse) {
            if (finalResponse.code() >= 500) throw new TransferException("HTTP_" + finalResponse.code(), "媒体服务器暂时不可用", false);
            if (!finalResponse.isSuccessful()) throw new TransferException("HTTP_" + finalResponse.code(), "媒体下载返回HTTP " + finalResponse.code(), true);
            ResponseBody body = finalResponse.body();
            if (body == null) throw new TransferException("EMPTY_BODY", "媒体响应为空", true);
            String declaredType = body.contentType() == null ? "" : body.contentType().toString().toLowerCase(Locale.ROOT);
            if ("VIDEO".equals(task.getMediaType())) {
                if (!(declaredType.startsWith("video/mp4") || declaredType.startsWith("application/mp4")))
                    throw new TransferException("MIME_MISMATCH", "视频响应MIME不是MP4", true);
            } else if (!(declaredType.startsWith("image/jpeg") || declaredType.startsWith("image/png")
                || declaredType.startsWith("image/webp") || declaredType.startsWith("image/gif"))) {
                throw new TransferException("MIME_MISMATCH", "图片响应MIME不受支持", true);
            }
            long max = "VIDEO".equals(task.getMediaType()) ? properties.getVideoMaxBytes() : properties.getImageMaxBytes();
            if (body.contentLength() > max) throw new TransferException("FILE_TOO_LARGE", "媒体文件超过允许大小", true);
            Path raw = Files.createTempFile(Path.of(properties.getTempDir()), "news-" + task.getTaskId() + "-", ".download");
            long size;
            try {
                try (InputStream in = body.byteStream(); var out = Files.newOutputStream(raw)) {
                    byte[] buffer = new byte[8192]; size = 0; int read;
                    while ((read = in.read(buffer)) >= 0) {
                        size += read;
                        if (size > max) throw new TransferException("FILE_TOO_LARGE", "媒体文件超过允许大小", true);
                        out.write(buffer, 0, read);
                    }
                }
            } catch (Exception e) {
                Files.deleteIfExists(raw);
                throw e;
            }
            MediaFormat format;
            try { format = detect(raw, task.getMediaType()); }
            catch (Exception e) { Files.deleteIfExists(raw); throw e; }
            Path named = raw.resolveSibling(raw.getFileName() + format.extension());
            Files.move(raw, named, StandardCopyOption.REPLACE_EXISTING);
            return new Downloaded(named, size, format.contentType());
        }
    }

    static List<InetAddress> validatePublicHttps(URI uri) throws Exception {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || StrUtil.isBlank(uri.getHost()) || uri.getUserInfo() != null
            || (uri.getPort() != -1 && uri.getPort() != 443))
            throw new TransferException("UNSAFE_URL", "媒体必须是公网HTTPS直链", true);
        if ("localhost".equalsIgnoreCase(uri.getHost())) throw new TransferException("UNSAFE_URL", "禁止访问本机地址", true);
        List<InetAddress> addresses = List.of(InetAddress.getAllByName(uri.getHost()));
        for (InetAddress address : addresses) {
            byte[] b = address.getAddress();
            boolean carrierNat = b.length == 4 && (b[0] & 255) == 100 && ((b[1] & 255) >= 64 && (b[1] & 255) <= 127);
            boolean uniqueLocalV6 = b.length == 16 && ((b[0] & 0xfe) == 0xfc);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress() || carrierNat || uniqueLocalV6)
                throw new TransferException("UNSAFE_URL", "媒体域名解析到非公网地址", true);
        }
        return addresses;
    }

    private MediaFormat detect(Path path, String type) throws IOException, TransferException {
        byte[] h;
        try (InputStream in = Files.newInputStream(path)) { h = in.readNBytes(16); }
        if ("VIDEO".equals(type)) {
            if (h.length >= 12 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p') return new MediaFormat(".mp4", "video/mp4");
            throw new TransferException("UNSUPPORTED_TYPE", "仅支持MP4视频直链", true);
        }
        if (h.length >= 3 && (h[0] & 255) == 0xff && (h[1] & 255) == 0xd8 && (h[2] & 255) == 0xff) return new MediaFormat(".jpg", "image/jpeg");
        if (h.length >= 8 && (h[0] & 255) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return new MediaFormat(".png", "image/png");
        if (h.length >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F') return new MediaFormat(".gif", "image/gif");
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return new MediaFormat(".webp", "image/webp");
        throw new TransferException("UNSUPPORTED_TYPE", "仅支持JPEG、PNG、WebP、GIF图片", true);
    }

    private boolean replaceHtml(SfNewsMediaTransfer task, RemoteFile oss) {
        for (int i = 0; i < 5; i++) {
            SfNewsRevision revision = revisionMapper.selectRevision(task.getRevisionId());
            if (revision == null || !Set.of("PENDING", "REJECTED").contains(revision.getReviewStatus())) return false;
            String oldHtml = revision.getSanitizedHtml();
            Document doc = Jsoup.parseBodyFragment(oldHtml);
            int changed = 0;
            if ("IMAGE".equals(task.getMediaType())) {
                for (Element e : doc.select("img[src]")) if (task.getSourceUrl().equals(e.attr("src"))) { e.attr("src", oss.getUrl()); e.attr("data-oss-id", String.valueOf(oss.getOssId())); changed++; }
            } else if ("VIDEO".equals(task.getMediaType())) {
                for (Element e : doc.select("video[src],video source[src]")) if (task.getSourceUrl().equals(e.attr("src"))) { e.attr("src", oss.getUrl()); e.attr("data-oss-id", String.valueOf(oss.getOssId())); changed++; }
            } else {
                for (Element e : doc.select("video[poster]")) if (task.getSourceUrl().equals(e.attr("poster"))) { e.attr("poster", oss.getUrl()); e.attr("data-poster-oss-id", String.valueOf(oss.getOssId())); changed++; }
            }
            if (changed == 0) return false;
            SfNewsContentResult content = contentSupport.sanitizeAndConvert(doc.body().html());
            if (revisionMapper.updateMediaHtmlCas(revision.getRevisionId(), oldHtml, content.getSanitizedHtml(),
                content.getContentBlocksJson(), content.getFirstImageUrl(), DigestUtil.sha256Hex(content.getSanitizedHtml())) > 0) return true;
        }
        throw new ServiceException("正文正在被编辑，媒体合并失败，请稍后重试");
    }

    private List<MediaRef> extractExternal(Document doc) {
        List<MediaRef> refs = new ArrayList<>(); Set<String> seen = new HashSet<>();
        doc.select("img[src]").forEach(e -> add(refs, seen, "IMAGE", e.attr("src"), e.attr("data-oss-id")));
        doc.select("video[src],video source[src]").forEach(e -> add(refs, seen, "VIDEO", e.attr("src"), e.attr("data-oss-id")));
        doc.select("video[poster]").forEach(e -> add(refs, seen, "POSTER", e.attr("poster"), e.attr("data-poster-oss-id")));
        return refs;
    }

    private void add(List<MediaRef> refs, Set<String> seen, String type, String url, String ossId) {
        if (StrUtil.isBlank(url) || StrUtil.isNotBlank(ossId)) return;
        String key = type + ":" + url;
        if (seen.add(key)) refs.add(new MediaRef(type, url));
    }

    private void markFailure(SfNewsMediaTransfer task, TransferException e) {
        int attempts = task.getAttemptCount() == null ? 1 : task.getAttemptCount();
        boolean finalFailure = e.permanent || attempts >= properties.getMaxAttempts();
        Date retry = null;
        if (!finalFailure) {
            List<Integer> delays = properties.getRetryDelaysSeconds();
            int seconds = delays.get(Math.min(Math.max(0, attempts - 1), delays.size() - 1));
            retry = new Date(System.currentTimeMillis() + seconds * 1000L);
        }
        mediaMapper.update(null, Wrappers.<SfNewsMediaTransfer>lambdaUpdate()
            .eq(SfNewsMediaTransfer::getTaskId, task.getTaskId())
            .eq(SfNewsMediaTransfer::getTransferStatus, "PROCESSING")
            .set(SfNewsMediaTransfer::getTransferStatus, finalFailure ? "FAILED" : "PENDING")
            .set(SfNewsMediaTransfer::getNextRetryAt, retry)
            .set(SfNewsMediaTransfer::getErrorCode, e.code)
            .set(SfNewsMediaTransfer::getErrorMessage, StrUtil.sub(e.getMessage(), 0, 1000))
            .set(SfNewsMediaTransfer::getCompletedAt, finalFailure ? new Date() : null));
    }

    private void refreshRevisionStatus(Long revisionId) {
        List<SfNewsMediaTransfer> active = listTasks(revisionId).stream().filter(t -> !"CANCELLED".equals(t.getTransferStatus())).toList();
        int total = active.size(); int success = (int) active.stream().filter(t -> "SUCCESS".equals(t.getTransferStatus())).count();
        int failed = (int) active.stream().filter(t -> "FAILED".equals(t.getTransferStatus())).count();
        String status = total == 0 ? SfNewsConstants.MEDIA_NONE : failed > 0 ? SfNewsConstants.MEDIA_FAILED
            : success == total ? SfNewsConstants.MEDIA_READY : SfNewsConstants.MEDIA_TRANSFERRING;
        revisionMapper.update(null, Wrappers.<SfNewsRevision>lambdaUpdate().eq(SfNewsRevision::getRevisionId, revisionId)
            .set(SfNewsRevision::getMediaStatus, status).set(SfNewsRevision::getMediaTotal, total)
            .set(SfNewsRevision::getMediaSucceeded, success).set(SfNewsRevision::getMediaFailed, failed));
    }

    private List<SfNewsMediaTransfer> listTasks(Long revisionId) {
        return mediaMapper.selectList(Wrappers.<SfNewsMediaTransfer>lambdaQuery()
            .eq(SfNewsMediaTransfer::getRevisionId, revisionId).orderByAsc(SfNewsMediaTransfer::getCreateTime));
    }

    @Override public void wake() { worker.wake(); }

    @Override
    public void retryFailed(Long revisionId) {
        mediaMapper.update(null, Wrappers.<SfNewsMediaTransfer>lambdaUpdate()
            .eq(SfNewsMediaTransfer::getRevisionId, revisionId).eq(SfNewsMediaTransfer::getTransferStatus, "FAILED")
            .set(SfNewsMediaTransfer::getTransferStatus, "PENDING").set(SfNewsMediaTransfer::getAttemptCount, 0)
            .set(SfNewsMediaTransfer::getNextRetryAt, null).set(SfNewsMediaTransfer::getErrorCode, null)
            .set(SfNewsMediaTransfer::getErrorMessage, null));
        refreshRevisionStatus(revisionId); wake();
    }

    @Override public List<SfNewsMediaItemVo> listByRevision(Long revisionId) {
        return listTasks(revisionId).stream().filter(t -> !"CANCELLED".equals(t.getTransferStatus())).map(t -> {
            SfNewsMediaItemVo vo = new SfNewsMediaItemVo();
            vo.setTaskId(t.getTaskId()); vo.setMediaType(t.getMediaType()); vo.setSourceUrl(t.getSourceUrl());
            vo.setTransferStatus(t.getTransferStatus()); vo.setOssId(t.getOssId()); vo.setOssUrl(t.getOssUrl());
            vo.setContentType(t.getContentType()); vo.setFileSize(t.getFileSize()); vo.setAttemptCount(t.getAttemptCount());
            vo.setErrorCode(t.getErrorCode()); vo.setErrorMessage(t.getErrorMessage()); return vo;
        }).toList();
    }

    @Override public void validatePublishable(SfNewsRevision revision) {
        if (!Set.of(SfNewsConstants.MEDIA_NONE, SfNewsConstants.MEDIA_READY).contains(revision.getMediaStatus()))
            throw new ServiceException("正文媒体仍在转存或存在失败项，请处理后再发布");
        Document doc = Jsoup.parseBodyFragment(revision.getSanitizedHtml());
        List<Element> elements = new ArrayList<>(); elements.addAll(doc.select("img[data-oss-id],video[data-oss-id],video source[data-oss-id]"));
        for (Element e : elements) validateOss(e.attr("data-oss-id"), e.attr("src"));
        for (Element e : doc.select("video[data-poster-oss-id]")) validateOss(e.attr("data-poster-oss-id"), e.attr("poster"));
        contentSupport.validatePublishableMedia(revision.getSanitizedHtml());
    }

    private void validateOss(String id, String url) {
        try {
            RemoteFile oss = ossAccessor.get(properties.getPlatformTenantId(), Long.valueOf(id));
            if (oss == null || !sameOssUrl(oss.getUrl(), url)) throw new ServiceException("正文存在无效或伪造的OSS媒体标识");
        } catch (NumberFormatException e) { throw new ServiceException("正文存在无效的OSS媒体标识"); }
    }

    private boolean sameOssUrl(String expected, String actual) {
        if (StrUtil.isBlank(expected) || StrUtil.isBlank(actual)) return false;
        try { return URI.create(expected).getPath().equals(URI.create(actual).getPath()); }
        catch (Exception e) { return expected.equals(actual); }
    }

    private record MediaRef(String type, String url) { }
    private record Downloaded(Path path, long size, String contentType) { }
    private record MediaFormat(String extension, String contentType) { }
    static class TransferException extends Exception {
        final String code; final boolean permanent;
        TransferException(String code, String message, boolean permanent) { super(message); this.code = code; this.permanent = permanent; }
    }
}
