package com.ym.agriculture.farming.dubbo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ym.agriculture.farming.field.dao.SfFieldIotMapper;
import com.ym.common.core.exception.ServiceException;

import org.junit.jupiter.api.*;

@Tag("dev")
class RemoteFieldDeviceBindingServiceTest {
    @Test
    void returnsCountOnlyAndDoesNotMutateAnyBinding() {
        var mapper = mock(SfFieldIotMapper.class);
        when(mapper.countAllActiveBindings("SN10")).thenReturn(3L);
        var service = new RemoteFieldDeviceBindingServiceImpl(mapper);
        assertEquals(3L, service.countActiveBindings("SN10"));
        verify(mapper).countAllActiveBindings("SN10");
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void missingCodeAndStorageFailureAreNotReportedAsZeroBindings() {
        var mapper = mock(SfFieldIotMapper.class);
        var service = new RemoteFieldDeviceBindingServiceImpl(mapper);
        for (String code : new String[] {null, "", " ", "x".repeat(129)})
            assertThrows(ServiceException.class, () -> service.countActiveBindings(code));
        verifyNoInteractions(mapper);
        when(mapper.countAllActiveBindings("SN10"))
                .thenThrow(new IllegalStateException("storage unavailable"));
        assertThrows(IllegalStateException.class, () -> service.countActiveBindings("SN10"));
    }
}
