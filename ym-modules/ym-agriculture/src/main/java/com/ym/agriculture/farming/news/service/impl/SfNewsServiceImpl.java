package com.ym.agriculture.farming.news.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.news.dao.SfNewsArticleMapper;
import com.ym.agriculture.farming.news.dao.SfNewsIngestStagingMapper;
import com.ym.agriculture.farming.news.dao.SfNewsReviewLogMapper;
import com.ym.agriculture.farming.news.dao.SfNewsRevisionMapper;
import com.ym.agriculture.farming.news.dao.SfNewsSourceMapper;
import com.ym.agriculture.farming.news.model.bo.SfNewsQueryBo;
import com.ym.agriculture.farming.news.model.bo.SfNewsReviewBo;
import com.ym.agriculture.farming.news.model.bo.SfNewsRevisionEditBo;
import com.ym.agriculture.farming.news.model.constants.SfNewsConstants;
import com.ym.agriculture.farming.news.model.entity.SfNewsArticle;
import com.ym.agriculture.farming.news.model.entity.SfNewsIngestStaging;
import com.ym.agriculture.farming.news.model.entity.SfNewsReviewLog;
import com.ym.agriculture.farming.news.model.entity.SfNewsRevision;
import com.ym.agriculture.farming.news.model.entity.SfNewsSource;
import com.ym.agriculture.farming.news.model.vo.SfNewsAdminDetailVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsAdminListVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsDashboardVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsIngestProcessVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsMobileDetailVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsMobileListVo;
import com.ym.agriculture.farming.news.service.ISfNewsService;
import com.ym.agriculture.farming.news.service.ISfNewsMediaService;
import com.ym.agriculture.farming.news.support.SfNewsContentResult;
import com.ym.agriculture.farming.news.support.SfNewsContentSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 农业资讯采集、审核、发布与移动端查询服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfNewsServiceImpl implements ISfNewsService {

    private static final Set<String> CATEGORIES = Set.of(
        SfNewsConstants.CATEGORY_POLICY,
        SfNewsConstants.CATEGORY_KNOWLEDGE,
        SfNewsConstants.CATEGORY_MARKET,
        SfNewsConstants.CATEGORY_GENERAL
    );

    private final SfNewsArticleMapper articleMapper;
    private final SfNewsRevisionMapper revisionMapper;
    private final SfNewsIngestStagingMapper ingestMapper;
    private final SfNewsReviewLogMapper reviewLogMapper;
    private final SfNewsSourceMapper sourceMapper;
    private final SfNewsContentSupport contentSupport;
    private final TransactionTemplate transactionTemplate;
    private final ISfNewsMediaService mediaService;

    @Override
    public PageResult<SfNewsAdminListVo> queryAdminPage(SfNewsQueryBo bo, PageQuery pageQuery) {
        SfNewsQueryBo query = bo == null ? new SfNewsQueryBo() : bo;
        Page<SfNewsAdminListVo> page = articleMapper.selectAdminPage(pageQuery.build(), query);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    public SfNewsAdminDetailVo getAdminDetail(Long articleId) {
        SfNewsArticle article = requireArticle(articleId);
        SfNewsRevision revision = requireRevision(article.getCurrentRevisionId());
        SfNewsAdminDetailVo vo = new SfNewsAdminDetailVo();
        BeanUtil.copyProperties(article, vo);
        BeanUtil.copyProperties(revision, vo);
        vo.setRevisionId(revision.getRevisionId());
        vo.setTags(parseTags(revision.getTagsJson()));
        vo.setContentBlocks(parseBlocks(revision.getContentBlocksJson()));
        vo.setMediaItems(mediaService.listByRevision(revision.getRevisionId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editRevision(Long articleId, SfNewsRevisionEditBo bo) {
        SfNewsArticle article = requireArticle(articleId);
        SfNewsRevision revision = requireCurrentRevision(article, bo.getRevisionId());
        boolean directPublish = SfNewsConstants.REVIEW_APPROVED.equals(revision.getReviewStatus());
        if (!directPublish && !Set.of(SfNewsConstants.REVIEW_PENDING, SfNewsConstants.REVIEW_REJECTED)
            .contains(revision.getReviewStatus())) {
            throw new ServiceException("当前版本状态不允许修改");
        }
        if (directPublish) {
            StpUtil.checkPermission("smartfarming:news:review");
        }
        validateCategory(bo.getCategory());
        SfNewsContentResult content = contentSupport.sanitizeAndConvert(bo.getSanitizedHtml());
        if (directPublish) {
            contentSupport.validatePublishableMedia(content.getSanitizedHtml());
        }
        Date now = new Date();
        Long operatorId = LoginHelper.getUserId();
        var revisionUpdate = Wrappers.<SfNewsRevision>lambdaUpdate()
            .eq(SfNewsRevision::getRevisionId, revision.getRevisionId())
            .set(SfNewsRevision::getTitle, bo.getTitle().trim())
            .set(SfNewsRevision::getSummary, StrUtil.trim(bo.getSummary()))
            .set(SfNewsRevision::getCategory, bo.getCategory())
            .set(SfNewsRevision::getSourceName, bo.getSourceName().trim())
            .set(SfNewsRevision::getAuthor, StrUtil.trim(bo.getAuthor()))
            .set(SfNewsRevision::getTagsJson, JSON.toJSONString(bo.getTags() == null ? List.of() : bo.getTags()))
            .set(SfNewsRevision::getSanitizedHtml, content.getSanitizedHtml())
            .set(SfNewsRevision::getContentBlocksJson, content.getContentBlocksJson())
            .set(SfNewsRevision::getCoverUrl, content.getFirstImageUrl())
            .set(SfNewsRevision::getContentSha256, DigestUtil.sha256Hex(content.getSanitizedHtml()))
            .set(SfNewsRevision::getUpdateBy, operatorId)
            .set(SfNewsRevision::getUpdateTime, now);
        if (directPublish) {
            revisionUpdate
                .set(SfNewsRevision::getReviewStatus, SfNewsConstants.REVIEW_APPROVED)
                .set(SfNewsRevision::getReviewerId, operatorId)
                .set(SfNewsRevision::getReviewerName, LoginHelper.getUsername())
                .set(SfNewsRevision::getReviewComment, "审核员直接修改并发布")
                .set(SfNewsRevision::getReviewedAt, now);
        } else {
            revisionUpdate
                .set(SfNewsRevision::getReviewStatus, SfNewsConstants.REVIEW_PENDING)
                .set(SfNewsRevision::getReviewerId, null)
                .set(SfNewsRevision::getReviewerName, null)
                .set(SfNewsRevision::getReviewComment, null)
                .set(SfNewsRevision::getReviewedAt, null);
        }
        revisionMapper.update(null, revisionUpdate);
        articleMapper.update(null, Wrappers.<SfNewsArticle>lambdaUpdate()
            .eq(SfNewsArticle::getArticleId, articleId)
            .set(SfNewsArticle::getRecommendFlag, Boolean.TRUE.equals(bo.getRecommendFlag()))
            .set(SfNewsArticle::getSortOrder, bo.getSortOrder() == null ? 0 : bo.getSortOrder())
            .set(SfNewsArticle::getArticleStatus,
                article.getPublishedRevisionId() == null ? SfNewsConstants.ARTICLE_REVIEWING : article.getArticleStatus())
            .set(SfNewsArticle::getUpdateBy, operatorId)
            .set(SfNewsArticle::getUpdateTime, now));
        insertReviewLog(articleId, revision.getRevisionId(),
            directPublish ? "EDIT_PUBLISHED" : "EDIT",
            directPublish ? "审核员直接修改并发布" : "审核人员修改内容和格式");
        SfNewsRevision edited = revisionMapper.selectRevision(revision.getRevisionId());
        mediaService.reconcileRevision(edited);
        wakeMediaAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long articleId, SfNewsReviewBo bo) {
        SfNewsArticle article = requireArticle(articleId);
        SfNewsRevision revision = requireCurrentRevision(article, bo.getRevisionId());
        if (!SfNewsConstants.REVIEW_PENDING.equals(revision.getReviewStatus())) {
            throw new ServiceException("只有待审核版本可以执行审核");
        }
        String action = bo.getAction().trim().toUpperCase();
        if ("APPROVE".equals(action)) {
            approve(article, revision, bo.getComment());
            return;
        }
        if ("REJECT".equals(action)) {
            reject(article, revision, bo.getComment());
            return;
        }
        throw new ServiceException("审核动作仅支持 APPROVE 或 REJECT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long articleId, String comment) {
        SfNewsArticle article = requireArticle(articleId);
        if (!SfNewsConstants.ARTICLE_PUBLISHED.equals(article.getArticleStatus())) {
            throw new ServiceException("只有已发布文章可以下架");
        }
        articleMapper.update(null, Wrappers.<SfNewsArticle>lambdaUpdate()
            .eq(SfNewsArticle::getArticleId, articleId)
            .eq(SfNewsArticle::getArticleStatus, SfNewsConstants.ARTICLE_PUBLISHED)
            .set(SfNewsArticle::getArticleStatus, SfNewsConstants.ARTICLE_OFFLINE)
            .set(SfNewsArticle::getUpdateBy, LoginHelper.getUserId())
            .set(SfNewsArticle::getUpdateTime, new Date()));
        insertReviewLog(articleId, article.getPublishedRevisionId(), "OFFLINE", comment);
    }

    @Override
    public SfNewsIngestProcessVo processPending(int limit) {
        List<SfNewsIngestStaging> pending = ingestMapper.selectPending(Math.max(1, Math.min(limit, 100)));
        int claimed = 0;
        int succeeded = 0;
        int rejected = 0;
        int failed = 0;
        int mediaQueued = 0;
        int mediaSkipped = 0;
        for (SfNewsIngestStaging row : pending) {
            if (!ingestMapper.claim(row.getId(), new Date())) {
                continue;
            }
            claimed++;
            try {
                ISfNewsMediaService.EnqueueResult media = transactionTemplate.execute(status -> processClaimed(row));
                if (media != null) {
                    mediaQueued += media.queued();
                    mediaSkipped += media.skipped();
                }
                succeeded++;
            } catch (IllegalArgumentException e) {
                rejected++;
                markIngestError(row.getId(), SfNewsConstants.INGEST_REJECTED, "VALIDATION_ERROR", e.getMessage());
            } catch (Exception e) {
                failed++;
                log.error("农业资讯暂存处理失败 requestId={}", row.getRequestId(), e);
                markIngestError(row.getId(), SfNewsConstants.INGEST_FAILED, "INTERNAL_ERROR", "平台处理失败，请联系管理员");
            }
        }
        if (mediaQueued > 0) mediaService.wake();
        return new SfNewsIngestProcessVo(claimed, succeeded, rejected, failed, mediaQueued, mediaSkipped);
    }

    @Override
    public PageResult<SfNewsMobileListVo> queryMobilePage(String category, String keyword, PageQuery pageQuery) {
        if (StrUtil.isNotBlank(category) && !"recommend".equals(category) && !CATEGORIES.contains(category)) {
            throw new ServiceException("不支持的资讯分类");
        }
        Page<SfNewsMobileListVo> page = articleMapper.selectMobilePage(pageQuery.build(), category, keyword);
        page.getRecords().forEach(this::enrichMobileListItem);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    public SfNewsMobileDetailVo getMobileDetail(Long articleId) {
        SfNewsArticle article = articleMapper.selectOne(Wrappers.<SfNewsArticle>lambdaQuery()
            .eq(SfNewsArticle::getArticleId, articleId)
            .eq(SfNewsArticle::getArticleStatus, SfNewsConstants.ARTICLE_PUBLISHED)
            .eq(SfNewsArticle::getDelFlag, "0"));
        if (article == null || article.getPublishedRevisionId() == null) {
            throw new ServiceException("资讯不存在或已下架");
        }
        SfNewsRevision revision = requireRevision(article.getPublishedRevisionId());
        SfNewsMobileDetailVo vo = new SfNewsMobileDetailVo();
        BeanUtil.copyProperties(article, vo);
        BeanUtil.copyProperties(revision, vo);
        vo.setSource(revision.getSourceName());
        vo.setCategoryName(categoryName(revision.getCategory()));
        vo.setTags(parseTags(revision.getTagsJson()));
        vo.setContentBlocks(parseBlocks(revision.getContentBlocksJson()));
        articleMapper.incrementReadCount(articleId);
        return vo;
    }

    @Override
    public SfNewsDashboardVo getDashboard() {
        PageQuery query = new PageQuery(5, 1);
        PageResult<SfNewsMobileListVo> page = queryMobilePage("recommend", null, query);
        SfNewsDashboardVo dashboard = new SfNewsDashboardVo();
        dashboard.setFeatured(new ArrayList<>(page.getRows()));
        return dashboard;
    }

    private void approve(SfNewsArticle article, SfNewsRevision revision, String comment) {
        mediaService.validatePublishable(revision);
        SfNewsContentResult content = contentSupport.sanitizeAndConvert(revision.getSanitizedHtml());
        Date now = new Date();
        Long operatorId = LoginHelper.getUserId();
        String operatorName = LoginHelper.getUsername();
        revisionMapper.update(null, Wrappers.<SfNewsRevision>lambdaUpdate()
            .eq(SfNewsRevision::getRevisionId, revision.getRevisionId())
            .eq(SfNewsRevision::getReviewStatus, SfNewsConstants.REVIEW_PENDING)
            .set(SfNewsRevision::getReviewStatus, SfNewsConstants.REVIEW_APPROVED)
            .set(SfNewsRevision::getSanitizedHtml, content.getSanitizedHtml())
            .set(SfNewsRevision::getContentBlocksJson, content.getContentBlocksJson())
            .set(SfNewsRevision::getCoverUrl, content.getFirstImageUrl())
            .set(SfNewsRevision::getReviewerId, operatorId)
            .set(SfNewsRevision::getReviewerName, operatorName)
            .set(SfNewsRevision::getReviewComment, StrUtil.trim(comment))
            .set(SfNewsRevision::getReviewedAt, now)
            .set(SfNewsRevision::getUpdateBy, operatorId)
            .set(SfNewsRevision::getUpdateTime, now));
        articleMapper.update(null, Wrappers.<SfNewsArticle>lambdaUpdate()
            .eq(SfNewsArticle::getArticleId, article.getArticleId())
            .eq(SfNewsArticle::getCurrentRevisionId, revision.getRevisionId())
            .set(SfNewsArticle::getPublishedRevisionId, revision.getRevisionId())
            .set(SfNewsArticle::getArticleStatus, SfNewsConstants.ARTICLE_PUBLISHED)
            .set(SfNewsArticle::getPublishTime, now)
            .set(SfNewsArticle::getUpdateBy, operatorId)
            .set(SfNewsArticle::getUpdateTime, now));
        insertReviewLog(article.getArticleId(), revision.getRevisionId(), "APPROVE", comment);
    }

    private void reject(SfNewsArticle article, SfNewsRevision revision, String comment) {
        if (StrUtil.isBlank(comment)) {
            throw new ServiceException("驳回时必须填写审核意见");
        }
        Date now = new Date();
        Long operatorId = LoginHelper.getUserId();
        String operatorName = LoginHelper.getUsername();
        revisionMapper.update(null, Wrappers.<SfNewsRevision>lambdaUpdate()
            .eq(SfNewsRevision::getRevisionId, revision.getRevisionId())
            .eq(SfNewsRevision::getReviewStatus, SfNewsConstants.REVIEW_PENDING)
            .set(SfNewsRevision::getReviewStatus, SfNewsConstants.REVIEW_REJECTED)
            .set(SfNewsRevision::getReviewerId, operatorId)
            .set(SfNewsRevision::getReviewerName, operatorName)
            .set(SfNewsRevision::getReviewComment, comment.trim())
            .set(SfNewsRevision::getReviewedAt, now)
            .set(SfNewsRevision::getUpdateBy, operatorId)
            .set(SfNewsRevision::getUpdateTime, now));
        articleMapper.update(null, Wrappers.<SfNewsArticle>lambdaUpdate()
            .eq(SfNewsArticle::getArticleId, article.getArticleId())
            .set(SfNewsArticle::getArticleStatus,
                article.getPublishedRevisionId() == null ? SfNewsConstants.ARTICLE_REJECTED : SfNewsConstants.ARTICLE_PUBLISHED)
            .set(SfNewsArticle::getUpdateBy, operatorId)
            .set(SfNewsArticle::getUpdateTime, now));
        insertReviewLog(article.getArticleId(), revision.getRevisionId(), "REJECT", comment);
    }

    private ISfNewsMediaService.EnqueueResult processClaimed(SfNewsIngestStaging row) {
        validateIngest(row);
        SfNewsSource source = requireSource(row);
        JSONObject payload = JSON.parseObject(row.getPayloadJson());
        SfNewsContentResult content;
        try {
            content = contentSupport.sanitizeAndConvert(row.getExtractedHtml(), row.getOriginUrl());
        } catch (ServiceException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        SfNewsArticle article = articleMapper.selectBySourceAndCanonicalUrl(row.getSourceCode(), row.getCanonicalUrl());
        if (article == null) {
            article = articleMapper.selectBySourceAndExternalKey(row.getSourceCode(), row.getExternalArticleKey());
        }
        boolean created = article == null;
        if (created) {
            article = createArticle(row);
        }
        SfNewsRevision current = article.getCurrentRevisionId() == null
            ? null : revisionMapper.selectRevision(article.getCurrentRevisionId());
        if (!created && row.getContentSha256().equalsIgnoreCase(article.getLatestSourceSha256())) {
            markIngestSuccess(row.getId(), article.getArticleId(),
                current == null ? article.getPublishedRevisionId() : current.getRevisionId(), SfNewsConstants.INGEST_UNCHANGED,
                "UNCHANGED");
            return new ISfNewsMediaService.EnqueueResult(0, 0);
        }
        SfNewsRevision revision = createRevision(article, row, payload, source, content);
        articleMapper.update(null, Wrappers.<SfNewsArticle>lambdaUpdate()
            .eq(SfNewsArticle::getArticleId, article.getArticleId())
            .set(SfNewsArticle::getCurrentRevisionId, revision.getRevisionId())
            .set(SfNewsArticle::getOriginUrl, row.getOriginUrl())
            .set(SfNewsArticle::getCanonicalUrl, row.getCanonicalUrl())
            .set(SfNewsArticle::getExternalArticleKey, row.getExternalArticleKey())
            .set(SfNewsArticle::getLatestSourceSha256, row.getContentSha256().toLowerCase())
            .set(SfNewsArticle::getArticleStatus,
                article.getPublishedRevisionId() == null ? SfNewsConstants.ARTICLE_REVIEWING : article.getArticleStatus())
            .set(SfNewsArticle::getUpdateTime, new Date()));
        markIngestSuccess(row.getId(), article.getArticleId(), revision.getRevisionId(), SfNewsConstants.INGEST_ACCEPTED,
            created ? "CREATED" : "NEW_REVISION");
        return mediaService.reconcileRevision(revision);
    }

    private SfNewsArticle createArticle(SfNewsIngestStaging row) {
        SfNewsArticle article = new SfNewsArticle();
        article.setArticleId(IdWorker.getId());
        article.setSourceCode(row.getSourceCode());
        article.setExternalArticleKey(row.getExternalArticleKey());
        article.setCanonicalUrl(row.getCanonicalUrl());
        article.setOriginUrl(row.getOriginUrl());
        article.setLatestSourceSha256(row.getContentSha256().toLowerCase());
        article.setArticleStatus(SfNewsConstants.ARTICLE_REVIEWING);
        article.setRecommendFlag(false);
        article.setSortOrder(0);
        article.setReadCount(0L);
        article.setDelFlag("0");
        article.setCreateTime(new Date());
        article.setUpdateTime(new Date());
        articleMapper.insert(article);
        return article;
    }

    private SfNewsRevision createRevision(SfNewsArticle article, SfNewsIngestStaging row, JSONObject payload,
                                           SfNewsSource source, SfNewsContentResult content) {
        SfNewsRevision revision = new SfNewsRevision();
        revision.setRevisionId(IdWorker.getId());
        revision.setArticleId(article.getArticleId());
        revision.setRevisionNo(revisionMapper.selectNextRevisionNo(article.getArticleId()));
        revision.setSourceRequestId(row.getRequestId());
        revision.setTitle(payload.getString("title").trim());
        revision.setSummary(StrUtil.trim(payload.getString("summary")));
        revision.setCategory(normalizeCategory(payload.getString("categoryHint")));
        revision.setSourceName(source.getSourceName());
        revision.setAuthor(StrUtil.trim(payload.getString("author")));
        revision.setOriginPublishedAt(parseOriginTime(payload.getString("originPublishedAt")));
        revision.setOriginUrl(row.getOriginUrl());
        revision.setCoverUrl(content.getFirstImageUrl());
        JSONArray tags = payload.getJSONArray("tags");
        revision.setTagsJson(tags == null ? "[]" : tags.toJSONString());
        revision.setSanitizedHtml(content.getSanitizedHtml());
        revision.setContentBlocksJson(content.getContentBlocksJson());
        revision.setContentSha256(row.getContentSha256().toLowerCase());
        revision.setMediaStatus(SfNewsConstants.MEDIA_NONE);
        revision.setMediaTotal(0);
        revision.setMediaSucceeded(0);
        revision.setMediaFailed(0);
        revision.setReviewStatus(SfNewsConstants.REVIEW_PENDING);
        revision.setCreateTime(new Date());
        revision.setUpdateTime(new Date());
        revisionMapper.insert(revision);
        return revision;
    }

    @Override
    public void retryFailedMedia(Long articleId) {
        SfNewsArticle article = requireArticle(articleId);
        SfNewsRevision revision = requireRevision(article.getCurrentRevisionId());
        mediaService.retryFailed(revision.getRevisionId());
    }

    private void wakeMediaAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mediaService.wake();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { mediaService.wake(); }
        });
    }

    private void validateIngest(SfNewsIngestStaging row) {
        if (!"1.0".equals(row.getPayloadSchemaVersion())) {
            throw new IllegalArgumentException("不支持的 payload_schema_version");
        }
        if (StrUtil.isBlank(row.getSourceCode()) || StrUtil.isBlank(row.getCanonicalUrl())) {
            throw new IllegalArgumentException("来源编码和规范URL不能为空");
        }
        JSONObject payload;
        try {
            payload = JSON.parseObject(row.getPayloadJson());
        } catch (Exception e) {
            throw new IllegalArgumentException("payload_json 不是有效JSON", e);
        }
        if (payload == null || StrUtil.isBlank(payload.getString("title"))) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (payload.getString("title").length() > 200) {
            throw new IllegalArgumentException("标题不能超过200个字符");
        }
        if (StrUtil.isBlank(row.getExtractedHtml())) {
            throw new IllegalArgumentException("抽取正文不能为空");
        }
        if (StrUtil.isBlank(row.getContentSha256()) || row.getContentSha256().length() != 64) {
            throw new IllegalArgumentException("content_sha256 格式错误");
        }
        if (!row.getContentSha256().equalsIgnoreCase(DigestUtil.sha256Hex(row.getExtractedHtml()))) {
            throw new IllegalArgumentException("content_sha256 与 extracted_html 不一致");
        }
        if (StrUtil.isBlank(row.getRawHtml()) || StrUtil.isBlank(row.getRawSha256())
            || !row.getRawSha256().equalsIgnoreCase(DigestUtil.sha256Hex(row.getRawHtml()))) {
            throw new IllegalArgumentException("raw_sha256 与 raw_html 不一致");
        }
        if (!CATEGORIES.contains(normalizeCategory(payload.getString("categoryHint")))) {
            throw new IllegalArgumentException("categoryHint 仅支持 policy/knowledge/market/general");
        }
    }

    private SfNewsSource requireSource(SfNewsIngestStaging row) {
        SfNewsSource source = sourceMapper.selectEnabledByCode(row.getSourceCode());
        if (source == null) {
            throw new IllegalArgumentException("source_code 未配置或已停用");
        }
        List<String> allowedDomains = parseTags(source.getAllowedDomainsJson());
        if (allowedDomains.isEmpty()) {
            throw new IllegalArgumentException("来源未配置允许域名");
        }
        validateAllowedUrl(row.getOriginUrl(), allowedDomains, "origin_url");
        validateAllowedUrl(row.getCanonicalUrl(), allowedDomains, "canonical_url");
        return source;
    }

    private void validateAllowedUrl(String url, List<String> allowedDomains, String fieldName) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || StrUtil.isBlank(host)) {
                throw new IllegalArgumentException(fieldName + " 必须是完整 HTTPS 地址");
            }
            String normalizedHost = host.toLowerCase();
            boolean allowed = allowedDomains.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(domain -> normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain));
            if (!allowed) {
                throw new IllegalArgumentException(fieldName + " 不属于来源允许域名");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(fieldName + " 格式错误", e);
        }
    }

    private void markIngestSuccess(Long id, Long articleId, Long revisionId, String status, String action) {
        ingestMapper.update(null, Wrappers.<SfNewsIngestStaging>lambdaUpdate()
            .eq(SfNewsIngestStaging::getId, id)
            .eq(SfNewsIngestStaging::getIngestStatus, SfNewsConstants.INGEST_PROCESSING)
            .set(SfNewsIngestStaging::getIngestStatus, status)
            .set(SfNewsIngestStaging::getResultAction, action)
            .set(SfNewsIngestStaging::getArticleId, articleId)
            .set(SfNewsIngestStaging::getRevisionId, revisionId)
            .set(SfNewsIngestStaging::getErrorCode, null)
            .set(SfNewsIngestStaging::getErrorMessage, null)
            .set(SfNewsIngestStaging::getProcessedAt, new Date()));
    }

    private void markIngestError(Long id, String status, String errorCode, String errorMessage) {
        ingestMapper.update(null, Wrappers.<SfNewsIngestStaging>lambdaUpdate()
            .eq(SfNewsIngestStaging::getId, id)
            .eq(SfNewsIngestStaging::getIngestStatus, SfNewsConstants.INGEST_PROCESSING)
            .set(SfNewsIngestStaging::getIngestStatus, status)
            .set(SfNewsIngestStaging::getErrorCode, errorCode)
            .set(SfNewsIngestStaging::getErrorMessage, StrUtil.sub(errorMessage, 0, 1000))
            .set(SfNewsIngestStaging::getProcessedAt, new Date()));
    }

    private void insertReviewLog(Long articleId, Long revisionId, String action, String comment) {
        SfNewsReviewLog logRow = new SfNewsReviewLog();
        logRow.setLogId(IdWorker.getId());
        logRow.setArticleId(articleId);
        logRow.setRevisionId(revisionId);
        logRow.setAction(action);
        logRow.setOperatorId(LoginHelper.getUserId());
        logRow.setOperatorName(LoginHelper.getUsername());
        logRow.setComment(StrUtil.sub(StrUtil.trim(comment), 0, 500));
        logRow.setCreateTime(new Date());
        reviewLogMapper.insert(logRow);
    }

    private SfNewsArticle requireArticle(Long articleId) {
        SfNewsArticle article = articleMapper.selectOne(Wrappers.<SfNewsArticle>lambdaQuery()
            .eq(SfNewsArticle::getArticleId, articleId)
            .eq(SfNewsArticle::getDelFlag, "0"));
        if (article == null) {
            throw new ServiceException("农业资讯不存在");
        }
        return article;
    }

    private SfNewsRevision requireRevision(Long revisionId) {
        if (revisionId == null) {
            throw new ServiceException("文章版本不存在");
        }
        SfNewsRevision revision = revisionMapper.selectRevision(revisionId);
        if (revision == null) {
            throw new ServiceException("文章版本不存在");
        }
        return revision;
    }

    private SfNewsRevision requireCurrentRevision(SfNewsArticle article, Long revisionId) {
        if (article.getCurrentRevisionId() == null || !article.getCurrentRevisionId().equals(revisionId)) {
            throw new ServiceException("文章已产生新版本，请刷新后再操作");
        }
        SfNewsRevision revision = requireRevision(revisionId);
        if (!article.getArticleId().equals(revision.getArticleId())) {
            throw new ServiceException("版本与文章不匹配");
        }
        return revision;
    }

    private void enrichMobileListItem(SfNewsMobileListVo item) {
        item.setCategoryName(categoryName(item.getCategory()));
        item.setTags(parseTags(item.getTagsJson()));
        item.setHasVideo(StrUtil.contains(item.getContentBlocksJson(), "\"type\":\"video\""));
    }

    private List<String> parseTags(String json) {
        return StrUtil.isBlank(json) ? List.of() : JSON.parseArray(json, String.class);
    }

    private List<Object> parseBlocks(String json) {
        return StrUtil.isBlank(json) ? List.of() : JSON.parseArray(json, Object.class);
    }

    private String normalizeCategory(String category) {
        return StrUtil.blankToDefault(category, SfNewsConstants.CATEGORY_GENERAL).trim().toLowerCase();
    }

    private void validateCategory(String category) {
        if (!CATEGORIES.contains(category)) {
            throw new ServiceException("分类仅支持 policy/knowledge/market/general");
        }
    }

    private Date parseOriginTime(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Date.from(OffsetDateTime.parse(text).toInstant());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("originPublishedAt 必须是带时区的 ISO-8601 时间", e);
        }
    }

    private String categoryName(String category) {
        return switch (category) {
            case SfNewsConstants.CATEGORY_POLICY -> "政策";
            case SfNewsConstants.CATEGORY_KNOWLEDGE -> "农技知识";
            case SfNewsConstants.CATEGORY_MARKET -> "行情";
            default -> "综合";
        };
    }
}
