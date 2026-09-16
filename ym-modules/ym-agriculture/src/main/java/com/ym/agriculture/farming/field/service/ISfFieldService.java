package com.ym.agriculture.farming.field.service;

import com.ym.agriculture.farming.field.model.bo.SfFieldBo;
import com.ym.agriculture.farming.field.model.vo.SfFieldExportVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldBatchInfoVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldDetailVo;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 地块档案服务。
 *
 * @author ym-cloud
 */
public interface ISfFieldService {

    List<SfFieldVo> queryList(SfFieldBo bo);

    List<SfFieldVo> listFieldsOrdered(SfFieldBo bo);

    PageResult<SfFieldVo> queryPageList(SfFieldBo bo, PageQuery pageQuery);

    SfFieldVo queryById(Long fieldId);

    /** 查询当前租户可见地块及其批次、设备详情。 */
    SfFieldDetailVo queryDetailById(Long fieldId);

    boolean checkFieldNameUnique(SfFieldBo bo);

    SfFieldBatchInfoVo queryBatchInfoByFieldId(Long fieldId);

    Boolean insertByBo(SfFieldBo bo);

    Boolean updateByBo(SfFieldBo bo);

    Boolean updateStatus(Long fieldId, String status);

    Boolean deleteWithValidByIds(Collection<Long> fieldIds);

    List<SfFieldExportVo> queryExportList(SfFieldBo bo);

    List<String> listIotDeviceSnsByField(Long fieldId);

    void saveIotDeviceSnsForField(Long fieldId, List<String> deviceSns);

    void addIotDeviceSnForField(Long fieldId, String deviceSn);

    void removeIotDeviceSnForField(Long fieldId, String deviceSn);
}
