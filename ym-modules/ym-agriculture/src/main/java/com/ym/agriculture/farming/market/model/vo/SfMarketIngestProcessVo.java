package com.ym.agriculture.farming.market.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 农业行情采集队列处理统计。 */
@Data
@AllArgsConstructor
public class SfMarketIngestProcessVo {
    /** 成功领取数量。 */
    private int claimed;
    /** 创建正式报价数量。 */
    private int created;
    /** 更新正式报价数量。 */
    private int updated;
    /** 未发生变化数量。 */
    private int unchanged;
    /** 业务校验拒绝数量。 */
    private int rejected;
    /** 平台处理失败数量。 */
    private int failed;
    /** 产生告警的报价数量。 */
    private int warnings;
}
