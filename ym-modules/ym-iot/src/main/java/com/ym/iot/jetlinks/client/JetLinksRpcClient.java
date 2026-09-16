package com.ym.iot.jetlinks.client;

import com.ym.common.core.exception.ServiceException;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.jetlinks.rpc.*;

import lombok.Getter;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** One transport policy for every core RPC. Timeouts never trigger a second command. */
@Getter
@Component
@ConditionalOnJetLinks
public class JetLinksRpcClient {
    public static final int READ_TIMEOUT = 5000;
    public static final int WRITE_TIMEOUT = 10000;
    public static final int COMMAND_TIMEOUT = 5000;
    public static final int VIDEO_START_TIMEOUT = 60000;
    // RPC deadlines belong to each method. This outer wait must not cut short a first video start.
    public static final int AWAIT_TIMEOUT_SECONDS = 65;

    @DubboReference(
            group = "jetlinks-iot",
            version = "2.0.0",
            retries = 0,
            timeout = READ_TIMEOUT,
            check = false)
    private IotCatalogRpcService catalog;

    @DubboReference(
            group = "jetlinks-iot",
            version = "2.0.0",
            retries = 0,
            timeout = READ_TIMEOUT,
            check = false)
    private IotDeviceRpcService device;

    @DubboReference(
            group = "jetlinks-iot",
            version = "1.0.0",
            retries = 0,
            timeout = READ_TIMEOUT,
            check = false,
            methods = @Method(name = "ingest", timeout = WRITE_TIMEOUT, retries = 0))
    private IotTelemetryRpcService telemetry;

    @DubboReference(
            group = "jetlinks-iot",
            version = "1.0.0",
            retries = 0,
            timeout = READ_TIMEOUT,
            check = false,
            methods = {
                @Method(name = "submit", timeout = COMMAND_TIMEOUT, retries = 0),
                @Method(name = "setOperationFence", timeout = WRITE_TIMEOUT, retries = 0)
            })
    private IotCommandRpcService command;

    @DubboReference(
            group = "jetlinks-iot",
            version = "1.0.0",
            retries = 0,
            timeout = READ_TIMEOUT,
            check = false,
            methods = {
                @Method(name = "start", timeout = VIDEO_START_TIMEOUT, retries = 0),
                        @Method(name = "stop", timeout = WRITE_TIMEOUT, retries = 0),
                @Method(name = "control", timeout = COMMAND_TIMEOUT, retries = 0),
                        @Method(name = "frontEndPtz", timeout = COMMAND_TIMEOUT, retries = 0)
            })
    private IotVideoRpcService video;

    @DubboReference(
            group = "jetlinks-iot",
            version = "2.0.0",
            retries = 0,
            timeout = READ_TIMEOUT,
            check = false,
            methods = {
                @Method(name = "saveConfiguration", timeout = WRITE_TIMEOUT, retries = 0),
                @Method(name = "deleteConfiguration", timeout = WRITE_TIMEOUT, retries = 0),
                @Method(name = "setEnabled", timeout = WRITE_TIMEOUT, retries = 0),
                @Method(name = "handle", timeout = WRITE_TIMEOUT, retries = 0),
                @Method(name = "removeDeviceScope", timeout = WRITE_TIMEOUT, retries = 0)
            })
    private IotAlarmRpcService alarm;

    @DubboReference(
            group = "jetlinks-iot",
            version = "1.0.0",
            retries = 0,
            timeout = READ_TIMEOUT,
            check = false,
            methods = @Method(name = "acknowledge", timeout = WRITE_TIMEOUT, retries = 0))
    private IotChangeRpcService change;

    public static <T> T await(CompletableFuture<T> future) {
        if (future == null) throw new ServiceException("JetLinks provider returned no future");
        try {
            return future.get(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(
                    "JetLinks RPC interrupted; command outcome may be unknown; do not resubmit");
        } catch (TimeoutException ex) {
            throw new ServiceException(
                    "JetLinks RPC timed out; command outcome is UNKNOWN; do not resubmit");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            throw new ServiceException(
                    "JetLinks RPC failed: "
                            + (cause == null ? ex.getMessage() : cause.getMessage()));
        }
    }

    public static ServiceException invalidProviderRecord(String semantic) {
        return new ServiceException("JetLinks返回的记录缺少必需字段: " + semantic);
    }

    public RequestContext context() {
        return context(UUID.randomUUID().toString());
    }

    public RequestContext context(String requestId) {
        if (requestId == null || requestId.isBlank())
            throw new ServiceException("requestId is required");
        return new RequestContext(requestId, "ym-iot", LoginHelper.getUserIdStr());
    }

    public RequestContext deviceContext(String requestId, Long assignmentVersion) {
        return deviceContext(requestId, assignmentVersion, LoginHelper.getUserIdStr());
    }

    public RequestContext deviceContext(
            String requestId, Long assignmentVersion, String operatorId) {
        if (requestId == null
                || requestId.isBlank()
                || assignmentVersion == null
                || assignmentVersion <= 0)
            throw new ServiceException("设备请求必须包含 requestId 和有效 assignmentVersion");
        return new RequestContext(requestId, "ym-iot", operatorId, assignmentVersion);
    }

    /** Compatibility fence helper; ownership currently uses its independent RPC consumer. */
    public boolean setOperationFence(
            String requestId, Long deviceId, Long assignmentVersion, boolean frozen) {
        if (deviceId == null || assignmentVersion == null)
            throw new ServiceException("device/version required for fence");
        return Boolean.TRUE.equals(
                await(
                        command.setOperationFence(
                                deviceContext(requestId, assignmentVersion),
                                deviceId.toString(),
                                assignmentVersion,
                                frozen)));
    }
}
