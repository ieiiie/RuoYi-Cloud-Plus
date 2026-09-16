package com.ym.agriculture.farmtask.sop.model.constants;

import java.util.Set;

/** 农事 SOP 作物范围常量。 */
public interface StaskSopCropScope {

    /** 全部作物。 */
    String ALL = "ALL";

    /** 指定作物物种。 */
    String SPECIFIC = "SPECIFIC";

    /** 支持的作物范围。 */
    Set<String> SUPPORTED = Set.of(ALL, SPECIFIC);
}
