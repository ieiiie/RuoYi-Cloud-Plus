package com.ym.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.core.enums.PushSourceEnum;
import com.ym.common.core.enums.PushTypeEnum;
import com.ym.common.core.service.DictService;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import com.ym.resource.api.RemoteMessageService;
import com.ym.resource.api.domain.RemotePushPayLoad;
import com.ym.system.domain.bo.SysNoticeBo;
import com.ym.system.domain.vo.SysNoticeVo;
import com.ym.system.service.ISysNoticeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 公告 信息操作处理
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/notice")
public class SysNoticeController extends BaseController {

    private final ISysNoticeService noticeService;
    private final DictService dictService;
    private final RemoteMessageService remoteMessageService;

    /**
     * 获取通知公告列表
     */
    @SaCheckPermission("system:notice:list")
    @GetMapping("/list")
    public R<PageResult<SysNoticeVo>> list(SysNoticeBo notice, PageQuery pageQuery) {
        return R.ok(noticeService.selectPageNoticeList(notice, pageQuery));
    }

    /**
     * 根据通知公告编号获取详细信息
     *
     * @param noticeId 公告ID
     */
    @SaCheckPermission("system:notice:query")
    @GetMapping(value = "/{noticeId}")
    public R<SysNoticeVo> getInfo(@PathVariable Long noticeId) {
        return R.ok(noticeService.selectNoticeById(noticeId));
    }

    /**
     * 新增通知公告
     */
    @SaCheckPermission("system:notice:add")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysNoticeBo notice) {
        int rows = noticeService.insertNotice(notice);
        if (rows <= 0) {
            return R.fail();
        }
        String type = dictService.getDictLabel("sys_notice_type", notice.getNoticeType());
        Map<String, Object> data = new HashMap<>(6);
        data.put("noticeType", notice.getNoticeType());
        data.put("noticeTypeLabel", type);
        data.put("noticeTitle", notice.getNoticeTitle());
        data.put("noticeId", notice.getNoticeId());
        data.put("noticeContent", notice.getNoticeContent());
        data.put("status", notice.getStatus());
        remoteMessageService.publishAllPayload(RemotePushPayLoad.of(
            PushTypeEnum.NOTICE,
            PushSourceEnum.NOTICE,
            "[" + type + "] " + notice.getNoticeTitle(),
            data,
            "/system/notice?noticeId=" + notice.getNoticeId()
        ));
        return R.ok();
    }

    /**
     * 修改通知公告
     */
    @SaCheckPermission("system:notice:edit")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysNoticeBo notice) {
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 删除通知公告
     *
     * @param noticeIds 公告ID串
     */
    @SaCheckPermission("system:notice:remove")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public R<Void> remove(@PathVariable Long[] noticeIds) {
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
