package com.ym.resource.api.domain;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

@Data
public class RemoteFileUploadFailVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private String fileName;
    private String message;
}
