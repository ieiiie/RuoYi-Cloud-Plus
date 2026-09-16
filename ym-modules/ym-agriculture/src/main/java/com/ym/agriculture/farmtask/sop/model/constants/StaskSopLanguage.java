package com.ym.agriculture.farmtask.sop.model.constants;

import java.util.List;

/** 农事 SOP 支持的内容语言。 */
public interface StaskSopLanguage {

    /** 简体中文。 */
    String ZH_CN = "zh-CN";

    /** 维吾尔文。 */
    String UG_CN = "ug-CN";

    /** 全部支持语言。 */
    List<String> ALL = List.of(ZH_CN, UG_CN);
}
