package com.ym.agriculture.farming.integration.ai.algback.client;

import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackResultVo;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * 算法中台 Retrofit 同步调用：处理 HTTP、空体与中台业务码。
 * <p>
 * {@code code == 200} 为成功；失败时 {@code msg}/{@code data} 可能含原因。成功返回 {@link AlgBackResultVo#getData()}，可为 {@code null}。
 *
 * @author ym-cloud
 */
public final class AlgBackSync {

    private AlgBackSync() {
    }

    /**
     * 同步执行请求；HTTP 2xx 且 {@code body.code == 200} 时返回 {@code data}。
     *
     * @throws AlgBackClientException HTTP 非成功、响应体为空，或 {@code code != 200}
     */
    public static <T> T execute(Call<AlgBackResultVo<T>> call) {
        try {
            Response<AlgBackResultVo<T>> response = call.execute();
            if (!response.isSuccessful()) {
                String err = readError(response);
                throw new AlgBackClientException(response.code(), err);
            }
            AlgBackResultVo<T> body = response.body();
            if (body == null) {
                throw new AlgBackClientException("算法中台响应体为空");
            }
            if (!body.isSuccess()) {
                throw new AlgBackClientException(body);
            }
            return body.getData();
        } catch (IOException e) {
            throw AlgBackClientException.fromIo(e);
        }
    }

    private static String readError(Response<?> response) {
        if (response.errorBody() == null) {
            return "";
        }
        try (ResponseBody eb = response.errorBody()) {
            return eb.string();
        } catch (IOException e) {
            return "";
        }
    }
}
