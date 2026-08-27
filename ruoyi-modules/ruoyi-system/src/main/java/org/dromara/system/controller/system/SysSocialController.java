package org.dromara.system.controller.system;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.api.domain.vo.RemoteSocialVo;
import org.dromara.system.domain.SysGlobalSocial;
import org.dromara.system.mapper.SysGlobalSocialMapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 全局社会化关系
 *
 * @author thiszhc
 * @date 2023-06-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/social")
public class SysSocialController extends BaseController {

    private final SysGlobalSocialMapper globalSocialMapper;

    /**
     * 查询社会化关系列表
     */
    @GetMapping("/list")
    public R<List<RemoteSocialVo>> list() {
        List<SysGlobalSocial> list = TenantHelper.ignore(() -> globalSocialMapper.lambda()
            .eq(SysGlobalSocial::getGlobalUserId, LoginHelper.getGlobalUserId())
            .list());
        return R.ok(BeanUtil.copyToList(list, RemoteSocialVo.class));
    }

}
