package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * stask 工单编号生成器。
 */
@Component
public class SfStaskOrderNoGenerator {

    /**
     * 生成工单编号。
     *
     * @param now 当前时间
     * @return 工单编号
     */
    public String next(Date now) {
        return "ST" + DateUtil.format(now, "yyyyMMdd") + IdWorker.getIdStr().substring(12);
    }
}
