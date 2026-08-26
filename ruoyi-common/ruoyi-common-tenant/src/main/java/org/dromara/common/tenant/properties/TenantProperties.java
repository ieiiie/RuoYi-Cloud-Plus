package org.dromara.common.tenant.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 租户配置属性。
 *
 * @author Lion Li
 */
@Data
@ConfigurationProperties(prefix = "tenant")
public class TenantProperties {

    /**
     * 是否启用租户隔离。
     */
    private Boolean enable;

    /**
     * 不参与行级租户隔离的表。
     */
    private List<String> excludes;

}
