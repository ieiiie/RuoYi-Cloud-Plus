package com.ym.system.controller.system;

import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.R;
import com.ym.system.domain.vo.SysDictTypeVo;
import com.ym.system.service.ISysDictTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SaaS 运行态字典类型读取接口。
 * 字典维护已迁移至 Dbo，本服务不再注册管理 CRUD。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/dict/type")
public class SysDictTypeController {
    private final ISysDictTypeService dictTypeService;

    @GetMapping("/optionselect")
    public R<List<SysDictTypeVo>> optionselect() {
        return R.ok(dictTypeService.selectDictTypeAll());
    }
}
