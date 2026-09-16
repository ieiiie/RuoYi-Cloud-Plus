package com.ym.agriculture.farming.farmrecord.model.constant;

import com.ym.common.core.utils.StringUtils;

/**
 * 农事媒体种类（{@code sf_farming_record_media.kind}）。
 *
 * @author ym-cloud
 */
public final class FarmingMediaKind {

    private FarmingMediaKind() {
    }

    /** 田间拍摄图片等媒体 */
    public static final String IMAGE = "IMAGE";
    /** 视频类附件（无人机/手机上摄） */
    public static final String VIDEO = "VIDEO";
    /** 田间语音或多媒体录音附件 */
    public static final String AUDIO = "AUDIO";

    public static boolean isValid(String raw) {
        if (StringUtils.isBlank(raw)) {
            return false;
        }
        String k = raw.trim().toUpperCase();
        return IMAGE.equals(k) || VIDEO.equals(k) || AUDIO.equals(k);
    }
}
