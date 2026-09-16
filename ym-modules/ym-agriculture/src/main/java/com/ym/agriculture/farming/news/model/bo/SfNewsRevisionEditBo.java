package com.ym.agriculture.farming.news.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 审核人员编辑待审农业资讯版本的入参。 */
@Data
public class SfNewsRevisionEditBo {

    /** 待编辑版本主键，用于防止编辑了旧版本。 */
    @NotNull(message = "版本ID不能为空")
    private Long revisionId;
    /** 标题。 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;
    /** 摘要。 */
    @Size(max = 500, message = "摘要不能超过500个字符")
    private String summary;
    /** 分类：policy/knowledge/market/general。 */
    @NotBlank(message = "分类不能为空")
    private String category;
    /** 展示来源。 */
    @NotBlank(message = "来源不能为空")
    @Size(max = 100, message = "来源不能超过100个字符")
    private String sourceName;
    /** 作者。 */
    @Size(max = 100, message = "作者不能超过100个字符")
    private String author;
    /** 标签，最多10项，每项不超过32个字符。 */
    @Size(max = 10, message = "标签不能超过10个")
    private List<@Size(max = 32, message = "单个标签不能超过32个字符") String> tags;
    /** 审核人员编辑后的正文 HTML。 */
    @NotBlank(message = "正文不能为空")
    @Size(max = 2 * 1024 * 1024, message = "正文不能超过2MiB")
    private String sanitizedHtml;
    /** 是否推荐。 */
    private Boolean recommendFlag;
    /** 推荐排序，升序。 */
    private Integer sortOrder;
}
