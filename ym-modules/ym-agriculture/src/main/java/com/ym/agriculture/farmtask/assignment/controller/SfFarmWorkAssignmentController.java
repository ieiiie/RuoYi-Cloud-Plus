package com.ym.agriculture.farmtask.assignment.controller;

import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignBatchBo;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignCancelBatchBo;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignPageBo;
import com.ym.agriculture.farmtask.assignment.model.vo.SfFarmAssignDetailVo;
import com.ym.agriculture.farmtask.assignment.model.vo.SfFarmAssignGreenhouseVo;
import com.ym.agriculture.farmtask.assignment.service.ISfFarmWorkAssignmentService;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * stask 农事分配管理接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/stask/farm-assign")
public class SfFarmWorkAssignmentController extends BaseController {

    private final ISfFarmWorkAssignmentService assignmentService;

    /**
     * 分页查询大棚维度农事分配统计。
     *
     * @param bo        查询条件，支持大棚编号/名称模糊匹配与只看需分配
     * @param pageQuery 分页参数
     * @return 大棚分配统计分页
     */
    @GetMapping("/greenhouses")
    public R<PageResult<SfFarmAssignGreenhouseVo>> greenhouses(SfFarmAssignPageBo bo, PageQuery pageQuery) {
        return R.ok(assignmentService.queryGreenhousePage(bo, pageQuery));
    }

    /**
     * 查询指定大棚的农事分配详情。
     *
     * @param greenhouseId 大棚ID
     * @return 已分配分组与待分配农事项
     */
    @GetMapping("/greenhouses/{greenhouseId}/detail")
    public R<SfFarmAssignDetailVo> detail(@NotNull @PathVariable Long greenhouseId) {
        return R.ok(assignmentService.queryDetail(greenhouseId));
    }

    /**
     * 批量分配农事项给同一组长。
     *
     * @param bo 批量分配入参
     * @return 分配结果
     */
    @Log(title = "stask农事分配", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/batch")
    public R<Void> batchAssign(@Valid @RequestBody SfFarmAssignBatchBo bo) {
        return toAjax(assignmentService.batchAssign(bo));
    }

    /**
     * 取消单条农事分配。
     *
     * @param assignmentId 分配记录ID
     * @return 取消结果
     */
    @Log(title = "stask农事分配", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/{assignmentId}")
    public R<Void> remove(@NotNull @PathVariable Long assignmentId) {
        return toAjax(assignmentService.remove(assignmentId));
    }

    /**
     * 批量取消指定组长在大棚下的农事分配。
     *
     * @param bo 批量取消入参
     * @return 取消结果
     */
    @Log(title = "stask农事分配", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/batch")
    public R<Void> batchRemove(@Valid @RequestBody SfFarmAssignCancelBatchBo bo) {
        return toAjax(assignmentService.batchRemove(bo));
    }

    /**
     * 查询当前大棚可选组长列表。
     *
     * @param greenhouseId 大棚ID，用于过滤已参与本棚的组长
     * @param keyword      搜索关键字，可匹配姓名或手机号
     * @return 可选组长列表
     */
    @GetMapping("/leaders")
    public R<List<SysEmployeeLeaderOptionVo>> leaders(@NotNull @RequestParam Long greenhouseId,
        @RequestParam(value = "keyword", required = false) String keyword) {
        return R.ok(assignmentService.queryLeaderOptions(greenhouseId, keyword));
    }
}
