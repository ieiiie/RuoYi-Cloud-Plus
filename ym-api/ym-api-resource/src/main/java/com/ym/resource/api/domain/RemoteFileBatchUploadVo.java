package com.ym.resource.api.domain;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class RemoteFileBatchUploadVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private List<RemoteFileUploadVo> successList = new ArrayList<>();
    private List<RemoteFileUploadFailVo> failList = new ArrayList<>();
}
