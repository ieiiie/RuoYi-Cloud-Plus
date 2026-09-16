package com.ym.agriculture.farming.news.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.news.model.bo.SfNewsIngestProcessBo;
import com.ym.agriculture.farming.news.model.bo.SfNewsOfflineBo;
import com.ym.agriculture.farming.news.model.bo.SfNewsQueryBo;
import com.ym.agriculture.farming.news.model.bo.SfNewsReviewBo;
import com.ym.agriculture.farming.news.model.bo.SfNewsRevisionEditBo;
import com.ym.agriculture.farming.news.model.vo.SfNewsAdminDetailVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsAdminListVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsIngestProcessVo;
import com.ym.agriculture.farming.news.service.ISfNewsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端农业资讯采集审核与发布接口。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/news")
public class SfNewsController extends BaseController {

    /** 农业资讯领域服务。 */
    private final ISfNewsService newsService;

    /**
     * 查询农业资讯审核分页。
     *
     * @param bo 查询条件
     * @param pageQuery 分页条件
     * @return 审核分页
     */
    @SaCheckPermission("smartfarming:news:list")
    @GetMapping("/page")
    public R<PageResult<SfNewsAdminListVo>> page(SfNewsQueryBo bo, PageQuery pageQuery) {
        return R.ok(newsService.queryAdminPage(bo, pageQuery));
    }

    /**
     * 查询文章当前待审版本详情。
     *
     * @param articleId 文章主键
     * @return 审稿详情
     */
    @SaCheckPermission("smartfarming:news:query")
    @GetMapping("/{articleId}")
    public R<SfNewsAdminDetailVo> detail(@NotNull @PathVariable Long articleId) {
        return R.ok(newsService.getAdminDetail(articleId));
    }

    /**
     * 保存审核人员对内容和格式的修改；已通过版本由同时具备编辑和审核权限的审核员直接发布。
     *
     * @param articleId 文章主键
     * @param bo 版本编辑内容
     * @return 保存结果
     */
    @SaCheckPermission("smartfarming:news:edit")
    @Log(title = "农业资讯编辑", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/{articleId}/revision")
    public R<Void> edit(@NotNull @PathVariable Long articleId,
                        @Valid @RequestBody SfNewsRevisionEditBo bo) {
        newsService.editRevision(articleId, bo);
        return R.ok();
    }

    /**
     * 人工审核当前版本；通过后立即发布，驳回后移动端不可见该版本。
     *
     * @param articleId 文章主键
     * @param bo 审核动作和意见
     * @return 审核结果
     */
    @SaCheckPermission("smartfarming:news:review")
    @Log(title = "农业资讯审核", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/{articleId}/review")
    public R<Void> review(@NotNull @PathVariable Long articleId,
                          @Valid @RequestBody SfNewsReviewBo bo) {
        newsService.review(articleId, bo);
        return R.ok();
    }

    /**
     * 下架已发布文章。
     *
     * @param articleId 文章主键
     * @param bo 下架原因
     * @return 下架结果
     */
    @SaCheckPermission("smartfarming:news:offline")
    @Log(title = "农业资讯下架", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/{articleId}/offline")
    public R<Void> offline(@NotNull @PathVariable Long articleId,
                           @Valid @RequestBody SfNewsOfflineBo bo) {
        newsService.offline(articleId, bo.getComment());
        return R.ok();
    }

    /**
     * 手动处理一批爬虫暂存记录，通常用于联调或补偿。
     *
     * @param bo 批处理数量
     * @return 处理统计
     */
    @SaCheckPermission("smartfarming:news:process")
    @Log(title = "农业资讯采集处理", businessType = BusinessType.OTHER)
    @RepeatSubmit()
    @PostMapping("/ingest/process")
    public R<SfNewsIngestProcessVo> processIngest(@Valid @RequestBody SfNewsIngestProcessBo bo) {
        return R.ok(newsService.processPending(bo.getLimit()));
    }

    @SaCheckPermission("smartfarming:news:process")
    @Log(title = "农业资讯媒体重新转存", businessType = BusinessType.OTHER)
    @RepeatSubmit()
    @PostMapping("/{articleId}/media/retry")
    public R<Void> retryMedia(@NotNull @PathVariable Long articleId) {
        newsService.retryFailedMedia(articleId);
        return R.ok();
    }
}
