package com.ym.agriculture.farming.news.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.news.model.bo.SfNewsQueryBo;
import com.ym.agriculture.farming.news.model.bo.SfNewsReviewBo;
import com.ym.agriculture.farming.news.model.bo.SfNewsRevisionEditBo;
import com.ym.agriculture.farming.news.model.vo.SfNewsAdminDetailVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsAdminListVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsDashboardVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsIngestProcessVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsMobileDetailVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsMobileListVo;

/** 农业资讯采集、审核、发布与移动端查询服务。 */
public interface ISfNewsService {

    /** 查询管理端审核分页。 */
    PageResult<SfNewsAdminListVo> queryAdminPage(SfNewsQueryBo bo, PageQuery pageQuery);

    /** 查询管理端当前待审版本详情。 */
    SfNewsAdminDetailVo getAdminDetail(Long articleId);

    /** 编辑当前版本并重新生成安全内容块；已通过版本由具备编辑和审核权限的审核员直接发布。 */
    void editRevision(Long articleId, SfNewsRevisionEditBo bo);

    /** 人工审核；通过后立即发布，驳回时保留原线上版本。 */
    void review(Long articleId, SfNewsReviewBo bo);

    /** 下架当前线上文章。 */
    void offline(Long articleId, String comment);

    /** 处理一批爬虫暂存记录。 */
    SfNewsIngestProcessVo processPending(int limit);

    void retryFailedMedia(Long articleId);

    /** 查询移动端已发布资讯分页。 */
    PageResult<SfNewsMobileListVo> queryMobilePage(String category, String keyword, PageQuery pageQuery);

    /** 查询移动端已发布资讯详情并增加阅读数。 */
    SfNewsMobileDetailVo getMobileDetail(Long articleId);

    /** 查询移动端资讯聚合数据。 */
    SfNewsDashboardVo getDashboard();
}
