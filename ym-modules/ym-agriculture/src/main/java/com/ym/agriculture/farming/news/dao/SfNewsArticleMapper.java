package com.ym.agriculture.farming.news.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.news.model.bo.SfNewsQueryBo;
import com.ym.agriculture.farming.news.model.entity.SfNewsArticle;
import com.ym.agriculture.farming.news.model.vo.SfNewsAdminListVo;
import com.ym.agriculture.farming.news.model.vo.SfNewsMobileListVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 平台全局农业资讯文章 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfNewsArticleMapper extends BaseMapperPlus<SfNewsArticle, SfNewsArticle> {

    /** 查询管理端审核分页。 */
    @Select({
        "<script>",
        "SELECT a.article_id, r.revision_id, r.revision_no, r.title, r.summary, r.category,",
        "r.source_name, a.article_status, r.review_status, a.recommend_flag,",
        "r.origin_published_at, a.publish_time, r.create_time, r.media_status, r.media_total, r.media_succeeded, r.media_failed",
        "FROM sf_news_article a JOIN sf_news_revision r ON r.revision_id = a.current_revision_id",
        "WHERE a.del_flag = '0'",
        "<if test='bo.articleStatus != null and bo.articleStatus != \"\"'> AND a.article_status = #{bo.articleStatus}</if>",
        "<if test='bo.reviewStatus != null and bo.reviewStatus != \"\"'> AND r.review_status = #{bo.reviewStatus}</if>",
        "<if test='bo.category != null and bo.category != \"\"'> AND r.category = #{bo.category}</if>",
        "<if test='bo.recommendFlag != null'> AND a.recommend_flag = #{bo.recommendFlag}</if>",
        "<if test='bo.keyword != null and bo.keyword != \"\"'>",
        "AND (r.title LIKE CONCAT('%', #{bo.keyword}, '%') OR r.summary LIKE CONCAT('%', #{bo.keyword}, '%')",
        "OR r.source_name LIKE CONCAT('%', #{bo.keyword}, '%') OR CAST(r.tags_json AS CHAR) LIKE CONCAT('%', #{bo.keyword}, '%'))",
        "</if>",
        "ORDER BY CASE WHEN r.review_status = 'PENDING' THEN 0 ELSE 1 END, r.create_time DESC",
        "</script>"
    })
    Page<SfNewsAdminListVo> selectAdminPage(Page<SfNewsAdminListVo> page, @Param("bo") SfNewsQueryBo bo);

    /** 查询移动端已发布资讯分页。 */
    @Select({
        "<script>",
        "SELECT a.article_id, r.title, r.summary, r.category, r.source_name AS source,",
        "a.publish_time, r.cover_url, a.read_count, r.tags_json, r.content_blocks_json",
        "FROM sf_news_article a JOIN sf_news_revision r ON r.revision_id = a.published_revision_id",
        "WHERE a.del_flag = '0' AND a.article_status = 'PUBLISHED' AND r.review_status = 'APPROVED'",
        "<if test='category == \"recommend\"'> AND a.recommend_flag = 1</if>",
        "<if test='category != null and category != \"\" and category != \"recommend\"'> AND r.category = #{category}</if>",
        "<if test='keyword != null and keyword != \"\"'>",
        "AND (r.title LIKE CONCAT('%', #{keyword}, '%') OR r.summary LIKE CONCAT('%', #{keyword}, '%')",
        "OR r.source_name LIKE CONCAT('%', #{keyword}, '%') OR CAST(r.tags_json AS CHAR) LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "ORDER BY a.recommend_flag DESC, a.sort_order ASC, a.publish_time DESC",
        "</script>"
    })
    Page<SfNewsMobileListVo> selectMobilePage(Page<SfNewsMobileListVo> page,
        @Param("category") String category, @Param("keyword") String keyword);

    /** 按来源规范地址查询文章身份。 */
    default SfNewsArticle selectBySourceAndCanonicalUrl(String sourceCode, String canonicalUrl) {
        return selectOne(Wrappers.<SfNewsArticle>lambdaQuery()
            .eq(SfNewsArticle::getSourceCode, sourceCode)
            .eq(SfNewsArticle::getCanonicalUrl, canonicalUrl)
            .eq(SfNewsArticle::getDelFlag, "0")
            .last("limit 1"));
    }

    /** 规范地址发生变化时，按来源内稳定标识兜底查询。 */
    default SfNewsArticle selectBySourceAndExternalKey(String sourceCode, String externalArticleKey) {
        return selectOne(Wrappers.<SfNewsArticle>lambdaQuery()
            .eq(SfNewsArticle::getSourceCode, sourceCode)
            .eq(SfNewsArticle::getExternalArticleKey, externalArticleKey)
            .eq(SfNewsArticle::getDelFlag, "0")
            .last("limit 1"));
    }

    /** 原子增加阅读数。 */
    @Update("UPDATE sf_news_article SET read_count = read_count + 1 WHERE article_id = #{articleId} AND article_status = 'PUBLISHED' AND del_flag = '0'")
    int incrementReadCount(@Param("articleId") Long articleId);
}
