package com.ym.system.dubbo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.RemoteSocialService;
import com.ym.system.api.domain.bo.RemoteSocialBo;
import com.ym.system.api.domain.vo.RemoteSocialVo;
import com.ym.system.domain.SysGlobalSocial;
import com.ym.system.mapper.SysGlobalSocialMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 全局第三方账号绑定服务。
 *
 * <p>绑定不再依赖当前租户的 {@code sys_user}，这样社交和小程序认证可先定位
 * 全局账号，再由用户服务选择当前登录租户。</p>
 *
 * @author Michelle.Chung
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteSocialServiceImpl implements RemoteSocialService {

    private final SysGlobalSocialMapper globalSocialMapper;

    @Override
    public List<RemoteSocialVo> selectByAuthId(String authId) {
        List<SysGlobalSocial> list = TenantHelper.ignore(() -> globalSocialMapper.lambda()
            .eq(SysGlobalSocial::getAuthId, authId)
            .list());
        return BeanUtil.copyToList(list, RemoteSocialVo.class);
    }

    @Override
    public List<RemoteSocialVo> queryList(RemoteSocialBo bo) {
        List<SysGlobalSocial> list = TenantHelper.ignore(() -> globalSocialMapper.lambda()
            .eqIfPresent(SysGlobalSocial::getGlobalUserId, bo.getGlobalUserId())
            .eqIfPresent(SysGlobalSocial::getAuthId, bo.getAuthId())
            .eqIfPresent(SysGlobalSocial::getSource, bo.getSource())
            .eqIfPresent(SysGlobalSocial::getOpenId, bo.getOpenId())
            .list());
        return BeanUtil.copyToList(list, RemoteSocialVo.class);
    }

    @Override
    public void insertByBo(RemoteSocialBo bo) {
        if (ObjectUtil.isNull(bo.getGlobalUserId())) {
            throw new ServiceException("全局账号ID不能为空");
        }
        SysGlobalSocial social = BeanUtil.toBean(bo, SysGlobalSocial.class);
        TenantHelper.ignore(() -> globalSocialMapper.insert(social));
    }

    @Override
    public void updateByBo(RemoteSocialBo bo) {
        if (ObjectUtil.isNull(bo.getId()) || ObjectUtil.isNull(bo.getGlobalUserId())) {
            throw new ServiceException("第三方绑定信息不完整");
        }
        SysGlobalSocial social = BeanUtil.toBean(bo, SysGlobalSocial.class);
        TenantHelper.ignore(() -> globalSocialMapper.updateById(social));
    }

    @Override
    public Boolean deleteWithValidById(Long socialId) {
        return TenantHelper.ignore(() -> globalSocialMapper.deleteBindingById(socialId) > 0);
    }
}
