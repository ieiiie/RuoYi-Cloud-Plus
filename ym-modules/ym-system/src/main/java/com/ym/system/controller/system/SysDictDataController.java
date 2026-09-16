package com.ym.system.controller.system;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.R;
import com.ym.system.domain.vo.SysDictDataVo;
import com.ym.system.service.ISysDictTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/** SaaS 运行态按类型读取全局字典；管理 CRUD 仅由 Dbo 提供。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/dict/data")
public class SysDictDataController {
    private final ISysDictTypeService dictTypeService;

    @GetMapping("/type/{dictType}")
    public R<List<SysDictDataVo>> dictType(@PathVariable String dictType) {
        List<SysDictDataVo> data = dictTypeService.selectDictDataByType(dictType);
        return R.ok(ObjectUtil.defaultIfNull(data, new ArrayList<>()));
    }
}
