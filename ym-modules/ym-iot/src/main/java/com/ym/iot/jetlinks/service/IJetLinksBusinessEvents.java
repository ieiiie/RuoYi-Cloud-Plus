package com.ym.iot.jetlinks.service;

import java.util.List;
import java.util.Map;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksBusinessEvents {
    /** 接收 JetLinks 主动推送，数据库提交及必要的任务推进完成后返回。 */
    void receive(String consumerId, com.ym.jetlinks.rpc.ChangeEventDto event);

    int consume(String consumerId, int limit);

    void replay(String eventId, String operator);

    List<Map<String, Object>> failures(int limit);
}
