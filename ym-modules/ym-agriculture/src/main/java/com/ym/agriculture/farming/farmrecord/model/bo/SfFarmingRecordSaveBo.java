package com.ym.agriculture.farming.farmrecord.model.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动端农事记录保存入参。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmingRecordSaveBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农事记录主键，编辑时由路径参数回填；新增时为空。
     */
    private Long recordId;

    /**
     * 地块 ID 列表，至少选择一个地块。
     */
    @NotEmpty(message = "地块不能为空", groups = {AddGroup.class, EditGroup.class})
    private List<@NotNull(message = "地块 ID 不能为空") Long> fieldIds = new ArrayList<>();

    /**
     * 农事项目列表，保存分类和项目快照；草稿保存时非必填，提交/更新已提交记录时至少选择一个。
     */
    @Valid
    private List<SfFarmingRecordWorkItemBo> workItems = new ArrayList<>();

    /**
     * 农事发生时间，格式：yyyy-MM-dd HH:mm:ss。
     */
    @NotNull(message = "发生时间不能为空", groups = {AddGroup.class, EditGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date happenedAt;

    /**
     * 作业时段，数组格式：[开始时间, 结束时间]，精确到分钟，允许跨 1 天。
     */
    private List<String> workPeriod = new ArrayList<>();

    /**
     * 今日小结（农事总结），选填，最大 500 字。
     */
    @Size(max = 500, message = "今日小结长度不能超过 500")
    private String summary;

    /**
     * 天气快照，非必填；传入时按用户手填保存，未传时服务端尝试按发生日期自动带出。
     */
    private Map<String, Object> weather = new LinkedHashMap<>();

    /**
     * 生长阶段快照；名称由服务端固定映射补齐，不依赖系统字典。
     */
    private Map<String, Object> growthStage = new LinkedHashMap<>();

    /**
     * 资源投入快照，包含 labor、machines、materials。
     */
    private Map<String, Object> resource = new LinkedHashMap<>();

    /**
     * 现场反馈快照，自由文本或对象透传，不做系统字典回填。
     */
    private Map<String, Object> feedback = new LinkedHashMap<>();

    /**
     * 环境摘要快照，保存土壤湿度、温度、降雨量、采集时间等。
     */
    private Map<String, Object> environmentSummary = new LinkedHashMap<>();

    /**
     * 传感器快照 JSON；为空时服务端按 fieldIds 重新生成。
     */
    private String sensorSnapshotJson;

    /**
     * 备注，最大 500 字。
     */
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;

    /**
     * 媒体全量列表；不传或空列表表示清空附件。
     */
    @Valid
    private List<SfFarmingRecordMediaItemBo> media = new ArrayList<>();
}
