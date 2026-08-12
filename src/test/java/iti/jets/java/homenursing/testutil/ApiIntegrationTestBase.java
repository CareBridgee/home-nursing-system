package iti.jets.java.homenursing.testutil;

import iti.jets.java.homenursing.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Base for HTTP-level integration tests (Phase 3): full context + MockMvc.
 * Cloudinary is mocked (empty creds in the test profile - external vendor client,
 * see .idea/TESTING.md Phase 0/2.9): all uploads resolve to a fixed test URL.
 * The {@code reportChatClient} bean (Gemini-backed) is mocked too so profile
 * report generation runs offline.
 */
@AutoConfigureMockMvc
public abstract class ApiIntegrationTestBase extends BaseIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected CloudinaryService cloudinaryService;

    @MockitoBean(name = "reportChatClient")
    protected ChatClient reportChatClient;

    @BeforeEach
    void stubCloudinary() {
        when(cloudinaryService.upload(any(MultipartFile.class)))
                .thenReturn("https://res.cloudinary.test/" + UUID.randomUUID() + ".jpg");
    }

    @BeforeEach
    void stubReportChatClient() {
        ChatClient.ChatClientRequestSpec spec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec call = Mockito.mock(ChatClient.CallResponseSpec.class);
        when(reportChatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.content()).thenReturn(
                "REPORT\n1. Patient Overview\n- Name: Test Patient\n- Gender: FEMALE\n" +
                "2. Medical History\n- Not recorded\n3. Allergies\n- Not recorded\n" +
                "4. Current Medications\n- Not recorded\n5. Care Considerations\n- Not recorded\n" +
                "6. Risk Flags\n- None recorded");
    }

    protected static MockMultipartFile image(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    protected String bearer(DevOtpAuth.Tokens tokens) {
        return "Bearer " + tokens.accessToken();
    }
}