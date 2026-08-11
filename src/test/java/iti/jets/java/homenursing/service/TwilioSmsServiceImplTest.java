package iti.jets.java.homenursing.service;

import com.twilio.exception.ApiException;
import iti.jets.java.homenursing.service.impl.TwilioSmsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TwilioSmsServiceImplTest {

    @Mock
    private Logger logger;

    private TwilioSmsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TwilioSmsServiceImpl();
        ReflectionTestUtils.setField(service, "accountSid", "AC-sid");
        ReflectionTestUtils.setField(service, "authToken", "token");
        ReflectionTestUtils.setField(service, "fromNumber", "+15005550006");
    }

    @Test
    void init_withAllCreds_marksConfigured() {
        service.init();

        assertEquals(true, ReflectionTestUtils.getField(service, "configured"));
    }

    @Test
    void init_withBlankAccountSid_staysUnconfigured() {
        ReflectionTestUtils.setField(service, "accountSid", "  ");

        service.init();

        assertEquals(false, ReflectionTestUtils.getField(service, "configured"));
    }

    @Test
    void init_withBlankAuthToken_staysUnconfigured() {
        ReflectionTestUtils.setField(service, "authToken", "  ");

        service.init();

        assertEquals(false, ReflectionTestUtils.getField(service, "configured"));
    }

    @Test
    void init_withBlankFromNumber_staysUnconfigured() {
        ReflectionTestUtils.setField(service, "fromNumber", "");

        service.init();

        assertEquals(false, ReflectionTestUtils.getField(service, "configured"));
    }

    @Test
    void sendOtp_notConfigured_logsDevOtp() {
        ReflectionTestUtils.setField(service, "configured", false);

        service.sendOtp("+201000000000", "123456");
    }

    @Test
    void sendOtp_configured_logsMessage() {
        ReflectionTestUtils.setField(service, "configured", true);

        service.sendOtp("+201000000000", "123456");
    }

    @Test
    void sendOtp_twilioFailure_wrapsInRuntime() {
        ReflectionTestUtils.setField(service, "configured", true);
        replaceStaticLogger();
        doThrow(new ApiException("twilio down"))
                .when(logger).info(anyString(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.sendOtp("+201000000000", "123456"));

        assertEquals("Failed to send SMS: twilio down", ex.getMessage());
        verify(logger).error(anyString(), eq("+201000000000"), eq("twilio down"));
    }

    private void replaceStaticLogger() {
        try {
            Field field = TwilioSmsServiceImpl.class.getDeclaredField("log");
            sun.misc.Unsafe unsafe;
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), logger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}