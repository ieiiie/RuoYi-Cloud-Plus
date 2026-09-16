package com.ym.agriculture.farming.news.model.bo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 农业资讯下架入参。 */
@Data
public class SfNewsOfflineBo {

    /** 下架原因。 */
    @Size(max = 500, message = "下架原因不能超过500个字符")
    private String comment;
}
