package com.ym.agriculture.farming.algback.service;

import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAddTaskResult;
import com.ym.agriculture.farming.algback.model.bo.AlgBackTaskSubmitBo;
import com.ym.agriculture.farming.algback.model.bo.AlgBackTaskStatusBo;

/**
 * 调用算法中台创建/启停任务（依赖 {@code ym.alg-back} 已配置且 token 有效）。
 */
public interface ISfAlgBackAlgorithmTaskService {

    AlgBackAddTaskResult submitAndOptionallyStart(AlgBackTaskSubmitBo bo);


    void setTaskStatus(AlgBackTaskStatusBo bo);
}
