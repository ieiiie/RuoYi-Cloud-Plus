package com.ym.agriculture.farming.news.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.news.model.entity.SfNewsRevision;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 平台全局农业资讯版本 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfNewsRevisionMapper extends BaseMapperPlus<SfNewsRevision, SfNewsRevision> {

    /** 查询文章的下一个版本号。 */
    @Select("SELECT COALESCE(MAX(revision_no), 0) + 1 FROM sf_news_revision WHERE article_id = #{articleId}")
    int selectNextRevisionNo(Long articleId);

    /** 查询版本主键。 */
    default SfNewsRevision selectRevision(Long revisionId) {
        return selectOne(Wrappers.<SfNewsRevision>lambdaQuery()
            .eq(SfNewsRevision::getRevisionId, revisionId));
    }

    @Update("UPDATE sf_news_revision SET sanitized_html=#{newHtml}, content_blocks_json=#{blocksJson}, " +
        "cover_url=#{coverUrl}, content_sha256=#{contentSha256}, update_time=NOW() " +
        "WHERE revision_id=#{revisionId} AND sanitized_html=#{oldHtml} AND review_status IN ('PENDING','REJECTED')")
    int updateMediaHtmlCas(@Param("revisionId") Long revisionId, @Param("oldHtml") String oldHtml,
        @Param("newHtml") String newHtml, @Param("blocksJson") String blocksJson,
        @Param("coverUrl") String coverUrl, @Param("contentSha256") String contentSha256);
}
