package com.ym.agriculture.farming.algback.model.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 算法中台任务启停请求体。
 *
 * @author ym-cloud
 */
@Data
public class AlgBackTaskStatusBo {

    /**
     * 算法中台任务号
     */
    @NotBlank
    private String taskNo;

    /**
     * 任务状态：1 启动，0 停止
     */
    private byte taskStatus;
}
