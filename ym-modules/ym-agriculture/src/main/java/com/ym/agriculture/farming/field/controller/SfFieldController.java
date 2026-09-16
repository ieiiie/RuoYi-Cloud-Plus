package com.ym.agriculture.farming.field.controller;

import com.ym.agriculture.farming.field.model.bo.SfFieldBo;
import com.ym.agriculture.farming.field.model.bo.SfFieldStatusBo;
import com.ym.agriculture.farming.field.layout.model.vo.GreenhouseLayoutVo;
import com.ym.agriculture.farming.field.layout.service.IGreenhouseLayoutService;
import com.ym.agriculture.farming.field.model.vo.SfFieldExportVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldDetailVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldBatchInfoVo;
import com.ym.agriculture.farming.field.service.ISfFieldService;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.excel.utils.ExcelBuilder;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地块档案管理。
 *
 * @author ym-cloud
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/field")
public class SfFieldController extends BaseController {

    private final ISfFieldService fieldService;
    private final IGreenhouseLayoutService greenhouseLayoutService;

    @GetMapping("/greenhouse-layout")
    public R<GreenhouseLayoutVo> greenhouseLayout() {
        return R.ok(greenhouseLayoutService.getLayout());
    }

    @GetMapping("/list")
    public R<List<SfFieldVo>> list(SfFieldBo bo) {
        return R.ok(fieldService.queryList(bo));
    }

    @GetMapping("/page")
    public R<PageResult<SfFieldVo>> page(SfFieldBo bo, PageQuery pageQuery) {
        return R.ok(fieldService.queryPageList(bo, pageQuery));
    }

    @GetMapping("/{fieldId}")
    public R<SfFieldVo> getInfo(@PathVariable Long fieldId) {
        return R.ok(fieldService.queryById(fieldId));
    }

    @GetMapping("/check-name")
    public R<Boolean> checkName(@RequestParam String fieldName,
                                @RequestParam(required = false) Long fieldId) {
        SfFieldBo bo = new SfFieldBo();
        bo.setFieldId(fieldId);
        bo.setFieldName(fieldName);
        return R.ok(fieldService.checkFieldNameUnique(bo));
    }

    /** 查询编辑和查看页面共用的完整地块档案。 */
    @GetMapping("/{fieldId}/detail")
    public R<SfFieldDetailVo> detail(@PathVariable Long fieldId) {
        return R.ok(fieldService.queryDetailById(fieldId));
    }

    /** 查询当前租户地块的进行中批次，供编辑页控制作物字段。 */
    @GetMapping("/{fieldId}/batch-info")
    public R<SfFieldBatchInfoVo> batchInfo(@PathVariable Long fieldId) {
        return R.ok(fieldService.queryBatchInfoByFieldId(fieldId));
    }

    @Log(title = "地块管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SfFieldBo bo) {
        return toAjax(fieldService.insertByBo(bo));
    }

    @Log(title = "地块管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SfFieldBo bo) {
        return toAjax(fieldService.updateByBo(bo));
    }

    @Log(title = "地块管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PatchMapping("/{fieldId}/status")
    public R<Void> updateStatus(@PathVariable Long fieldId,
                                @Validated @RequestBody SfFieldStatusBo bo) {
        return toAjax(fieldService.updateStatus(fieldId, bo.getStatus()));
    }

    @Log(title = "地块管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{fieldIds}")
    public R<Void> remove(@PathVariable Long[] fieldIds) {
        return toAjax(fieldService.deleteWithValidByIds(List.of(fieldIds)));
    }

    @Log(title = "地块管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SfFieldBo bo, HttpServletResponse response) {
        List<SfFieldExportVo> list = fieldService.queryExportList(bo);
        ExcelBuilder.of(list, SfFieldExportVo.class).sheetName("地块数据").toResponse(response);
    }

    @GetMapping("/{fieldId}/iot-devices")
    public R<List<String>> listIotDevices(@PathVariable Long fieldId) {
        return R.ok(fieldService.listIotDeviceSnsByField(fieldId));
    }

    @Log(title = "地块物联网设备绑定", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{fieldId}/iot-devices")
    public R<Void> saveIotDevices(@PathVariable Long fieldId, @RequestBody List<String> deviceSns) {
        fieldService.saveIotDeviceSnsForField(fieldId, deviceSns);
        return R.ok();
    }

    @Log(title = "地块传感器绑定", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{fieldId}/sensors")
    public R<Void> addSensor(@PathVariable Long fieldId, @RequestBody String deviceSn) {
        fieldService.addIotDeviceSnForField(fieldId, deviceSn);
        return R.ok();
    }

    @Log(title = "地块传感器解绑", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @DeleteMapping("/{fieldId}/sensors/{deviceSn}")
    public R<Void> removeSensor(@PathVariable Long fieldId, @PathVariable String deviceSn) {
        fieldService.removeIotDeviceSnForField(fieldId, deviceSn);
        return R.ok();
    }
}
