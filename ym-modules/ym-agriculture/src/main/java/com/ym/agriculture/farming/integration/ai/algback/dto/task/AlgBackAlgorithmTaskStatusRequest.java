package com.ym.agriculture.farming.integration.ai.algback.dto.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启停任务 {@code POST /algorithmTask/setAlgorithmTaskStatus}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackAlgorithmTaskStatusRequest {

    private String taskNo;

    /** {@code 1} 启动，{@code 0} 停止；与中台校验一致。 */
    private byte taskStatus;
}
