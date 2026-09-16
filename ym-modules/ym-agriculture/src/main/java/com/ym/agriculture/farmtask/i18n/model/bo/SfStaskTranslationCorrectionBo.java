package com.ym.agriculture.farmtask.i18n.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** stask 人工维文校对请求。 */
@Data
public class SfStaskTranslationCorrectionBo {

    /** 客户端读取记录时的中文原文 SHA-256。 */
    @NotBlank(message = "{" + StaskMessageKeys.TRANSLATION_SOURCE_HASH_INVALID + "}")
    @Pattern(regexp = "^[0-9a-fA-F]{64}$",
        message = "{" + StaskMessageKeys.TRANSLATION_SOURCE_HASH_INVALID + "}")
    private String sourceHash;

    /** 维吾尔文译文。 */
    @NotBlank(message = "{" + StaskMessageKeys.TRANSLATION_TRANSLATED_TEXT_REQUIRED + "}")
    @Size(max = 10000, message = "{" + StaskMessageKeys.TRANSLATION_TRANSLATED_TEXT_MAX + "}")
    private String translatedText;
}
