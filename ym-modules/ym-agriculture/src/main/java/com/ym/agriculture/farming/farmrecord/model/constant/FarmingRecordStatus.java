package com.ym.agriculture.farming.farmrecord.model.constant;

/**
 * 农事记录状态常量（与 {@code sf_farming_record.status} 一致）。
 *
 * @author ym-cloud
 */
public final class FarmingRecordStatus {

    private FarmingRecordStatus() {
    }

    /** 草稿：仅创建者可见且可编辑。 */
    public static final String DRAFT = "DRAFT";

    /** 已提交：参与历史 Feed 与地块内全员可见列表。 */
    public static final String SUBMITTED = "SUBMITTED";
}
