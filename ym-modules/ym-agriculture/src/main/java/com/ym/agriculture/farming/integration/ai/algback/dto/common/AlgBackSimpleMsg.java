package com.ym.agriculture.farming.integration.ai.algback.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 写接口成功时 {@code data} 仅含 {@code msg}。
 *
 * @author ym-cloud
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgBackSimpleMsg {

    private String msg;
}
