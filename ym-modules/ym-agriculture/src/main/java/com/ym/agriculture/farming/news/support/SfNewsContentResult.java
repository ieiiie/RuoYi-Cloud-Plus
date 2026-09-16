package com.ym.agriculture.farming.news.support;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 农业资讯正文白名单清洗和结构化转换结果。 */
@Data
@AllArgsConstructor
public class SfNewsContentResult {

    /** 白名单清洗后的 HTML。 */
    private String sanitizedHtml;
    /** 移动端 contentBlocks JSON 数组。 */
    private String contentBlocksJson;
    /** 正文首图地址，可作为默认封面。 */
    private String firstImageUrl;
}
