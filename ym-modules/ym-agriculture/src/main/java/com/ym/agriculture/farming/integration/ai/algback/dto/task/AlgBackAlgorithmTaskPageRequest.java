package com.ym.agriculture.farming.integration.ai.algback.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务分页 {@code POST /algorithmTask/getAlgorithmTaskListPageVo}。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackAlgorithmTaskPageRequest {

    private int pageNum;

    private int pageSize;

    private String searchKey;

    /** 任务运行状态 0/1。 */
    private Byte status;

    private String taskNo;

    private String modelNo;

    /**
     * 客户号；中台 DTO 字段名为 {@code customNo}（非 customerNo），JSON 须与此一致。
     */
    private String customNo;
}
