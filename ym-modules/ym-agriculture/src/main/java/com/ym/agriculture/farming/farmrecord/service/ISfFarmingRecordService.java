package com.ym.agriculture.farming.farmrecord.service;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordMediaGeoPatchBo;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordSaveBo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingGrowthStagePreviewVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingLatestRecordImagesVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordMediaVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordVo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 移动端农事记录服务。
 *
 * @author ym-cloud
 */
@Validated
public interface ISfFarmingRecordService {

    /**
     * 分页查询农事记录。
     *
     * @param fieldId        地块 ID，可为空
     * @param workItemId     农事项目 ID，可为空
     * @param categoryId     农事分类 ID，可为空
     * @param status         状态：DRAFT/SUBMITTED，可为空
     * @param fromHappenedAt 发生时间开始，可为空
     * @param toHappenedAt   发生时间结束，可为空
     * @param pageQuery      分页参数
     * @return 农事记录分页
     */
    PageResult<SfFarmingRecordVo> pageForMobile(Long fieldId,
                                                   Long workItemId,
                                                   Long categoryId,
                                                   String status,
                                                   Date fromHappenedAt,
                                                   Date toHappenedAt,
                                                   PageQuery pageQuery);

    /**
     * 查询最新一条含现场图片的已提交农事记录及其图片列表。
     *
     * @param fieldId    地块 ID，可为空；传入时仅在该地块关联记录范围内查找
     * @param imageLimit 返回图片条数上限，默认 9，最大 20
     * @return 最新记录图片；无匹配记录时 {@code recordId} 为 null、{@code images} 为空数组
     */
    SfFarmingLatestRecordImagesVo getLatestRecordImages(Long fieldId, Integer imageLimit);

    /**
     * 管理后台分页查询已提交农事记录。
     *
     * @param fieldId        地块 ID，可为空
     * @param workItemId     农事项目 ID，可为空
     * @param categoryId     农事分类 ID，可为空
     * @param fromHappenedAt 发生时间开始，可为空
     * @param toHappenedAt   发生时间结束，可为空
     * @param pageQuery      分页参数
     * @return 已提交农事记录分页
     */
    PageResult<SfFarmingRecordVo> pageSubmittedForAdmin(Long fieldId,
                                                           Long workItemId,
                                                           Long categoryId,
                                                           Date fromHappenedAt,
                                                           Date toHappenedAt,
                                                           PageQuery pageQuery);

    /**
     * 按地块生成传感器快照预览 JSON。
     *
     * @param fieldIds 地块 ID 列表
     * @return 传感器快照 JSON 字符串
     */
    Map<String, Object> buildSensorPreview(@NotEmpty List<Long> fieldIds);

    String buildSensorPreviewJson(@NotEmpty List<Long> fieldIds);

    /**
     * 按地块生成传感器极简摘要预览。
     *
     * @param fieldIds 地块 ID 列表
     * @return 只包含设备、测点、测点值的摘要对象
     */
    Map<String, Object> buildSensorSimplePreview(@NotEmpty List<Long> fieldIds);

    /**
     * 按农事发生时间预览天气快照。
     *
     * @param happenedAt 农事发生时间
     * @return 天气快照；无匹配预报时返回 source=NONE
     */
    Map<String, Object> buildWeatherPreview(@NotNull Date happenedAt);

    /**
     * 按地块和发生时间预览生长阶段。
     *
     * @param fieldIds   地块 ID 列表
     * @param happenedAt 农事发生时间
     * @return 生长阶段预览结果
     */
    SfFarmingGrowthStagePreviewVo buildGrowthStagePreview(@NotEmpty List<Long> fieldIds, @NotNull Date happenedAt);

    /**
     * 查询农事记录详情。
     *
     * @param recordId 农事记录主键
     * @return 详情
     */
    SfFarmingRecordVo getDetail(@NotNull Long recordId);

    /**
     * 管理后台查询已提交农事记录详情。
     *
     * @param recordId 农事记录主键
     * @return 已提交农事记录详情
     */
    SfFarmingRecordVo getSubmittedDetailForAdmin(@NotNull Long recordId);

    /**
     * 管理后台新增已提交农事记录。
     *
     * @param bo 保存入参
     * @return 新建记录 ID
     */
    Long addSubmittedForAdmin(@Validated(AddGroup.class) SfFarmingRecordSaveBo bo);

    /**
     * 管理后台编辑已提交农事记录。
     *
     * @param recordId 农事记录主键
     * @param bo       保存入参
     * @return 是否成功
     */
    boolean updateSubmittedForAdmin(@NotNull Long recordId, @Validated(EditGroup.class) SfFarmingRecordSaveBo bo);

    /**
     * 管理后台删除当前登录人创建的已提交农事记录。
     *
     * @param recordId 农事记录主键
     * @return 是否成功
     */
    boolean removeSubmittedForAdmin(@NotNull Long recordId);

    /**
     * 新建草稿。
     *
     * @param bo 保存入参
     * @return 新建记录 ID
     */
    Long addDraft(@Validated(AddGroup.class) SfFarmingRecordSaveBo bo);

    /**
     * 更新草稿。
     *
     * @param recordId 农事记录主键
     * @param bo       保存入参
     * @return 是否成功
     */
    boolean updateDraft(@NotNull Long recordId, @Validated(EditGroup.class) SfFarmingRecordSaveBo bo);

    /**
     * 提交草稿为正式记录。
     *
     * @param recordId 农事记录主键
     * @return 是否成功
     */
    boolean submit(@NotNull Long recordId);

    /**
     * 删除草稿。
     *
     * @param recordId 农事记录主键
     * @return 是否成功
     */
    boolean removeDraft(@NotNull Long recordId);

    /**
     * 纠错更新已提交记录。
     *
     * @param recordId 农事记录主键
     * @param bo       保存入参
     * @return 是否成功
     */
    boolean updateSubmitted(@NotNull Long recordId, @Validated(EditGroup.class) SfFarmingRecordSaveBo bo);

    /**
     * 删除已提交记录。
     *
     * @param recordId 农事记录主键
     * @return 是否成功
     */
    boolean removeSubmitted(@NotNull Long recordId);

    /**
     * 补丁更新媒体地理信息。
     *
     * @param recordId 农事记录主键
     * @param mediaId  媒体主键
     * @param body     补丁入参
     * @return 更新后的媒体
     */
    SfFarmingRecordMediaVo patchMediaGeo(@NotNull Long recordId,
                                         @NotNull Long mediaId,
                                         SfFarmingRecordMediaGeoPatchBo body);
}
