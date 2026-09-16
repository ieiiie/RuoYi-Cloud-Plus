package com.ym.agriculture.farming.news.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 农业资讯审核与发布操作日志。 */
@Data
@TableName("sf_news_review_log")
public class SfNewsReviewLog {

    /** 日志主键。 */
    @TableId("log_id")
    private Long logId;
    /** 文章主键。 */
    private Long articleId;
    /** 版本主键，可为空。 */
    private Long revisionId;
    /** 操作：EDIT/EDIT_PUBLISHED/APPROVE/REJECT/OFFLINE。 */
    private String action;
    /** 操作人 ID。 */
    private Long operatorId;
    /** 操作人名称快照。 */
    private String operatorName;
    /** 操作说明。 */
    private String comment;
    /** 操作时间。 */
    private Date createTime;
}
