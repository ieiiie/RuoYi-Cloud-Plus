package com.ym.agriculture.farming.news.model.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;

/** 移动端已发布农业资讯列表项。 */
@Data
public class SfNewsMobileListVo {

    /** 文章主键。 */
    private Long articleId;
    /** 标题。 */
    private String title;
    /** 摘要。 */
    private String summary;
    /** 分类。 */
    private String category;
    /** 分类名称。 */
    private String categoryName;
    /** 来源名称。 */
    private String source;
    /** 平台发布时间。 */
    private Date publishTime;
    /** 封面地址。 */
    private String coverUrl;
    /** 正文是否含视频。 */
    private Boolean hasVideo;
    /** 标签。 */
    private List<String> tags;
    /** 阅读次数。 */
    private Long readCount;
    /** 数据库标签 JSON，仅供服务层转换。 */
    @JsonIgnore
    private String tagsJson;
    /** 数据库内容块 JSON，仅供服务层判断视频。 */
    @JsonIgnore
    private String contentBlocksJson;
}
