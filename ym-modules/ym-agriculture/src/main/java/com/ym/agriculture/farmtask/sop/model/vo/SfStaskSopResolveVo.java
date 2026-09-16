package com.ym.agriculture.farmtask.sop.model.vo;

import lombok.Data;

/** 小程序农事 SOP 解析结果。 */
@Data
public class SfStaskSopResolveVo {

    /** 当前任务状态是否允许查看 SOP。 */
    private Boolean eligible;

    /** 是否匹配到 SOP。 */
    private Boolean matched;

    /** 客户端请求语言。 */
    private String requestedLanguage;

    /** 实际命中的内容语言。 */
    private String contentLanguage;

    /** 是否使用另一种语言作为回退。 */
    private Boolean languageFallback;

    /** 命中后的有序内容块；不满足查看条件或未命中时为空。 */
    private java.util.List<SfStaskSopContentBlockVo> contentBlocks;
}
