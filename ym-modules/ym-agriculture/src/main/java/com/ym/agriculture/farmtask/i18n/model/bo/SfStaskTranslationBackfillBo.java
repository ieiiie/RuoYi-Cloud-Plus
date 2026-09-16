package com.ym.agriculture.farmtask.i18n.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * stask 翻译历史回填入参。
 */
@Data
public class SfStaskTranslationBackfillBo {

    /**
     * 指定租户；不传时使用当前租户。
     */
    @Size(max = 20, message = "{" + StaskMessageKeys.TRANSLATION_TENANT_ID_MAX + "}")
    private String tenantId;

    /**
     * 要回填的资源类型；空集合表示本期全部资源。
     */
    @Size(max = 32, message = "{" + StaskMessageKeys.TRANSLATION_RESOURCE_TYPES_MAX + "}")
    private List<String> resourceTypes;
}
