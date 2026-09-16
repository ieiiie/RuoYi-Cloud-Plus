package com.ym.agriculture.farming.uav.model.vo;

import com.ym.agriculture.farming.uav.model.entity.SfUavMediaFile;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/** 已保存的历史影像元数据；不向旧平台查询、下载或刷新地址。 */
@Data
@AutoMapper(target = SfUavMediaFile.class)
public class SfUavMediaFileVo implements Serializable {
    private Long uavMediaId;
    private String fileName;
    private String fileType;
    private String objectKey;
    private String filePath;
    private String lat;
    private String lng;
    private Date fileCreateTime;
}
