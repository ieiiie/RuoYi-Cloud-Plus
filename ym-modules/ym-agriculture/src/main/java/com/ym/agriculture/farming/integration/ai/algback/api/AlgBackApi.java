package com.ym.agriculture.farming.integration.ai.algback.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackPageVo;
import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackResultVo;
import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackSimpleMsg;
import com.ym.agriculture.farming.integration.ai.algback.dto.customer.AlgBackCustomerAddRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.customer.AlgBackCustomerPageRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.customer.AlgBackCustomerUpdateRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.customer.AlgBackUpdateCustomerHttpRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.login.AlgBackLoginData;
import com.ym.agriculture.farming.integration.ai.algback.dto.login.AlgBackLoginRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.model.AlgBackAlgorithmModelListRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.model.AlgBackAlgorithmModelPageRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAddTaskResult;
import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAlgorithmTaskAddRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAlgorithmTaskPageRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAlgorithmTaskStatusRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.imageinfer.AlgBackImageInferSubmitRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.imageinfer.AlgBackImageInferSubmitResult;
import com.ym.agriculture.farming.integration.ai.algback.dto.imageinfer.AlgBackImageInferTaskResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 算法中台 Retrofit 声明，路径均相对 {@code baseUrl}。
 * <p>
 * 完整 URL = {@code baseUrl} + 本接口相对路径；{@code baseUrl} 一般为中台 API 根（常见含 {@code /api}），
 * <strong>末尾须有 {@code /}</strong> 以满足 Retrofit。
 * <p>
 * 除 {@link #login} 外，中台 {@code JwtFilter} 读取请求头 {@code token}；
 * 本模块通过 {@link com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackTokenInterceptor} 从 {@link com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackTokenHolder} 注入。
 * <p>
 * 响应体统一为 {@link AlgBackResultVo}，成功以 {@code code == 200} 为准；同步调用见 {@link com.ym.agriculture.farming.integration.ai.algback.client.AlgBackSync}。
 *
 * @author ym-cloud
 */
public interface AlgBackApi {

    /** 登录，无需 token；成功时 {@code data.token} 供后续请求头使用。 */
    @POST("sys/auth/login")
    Call<AlgBackResultVo<AlgBackLoginData>> login(@Body AlgBackLoginRequest body);

    /** 创建客户；新客户默认停用，响应不返回 id/customerNo，需再调 {@link #getCustomerListPageVo}。 */
    @POST("customer/addCustomer")
    Call<AlgBackResultVo<AlgBackSimpleMsg>> addCustomer(@Body AlgBackCustomerAddRequest body);

    /** 客户分页；从 {@code list} 取 id、customerNo 等。 */
    @POST("customer/getCustomerListPageVo")
    Call<AlgBackResultVo<AlgBackPageVo>> getCustomerListPageVo(@Body AlgBackCustomerPageRequest body);

    /** 客户详情（可选）。 */
    @GET("customer/getCustomerById/{id}")
    Call<AlgBackResultVo<JsonNode>> getCustomerById(@Path("id") long id);

    /**
     * 更新客户；接入时须将 {@code status=1} 启用，否则推理结果不会推送到 {@code httpReqUrl}。
     */
    @POST("customer/updateCustomer")
    Call<AlgBackResultVo<AlgBackSimpleMsg>> updateCustomer(@Body AlgBackCustomerUpdateRequest body);

    /** 仅更新推送 URL / 自定义头（可选）。 */
    @POST("customer/updateCustomerHttp")
    Call<AlgBackResultVo<AlgBackSimpleMsg>> updateCustomerHttp(@Body AlgBackUpdateCustomerHttpRequest body);

    /**
     * 模型列表（非分页）；成功时 {@code data} 为中台 JSON 数组，按 {@link JsonNode} 遍历。
     */
    @POST("algorithmModel/getAlgorithmModelList")
    Call<AlgBackResultVo<JsonNode>> getAlgorithmModelList(@Body AlgBackAlgorithmModelListRequest body);

    /** 模型分页（可选）。 */
    @POST("algorithmModel/getAlgorithmModelListPageVo")
    Call<AlgBackResultVo<AlgBackPageVo>> getAlgorithmModelListPageVo(@Body AlgBackAlgorithmModelPageRequest body);

    /** 模型详情（可选）。 */
    @GET("algorithmModel/getAlgorithmModelById/{id}")
    Call<AlgBackResultVo<JsonNode>> getAlgorithmModelById(@Path("id") long id);

    /** 创建计算任务；新建后 taskStatus 为 0，需 {@link #setAlgorithmTaskStatus} 启动。 */
    @POST("algorithmTask/addAlgorithmTask")
    Call<AlgBackResultVo<AlgBackAddTaskResult>> addAlgorithmTask(@Body AlgBackAlgorithmTaskAddRequest body);

    /** 启动/停止任务：{@code taskStatus=1} 启动，{@code 0} 停止。 */
    @POST("algorithmTask/setAlgorithmTaskStatus")
    Call<AlgBackResultVo<AlgBackSimpleMsg>> setAlgorithmTaskStatus(@Body AlgBackAlgorithmTaskStatusRequest body);

    /** 任务详情（可选）。 */
    @GET("algorithmTask/getAlgorithmTaskById/{id}")
    Call<AlgBackResultVo<JsonNode>> getAlgorithmTaskById(@Path("id") long id);

    /** 任务分页（可选）。 */
    @POST("algorithmTask/getAlgorithmTaskListPageVo")
    Call<AlgBackResultVo<AlgBackPageVo>> getAlgorithmTaskListPageVo(@Body AlgBackAlgorithmTaskPageRequest body);

    // ==================== 图片推理 ====================

    /** 提交图片推理任务（异步 URL 方式）；成功时 {@code data.taskId} 标识任务。 */
    @POST("imageInfer/submitTask")
    Call<AlgBackResultVo<AlgBackImageInferSubmitResult>> submitImageInferTask(
        @Body AlgBackImageInferSubmitRequest body);

    /** 查询图片推理任务状态及结果。 */
    @GET("imageInfer/taskResult")
    Call<AlgBackResultVo<AlgBackImageInferTaskResult>> getImageInferTaskResult(
        @Query("taskId") String taskId);
}
