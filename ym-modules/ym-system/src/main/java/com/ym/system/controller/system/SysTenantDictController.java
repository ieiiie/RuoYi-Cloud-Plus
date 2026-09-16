package com.ym.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import com.ym.system.domain.bo.SysTenantDictDataBo;
import com.ym.system.domain.bo.SysTenantDictTypeBo;
import com.ym.system.domain.vo.SysTenantDictDataVo;
import com.ym.system.domain.vo.SysTenantDictTypeVo;
import com.ym.system.service.ISysTenantDictService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 租户字典：类型只读，值按当前租户维护。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/tenant-dict")
public class SysTenantDictController extends BaseController {
    private final ISysTenantDictService service;

    @SaCheckPermission("system:tenantDict:list")
    @GetMapping("/type/list")
    public R<PageResult<SysTenantDictTypeVo>> typeList(SysTenantDictTypeBo bo, PageQuery pageQuery) {
        return R.ok(service.queryTypePage(bo, pageQuery));
    }

    @GetMapping("/type/options")
    public R<List<SysTenantDictTypeVo>> typeOptions() {
        return R.ok(service.queryTypeOptions());
    }

    @SaCheckPermission("system:tenantDict:query")
    @GetMapping("/type/{id}")
    public R<SysTenantDictTypeVo> typeInfo(@PathVariable Long id) {
        return R.ok(service.queryType(id));
    }

    @SaCheckPermission("system:tenantDict:list")
    @GetMapping("/data/list")
    public R<PageResult<SysTenantDictDataVo>> dataList(SysTenantDictDataBo bo, PageQuery pageQuery) {
        return R.ok(service.queryDataPage(bo, pageQuery));
    }

    @GetMapping("/data/type/{dictType}")
    public R<List<SysTenantDictDataVo>> dataByType(@PathVariable String dictType) {
        return R.ok(service.queryDataByType(dictType));
    }

    @SaCheckPermission("system:tenantDict:query")
    @GetMapping("/data/{id}")
    public R<SysTenantDictDataVo> dataInfo(@PathVariable Long id) {
        return R.ok(service.queryData(id));
    }

    @SaCheckPermission("system:tenantDict:add")
    @Log(title = "租户字典值", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/data")
    public R<Long> addData(@Validated(AddGroup.class) @RequestBody SysTenantDictDataBo bo) {
        return R.ok(service.insertData(bo));
    }

    @SaCheckPermission("system:tenantDict:edit")
    @Log(title = "租户字典值", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/data")
    public R<Void> editData(@Validated(EditGroup.class) @RequestBody SysTenantDictDataBo bo) {
        service.updateData(bo);
        return R.ok();
    }

    @SaCheckPermission("system:tenantDict:edit")
    @Log(title = "租户字典排序", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/data/order")
    public R<Void> sortData(@RequestBody List<Long> dictCodes) {
        service.sortData(dictCodes);
        return R.ok();
    }

    @SaCheckPermission("system:tenantDict:remove")
    @Log(title = "租户字典值", businessType = BusinessType.DELETE)
    @DeleteMapping("/data/{ids}")
    public R<Void> removeData(@PathVariable Long[] ids) {
        service.deleteData(List.of(ids));
        return R.ok();
    }
}
