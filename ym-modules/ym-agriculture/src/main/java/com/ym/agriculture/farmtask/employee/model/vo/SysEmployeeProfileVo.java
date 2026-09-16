package com.ym.agriculture.farmtask.employee.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 人员档案详情视图对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysEmployeeProfileVo extends SysEmployeeVo {

    /**
     * 人员档案是否可编辑。
     */
    private Boolean editable;

    /**
     * 档案只读原因。
     */
    private String readonlyReason;

    /**
     * 只读字段名列表，供前端灰色展示。
     */
    private List<String> readonlyFields;
}
