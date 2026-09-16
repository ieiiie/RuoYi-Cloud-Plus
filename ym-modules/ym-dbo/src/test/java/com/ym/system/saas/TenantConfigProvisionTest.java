package com.ym.system.saas;

import com.ym.system.saas.domain.SaasConfigDefinition;
import com.ym.system.saas.mapper.SaasConfigDefinitionMapper;
import com.ym.system.saas.mapper.SaasTenantConfigMapper;
import com.ym.system.saas.service.SaasTenantProvisionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.Tag("dev")
class TenantConfigProvisionTest {
    @Test void republishingPreservesExistingTenantValue() {
        var service = mock(SaasTenantProvisionService.class, CALLS_REAL_METHODS);
        var configs = mock(SaasTenantConfigMapper.class);
        ReflectionTestUtils.setField(service, "configMapper", configs);
        when(configs.selectCount(any())).thenReturn(1L);
        var definition = new SaasConfigDefinition();
        definition.setDefinitionId(123L);
        definition.setDefaultValue("changed-platform-default");
        ReflectionTestUtils.invokeMethod(service, "insertConfigValue", "658226", definition, null, null);
        verify(configs).selectCount(any());
        verifyNoMoreInteractions(configs);
    }

    @Test void disabledDefinitionIsNotIssued() {
        var service = mock(SaasTenantProvisionService.class, CALLS_REAL_METHODS);
        var definitions = mock(SaasConfigDefinitionMapper.class);
        ReflectionTestUtils.setField(service, "definitionMapper", definitions);
        var definition = new SaasConfigDefinition();
        definition.setStatus("1");
        when(definitions.selectById(123L)).thenReturn(definition);
        service.issueDefinition(123L);
        verify(definitions).selectById(123L);
        verifyNoMoreInteractions(definitions);
    }
}
