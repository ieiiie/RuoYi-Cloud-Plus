package com.ym.agriculture.farming.news.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 平台维护的农业资讯来源白名单。 */
@Data
@TableName("sf_news_source")
public class SfNewsSource implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 来源主键。 */
    @TableId("source_id")
    private Long sourceId;
    /** 分配给爬虫的来源编码。 */
    private String sourceCode;
    /** 移动端展示来源。 */
    private String sourceName;
    /** 允许采集的域名 JSON 数组。 */
    private String allowedDomainsJson;
    /** 授权、robots 或开放数据依据说明。 */
    private String authorizationNote;
    /** 是否允许入库。 */
    private Boolean enabledFlag;
    /** 创建人。 */
    private Long createBy;
    /** 创建时间。 */
    private Date createTime;
    /** 更新人。 */
    private Long updateBy;
    /** 更新时间。 */
    private Date updateTime;
}
