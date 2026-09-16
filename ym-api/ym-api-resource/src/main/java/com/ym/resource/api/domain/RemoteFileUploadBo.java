package com.ym.resource.api.domain;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

@Data
public class RemoteFileUploadBo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private String requestId;
    private String businessId;
    private String fileName;
    private String contentType;
    private byte[] content;
}
