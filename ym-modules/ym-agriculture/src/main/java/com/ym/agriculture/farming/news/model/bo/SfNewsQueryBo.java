package com.ym.agriculture.farming.news.model.bo;

import lombok.Data;

/** 管理端农业资讯分页查询条件。 */
@Data
public class SfNewsQueryBo {

    /** 标题、摘要、来源或标签关键词。 */
    private String keyword;
    /** 内容分类：policy/knowledge/market/general。 */
    private String category;
    /** 文章状态：REVIEWING/REJECTED/PUBLISHED/OFFLINE。 */
    private String articleStatus;
    /** 审核状态：PENDING/APPROVED/REJECTED。 */
    private String reviewStatus;
    /** 是否推荐。 */
    private Boolean recommendFlag;
}
