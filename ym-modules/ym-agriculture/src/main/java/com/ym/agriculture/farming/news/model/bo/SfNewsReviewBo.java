package com.ym.agriculture.farming.news.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 农业资讯人工审核入参。 */
@Data
public class SfNewsReviewBo {

    /** 待审核版本主键。 */
    @NotNull(message = "版本ID不能为空")
    private Long revisionId;
    /** 审核动作：APPROVE 或 REJECT。 */
    @NotBlank(message = "审核动作不能为空")
    private String action;
    /** 审核意见；驳回时必填。 */
    @Size(max = 500, message = "审核意见不能超过500个字符")
    private String comment;
}
