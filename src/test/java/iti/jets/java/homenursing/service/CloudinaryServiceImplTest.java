package iti.jets.java.homenursing.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import iti.jets.java.homenursing.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    private CloudinaryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CloudinaryServiceImpl();
        ReflectionTestUtils.setField(service, "cloudName", "demo");
        ReflectionTestUtils.setField(service, "apiKey", "key");
        ReflectionTestUtils.setField(service, "apiSecret", "secret");
    }

    private void configureSdk() {
        ReflectionTestUtils.setField(service, "cloudinary", cloudinary);
        ReflectionTestUtils.setField(service, "configured", true);
        when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void init_withAllCreds_buildsConfiguredInstance() {
        service.init();

        assertEquals(true, ReflectionTestUtils.getField(service, "configured"));
    }

    @Test
    void init_withBlankCreds_staysUnconfigured() {
        ReflectionTestUtils.setField(service, "cloudName", "   ");

        service.init();

        assertEquals(false, ReflectionTestUtils.getField(service, "configured"));
    }

    @Test
    void init_withBlankApiKey_staysUnconfigured() {
        ReflectionTestUtils.setField(service, "apiKey", "  ");

        service.init();

        assertEquals(false, ReflectionTestUtils.getField(service, "configured"));
    }

    @Test
    void init_withBlankApiSecret_staysUnconfigured() {
        ReflectionTestUtils.setField(service, "apiSecret", "");

        service.init();

        assertEquals(false, ReflectionTestUtils.getField(service, "configured"));
    }

    @Test
    void upload_delegatesWithEmptyOptions_returnsSecureUrl() throws IOException {
        configureSdk();
        when(uploader.upload(any(byte[].class), eq(Map.of())))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/a.png"));

        String url = service.upload(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2}));

        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/a.png", url);
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(uploader).upload(bytesCaptor.capture(), eq(Map.of()));
        assertArrayEquals(new byte[]{1, 2}, bytesCaptor.getValue());
    }

    @Test
    void upload_notConfigured_throwsRuntime() {
        ReflectionTestUtils.setField(service, "configured", false);

        assertThrows(RuntimeException.class,
                () -> service.upload(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1})));
    }

    @Test
    void upload_ioFailure_throwsRuntime() throws IOException {
        configureSdk();
        when(uploader.upload(any(byte[].class), eq(Map.of()))).thenThrow(new IOException("boom"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.upload(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1})));

        assertEquals("File upload failed: boom", ex.getMessage());
    }

    @Test
    void delete_nullOrBlankUrl_returnsEarly() throws IOException {
        configureSdk();
        service.delete(null);
        service.delete("  ");
        verify(uploader, never()).destroy(anyString(), any());
    }

    @Test
    void delete_nonCloudinaryHost_skipsDeletion() throws IOException {
        configureSdk();
        service.delete("https://example.com/images/a.png");
        verify(uploader, never()).destroy(anyString(), any());
    }

    @Test
    void delete_hostlessUrl_skipsDeletion() throws IOException {
        configureSdk();
        service.delete("images/a.png");
        verify(uploader, never()).destroy(anyString(), any());
    }

    @Test
    void delete_validCloudinaryUrl_destroysPublicId() throws IOException {
        configureSdk();
        service.delete("https://res.cloudinary.com/demo/image/upload/v123/images/photo.jpg");

        verify(uploader).destroy(eq("images/photo"), eq(Map.of("resource_type", "image", "invalidate", true)));
    }

    @Test
    void delete_validCloudinaryUrlWithoutVersion_stillDestroys() throws IOException {
        configureSdk();
        service.delete("https://res.cloudinary.com/demo/image/upload/images/photo.jpg");

        verify(uploader).destroy(eq("images/photo"), any(Map.class));
    }

    @Test
    void delete_extensionlessPath_doesNotStripSuffix() throws IOException {
        configureSdk();
        service.delete("https://res.cloudinary.com/demo/image/upload/images/photo");

        verify(uploader).destroy(eq("images/photo"), any(Map.class));
    }

    @Test
    void delete_noUploadMarker_throwsIllegalArgument() throws IOException {
        configureSdk();
        assertThrows(IllegalArgumentException.class,
                () -> service.delete("https://res.cloudinary.com/demo/image/photo.jpg"));
    }

    @Test
    void delete_ioFailure_throwsRuntime() throws IOException {
        configureSdk();
        when(uploader.destroy(anyString(), any(Map.class))).thenThrow(new IOException("boom"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.delete("https://res.cloudinary.com/demo/image/upload/v1/a.png"));

        assertEquals("File deletion failed: boom", ex.getMessage());
    }

    @Test
    void delete_notConfigured_throwsRuntime() throws IOException {
        ReflectionTestUtils.setField(service, "configured", false);

        assertThrows(RuntimeException.class,
                () -> service.delete("https://res.cloudinary.com/demo/image/upload/v1/a.png"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://res.cloudinary.com/demo/image/upload/v123/a%20b/c.png",
            "https://res.cloudinary.com/demo/image/upload/a-b-c.png"
    })
    void delete_urlDecodesAndHandlesDashes(String url) throws IOException {
        configureSdk();
        service.delete(url);
        verify(uploader).destroy(anyString(), any(Map.class));
    }
}
