package com.ym.agriculture.farmtask.inspection.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** 抽检详情。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfStaskInspectionDetailVo extends SfStaskInspectionListVo {

    /** 问题照片展示 JSON 数组。 */
    private String problemPhotoJson;
    /** 创建人员工 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorEmployeeId;
    /** 创建人姓名快照。 */
    private String creatorNameSnapshot;

    /** 首次处理信息；无处理历史时为 null。 */
    private HandleVo firstHandle;
    /** 当前最新处理信息；无处理历史时为 null。 */
    private HandleVo currentHandle;
    /** 最新打回信息；从未打回时为 null。 */
    private RejectVo latestReject;
    /** 成功处理次数。 */
    private Integer handleCount;
    /** 成功打回次数。 */
    private Integer rejectCount;
    /** 是否存在处理历史。 */
    private Boolean hasHandleHistory;

    @Data
    public static class HandleVo {
        /** 处理说明。 */
        private String description;
        /** 处理照片展示 JSON 数组。 */
        private String photoJson;
        /** 处理人员工 ID。 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long employeeId;
        /** 处理人姓名快照。 */
        private String employeeNameSnapshot;
        /** 处理时间。 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date handledAt;
    }

    @Data
    public static class RejectVo {
        /** 打回原因。 */
        private String reason;
        /** 打回人员工 ID。 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long employeeId;
        /** 打回人姓名快照。 */
        private String employeeNameSnapshot;
        /** 打回时间。 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date rejectedAt;
    }
}
