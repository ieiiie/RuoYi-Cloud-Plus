package com.ym.iot.jetlinks.service;

import com.ym.jetlinks.rpc.CommandDto;
import com.ym.jetlinks.rpc.RecordDto;

import java.util.Map;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksCommandService {
    CommandDto submit(Long id, String function, Map<String, Object> inputs);

    CommandDto get(String commandId);

    RecordDto capabilities(Long id);

    CommandDto awaitPayload(CommandDto initial);
}
