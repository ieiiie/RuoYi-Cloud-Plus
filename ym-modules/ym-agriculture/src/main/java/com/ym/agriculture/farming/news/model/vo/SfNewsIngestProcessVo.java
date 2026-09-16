package com.ym.agriculture.farming.news.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 一批采集暂存记录的处理统计。 */
@Data
@AllArgsConstructor
public class SfNewsIngestProcessVo {

    /** 领取条数。 */
    private int claimed;
    /** 成功生成待审版本或判定未变化的条数。 */
    private int succeeded;
    /** 永久校验失败条数。 */
    private int rejected;
    /** 平台内部处理失败条数。 */
    private int failed;
    /** 入队等待自动转存的媒体数。 */
    private int mediaQueued;
    /** 因超过单篇上限而跳过的媒体数。 */
    private int mediaSkipped;
}
