package com.ym.agriculture.farming.farmrecord.controller;

import com.ym.common.core.domain.R;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordSaveBo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingGrowthStagePreviewVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordVo;
import com.ym.agriculture.farming.farmrecord.service.ISfFarmingRecordService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 管理后台农事记录接口。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/farming/farm-records")
public class SfFarmingRecordController extends BaseController {

    private final ISfFarmingRecordService recordService;

    /**
     * 分页查询已提交农事记录。
     *
     * @param fieldId    地块 ID，可选
     * @param workItemId 农事项目 ID，可选
     * @param categoryId 农事分类 ID，可选
     * @param from       农事发生时间开始，可选，格式：yyyy-MM-dd HH:mm:ss
     * @param to         农事发生时间结束，可选，格式：yyyy-MM-dd HH:mm:ss
     * @param pageQuery  分页参数
     * @return 已提交农事记录分页
     */
    @GetMapping("/page")
    public R<PageResult<SfFarmingRecordVo>> page(@RequestParam(required = false) Long fieldId,
                                                 @RequestParam(required = false) Long workItemId,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date from,
                                                 @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date to,
                                                 PageQuery pageQuery) {
        return R.ok(recordService.pageSubmittedForAdmin(fieldId, workItemId, categoryId, from, to, pageQuery));
    }

    /**
     * 预览农事发生日期对应天气。
     *
     * @param happenedAt 农事发生时间，格式：yyyy-MM-dd HH:mm:ss
     * @return 天气快照预览
     */
    @GetMapping("/preview/weather")
    public R<Map<String, Object>> previewWeather(@NotNull @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date happenedAt) {
        return R.ok(recordService.buildWeatherPreview(happenedAt));
    }

    /**
     * 预览生长阶段。
     *
     * @param fieldIds   地块 ID 列表，支持逗号或重复参数
     * @param happenedAt 农事发生时间，格式：yyyy-MM-dd HH:mm:ss
     * @return 生长阶段预览
     */
    @GetMapping("/preview/growth-stage")
    public R<SfFarmingGrowthStagePreviewVo> previewGrowthStage(@NotEmpty @RequestParam List<Long> fieldIds,
                                                               @NotNull @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date happenedAt) {
        return R.ok(recordService.buildGrowthStagePreview(fieldIds, happenedAt));
    }

    /**
     * 查询已提交农事记录详情。
     *
     * <p>响应 {@code data.fields[]} 除地块快照外，还会按各地块解析种植批次并返回作物信息：
     * {@code plantingBatchId}、{@code varietyId}、{@code varietyName}、{@code speciesId}、
     * {@code speciesName}、{@code speciesImageUrl}。优先匹配记录内 {@code sowingDateSnapshot} 对应批次，
     * 否则回退为地块当前进行中批次；无批次时上述字段为空。
     *
     * @param recordId 农事记录主键
     * @return 已提交农事记录详情
     */
    @GetMapping("/{recordId}")
    public R<SfFarmingRecordVo> get(@NotNull @PathVariable Long recordId) {
        return R.ok(recordService.getSubmittedDetailForAdmin(recordId));
    }

    /**
     * 新增记录。
     *
     * @param bo 保存入参
     * @return 新建记录 ID
     */
    @Log(title = "农事记录", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SfFarmingRecordSaveBo bo) {
        return R.ok(recordService.addSubmittedForAdmin(bo));
    }

    /**
     * 编辑已提交农事记录。
     *
     * @param recordId 记录 ID
     * @param bo       保存入参
     * @return 操作结果
     */
    @Log(title = "农事记录", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{recordId}")
    public R<Void> edit(@NotNull @PathVariable Long recordId,
                        @Validated(EditGroup.class) @RequestBody SfFarmingRecordSaveBo bo) {
        bo.setRecordId(recordId);
        return toAjax(recordService.updateSubmittedForAdmin(recordId, bo));
    }

    /**
     * 删除当前登录人创建的已提交农事记录。
     *
     * @param recordId 记录 ID
     * @return 操作结果
     */
    @Log(title = "农事记录", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @DeleteMapping("/{recordId}")
    public R<Void> remove(@NotNull @PathVariable Long recordId) {
        return toAjax(recordService.removeSubmittedForAdmin(recordId));
    }
}
