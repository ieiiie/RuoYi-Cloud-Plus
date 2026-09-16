package com.ym.agriculture.farmtask.sop.model.vo;

import lombok.Data;

/** 小程序和后台共用的 SOP 内容块。 */
@Data
public class SfStaskSopContentBlockVo {

    /** 内容块类型：RICH_TEXT/VIDEO。 */
    private String type;

    /** 富文本 HTML，仅 RICH_TEXT 使用。 */
    private String html;

    /** OSS 文件 ID，仅 VIDEO 使用。 */
    private String assetId;

    /** 视频播放 URL，仅 VIDEO 使用。 */
    private String url;

    /** 视频封面 URL。 */
    private String coverUrl;

    /** 视频标题。 */
    private String title;
}
