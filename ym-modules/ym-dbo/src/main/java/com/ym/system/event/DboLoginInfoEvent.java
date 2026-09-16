package com.ym.system.event;

import lombok.Data;

/**
 * Dbo 登录日志事件。请求信息随事件传递，供异步监听器写入 Dbo 自身数据库。
 */
@Data
public class DboLoginInfoEvent {

    private String username;
    private String status;
    private String message;
    private String ip;
    private String userAgent;
    private String clientId;
    private Object[] args;
}
