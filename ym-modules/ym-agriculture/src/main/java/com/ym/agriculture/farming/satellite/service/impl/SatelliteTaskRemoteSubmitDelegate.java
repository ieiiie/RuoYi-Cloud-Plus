package com.ym.agriculture.farming.satellite.service.impl;

import cn.hutool.json.JSONUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.satellite.config.SatelliteProperties;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskMapper;
import com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTask;
import com.ym.agriculture.farming.satellite.remote.SatelliteRemoteClient;
import com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskRequest;
import com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.ObjectProvider;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Remote submit delegate for satellite tasks.
 * <p>
 * Supports both retained batch compensation submission and immediate submission
 * right after task creation.
 */
@Slf4j
@RequiredArgsConstructor
final class SatelliteTaskRemoteSubmitDelegate {

    private final SatelliteProperties satelliteProperties;

    private final ObjectProvider<SatelliteRemoteClient> satelliteRemoteClientProvider;

    private final SfSatelliteTaskMapper taskMapper;

    private final SatelliteTaskCrudDelegate crudDelegate;

    void submitPendingTasksBatch() {
        SatelliteRemoteClient satelliteRemoteClient = satelliteRemoteClientProvider.getIfAvailable();
        if (satelliteRemoteClient == null) {
            return;
        }
        TenantHelper.ignore(() -> {
            try {
                submitTasks(taskMapper.selectPendingSubmitFirst50(), satelliteRemoteClient, "batch-compensation");
            } catch (Exception e) {
                log.error("Satellite batch compensation submit failed", e);
            }
        });
    }

    void submitTasksNow(List<String> dkIds) {
        if (dkIds == null || dkIds.isEmpty()) {
            return;
        }
        SatelliteRemoteClient satelliteRemoteClient = satelliteRemoteClientProvider.getIfAvailable();
        if (satelliteRemoteClient == null) {
            return;
        }
        TenantHelper.ignore(() -> {
            try {
                List<SfSatelliteTask> tasks = taskMapper.selectByDkIds(dkIds).stream()
                    .filter(this::canSubmitNow)
                    .collect(Collectors.toList());
                submitTasks(tasks, satelliteRemoteClient, "create-immediate");
            } catch (Exception e) {
                log.error("Satellite immediate submit failed dkIds={}", dkIds, e);
            }
        });
    }

    private boolean canSubmitNow(SfSatelliteTask task) {
        if (task == null || task.getStatus() == null) {
            return false;
        }
        return task.getStatus() == SatelliteTaskStatus.PENDING_SUBMIT
            || task.getStatus() == SatelliteTaskStatus.FAILED;
    }

    private void submitTasks(List<SfSatelliteTask> tasks, SatelliteRemoteClient satelliteRemoteClient, String trigger) {
        if (tasks == null || tasks.isEmpty()) {
            log.info("No satellite tasks to submit, trigger={}", trigger);
            return;
        }
        log.info("Start satellite submit, trigger={}, taskCount={}", trigger, tasks.size());
        int maxConcurrent = Math.max(1, satelliteProperties.getSubmitMaxConcurrency());
        Semaphore inFlight = new Semaphore(maxConcurrent);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>(tasks.size());
        for (SfSatelliteTask task : tasks) {
            futures.add(submitTaskToRemoteAsync(task, satelliteRemoteClient, inFlight));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        int ok = 0;
        int fail = 0;
        for (CompletableFuture<Boolean> future : futures) {
            try {
                if (Boolean.TRUE.equals(future.join())) {
                    ok++;
                } else {
                    fail++;
                }
            } catch (Exception e) {
                fail++;
            }
        }
        log.info("Satellite submit finished, trigger={}, success={}, failed={}", trigger, ok, fail);
    }

    private CompletableFuture<Boolean> submitTaskToRemoteAsync(SfSatelliteTask task,
                                                               SatelliteRemoteClient satelliteRemoteClient,
                                                               Semaphore inFlight) {
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        if (StringUtils.isBlank(satelliteProperties.getCallUrl())) {
            log.warn("satellite.call-url is blank, skip submit dkId={}", task.getDkId());
            TenantHelper.ignore(() -> crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, "satellite.call-url is blank"));
            done.complete(false);
            return done;
        }

        final SatelliteRemoteTaskRequest request;
        try {
            request = buildRemoteRequest(task);
            log.info("request:{}", JsonUtils.toJsonString(request));
        } catch (Exception e) {
            log.error("Build satellite request failed dkId={}", task.getDkId(), e);
            TenantHelper.ignore(() -> crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, "submit failed: " + e.getMessage()));
            done.complete(false);
            return done;
        }

        try {
            inFlight.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Acquire satellite submit permit interrupted dkId={}", task.getDkId());
            TenantHelper.ignore(() -> crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, "submit interrupted"));
            done.complete(false);
            return done;
        }

        Call<SatelliteRemoteTaskResponse> call = satelliteRemoteClient.submitTask(request);
        try {
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<SatelliteRemoteTaskResponse> call, Response<SatelliteRemoteTaskResponse> response) {
                    try {
                        TenantHelper.ignore(() -> {
                            try {
                                if (!response.isSuccessful()) {
                                    String err = handleErrorBody(response.errorBody());
                                    log.error("Satellite submit failed dkId={} {}", task.getDkId(), err);
                                    crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, err);
                                    done.complete(false);
                                    return;
                                }
                                SatelliteRemoteTaskResponse body = response.body();
                                if (body == null) {
                                    crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, "empty response body");
                                    done.complete(false);
                                    return;
                                }
                                if (body.getCode() != null && body.getCode() != 200) {
                                    String msg = "code=" + body.getCode() + ", message=" + body.getMessage();
                                    crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, msg);
                                    done.complete(false);
                                    return;
                                }
                                int cnt = task.getSubmitCount() != null ? task.getSubmitCount() + 1 : 1;
                                crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.PROCESSING,
                                    body.getMessage() != null ? body.getMessage() : "submitted", cnt);
                                log.info("Satellite task submitted dkId={}", task.getDkId());
                                done.complete(true);
                            } catch (Exception e) {
                                log.error("Handle satellite response failed dkId={}", task.getDkId(), e);
                                crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, "submit failed: " + e.getMessage());
                                done.complete(false);
                            }
                        });
                    } finally {
                        inFlight.release();
                    }
                }

                @Override
                public void onFailure(Call<SatelliteRemoteTaskResponse> call, Throwable t) {
                    try {
                        TenantHelper.ignore(() -> {
                            log.error("Satellite network request failed dkId={}", task.getDkId(), t);
                            crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, "network request failed: " + t.getMessage());
                            done.complete(false);
                        });
                    } finally {
                        inFlight.release();
                    }
                }
            });
        } catch (RuntimeException e) {
            inFlight.release();
            log.error("Enqueue satellite submit failed dkId={}", task.getDkId(), e);
            TenantHelper.ignore(() -> crudDelegate.updateTaskStatus(task.getDkId(), SatelliteTaskStatus.FAILED, "submit failed: " + e.getMessage()));
            done.complete(false);
        }
        return done;
    }

    private SatelliteRemoteTaskRequest buildRemoteRequest(SfSatelliteTask task) {
        SatelliteRemoteTaskRequest request = new SatelliteRemoteTaskRequest();
        request.setDkId(task.getDkId());
        request.setDkGeom(JSONUtil.toBean(task.getDkGeom(), SatelliteRemoteTaskRequest.DkGeom.class));
        request.setCodeCroptype(task.getCodeCroptype());
        request.setStartDate(task.getStartDate());
        request.setEndDate(task.getEndDate());
        request.setTaskType(task.getTaskType());
        request.setPixelImage(task.getPixelImage() != null ? task.getPixelImage() : 10);
        request.setUrl(satelliteProperties.getCallUrl());
        return request;
    }

    private static String handleErrorBody(ResponseBody errorBody) throws IOException {
        if (errorBody != null) {
            return "server error: " + errorBody.string();
        }
        return "server error response";
    }
}
