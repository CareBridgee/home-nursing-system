package iti.jets.java.homenursing.controller;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import iti.jets.java.homenursing.dto.chat.ChatTurnResponse;
import iti.jets.java.homenursing.dto.chat.ReservationDraft;
import iti.jets.java.homenursing.exception.RateLimitException;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ChatDraftService;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.service.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit coverage for the private chat plumbing: the AI-call wrappers (advisor
 * lambdas, system draft instruction), provider-exception unwrapping edge cases,
 * rate-limit overflow and the remaining typed exception handlers.
 */
class ChatControllerTest {

    private final ChatClient chatClient = mock(ChatClient.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final ProfileService profileService = mock(ProfileService.class);
    private final ChatDraftService chatDraftService = mock(ChatDraftService.class);
    private final ChatMemory chatMemory = mock(ChatMemory.class);
    private final ChatController controller =
            new ChatController(chatClient, tokenService, profileService, chatDraftService, chatMemory);

    private static final UUID PROFILE_ID = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = ChatController.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            return method.invoke(controller, args);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        }
    }

    private void stubCooperativePrompt() {
        ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
        ChatClient.StreamResponseSpec stream = mock(ChatClient.StreamResponseSpec.class);
        when(chatClient.prompt()).thenReturn(spec);
        when(spec.advisors(any(Consumer.class))).thenAnswer(inv -> {
            inv.getArgument(0, Consumer.class).accept(mock(ChatClient.AdvisorSpec.class));
            return spec;
        });
        when(spec.toolContext(any())).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(call);
        when(call.content()).thenReturn("reply ok");
        when(spec.stream()).thenReturn(stream);
        when(stream.content()).thenReturn(Flux.just("token-a", "token-b"));
    }

    private ReservationDraft draftWithData() {
        return new ReservationDraft(UUID.randomUUID(), "Nursing", "Care at home", false);
    }

    @Test
    void callAi_invokesAdvisors_andSendsDraftInstruction() throws Exception {
        stubCooperativePrompt();
        String reply = (String) invoke("callAi",
                new Class<?>[]{String.class, String.class, UUID.class, ReservationDraft.class},
                PROFILE_ID.toString(), "hi", PROFILE_ID, draftWithData());
        assertThat(reply, is("reply ok"));
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(specOf()).system(captor.capture());
        assertThat(captor.getValue(), containsString("Current reservation draft state"));
    }

    @Test
    void callAi_withNullDraft_skipsSystemInstruction() throws Exception {
        stubCooperativePrompt();
        invoke("callAi",
                new Class<?>[]{String.class, String.class, UUID.class, ReservationDraft.class},
                PROFILE_ID.toString(), "hi", PROFILE_ID, null);
        verify(specOf(), org.mockito.Mockito.never()).system(anyString());
    }

    @Test
    void streamAi_invokesAdvisors_andSendsDraftInstruction() throws Exception {
        stubCooperativePrompt();
        Flux<String> flux = (Flux<String>) invoke("streamAi",
                new Class<?>[]{String.class, String.class, UUID.class, ReservationDraft.class},
                PROFILE_ID.toString(), "hi", PROFILE_ID, draftWithData());
        assertThat(flux.collectList().block(), is(java.util.List.of("token-a", "token-b")));
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(specOf()).system(captor.capture());
        assertThat(captor.getValue(), containsString("Current reservation draft state"));
    }

    @Test
    void unwrapProviderException_returnsMatchingProviderException() throws Exception {
        RuntimeException wrapped = new RuntimeException("Failed to generate content",
                new RuntimeException("inner", new RuntimeException("deep")));
        RuntimeException unwrapped = (RuntimeException) invoke("unwrapProviderException",
                new Class<?>[]{Throwable.class}, wrapped);
        assertThat(unwrapped, is(instanceOf(RuntimeException.class)));
        assertThat(unwrapped.getMessage(), is("Failed to generate content"));
    }

    @Test
    void unwrapProviderException_wrapsCheckedExceptions() throws Exception {
        Exception checked = new Exception("plain checked");
        RuntimeException unwrapped = (RuntimeException) invoke("unwrapProviderException",
                new Class<?>[]{Throwable.class}, checked);
        assertThat(unwrapped, is(instanceOf(RuntimeException.class)));
        assertThat(unwrapped.getMessage(), is("plain checked"));
    }

    @Test
    void unwrapProviderException_extractsApiExceptionFromChain() throws Exception {
        ApiException api = new ApiException(500, "INTERNAL", "provider down");
        RuntimeException wrapped = new RuntimeException("Failed to generate content", api);
        RuntimeException unwrapped = (RuntimeException) invoke("unwrapProviderException",
                new Class<?>[]{Throwable.class}, wrapped);
        assertThat(unwrapped, is(api));
    }

    @Test
    void draftInstruction_withDataButNoServiceType_marksItNotSet() throws Exception {
        ReservationDraft draft = new ReservationDraft(null, "Nursing", "Care at home", false);
        String instruction = (String) invoke("draftInstruction",
                new Class<?>[]{ReservationDraft.class}, draft);
        assertThat(instruction, containsString("serviceTypeId=not set"));
    }

    @Test
    void checkRateLimit_whenExceedingBudget_throwsRateLimitException() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(PROFILE_ID, null));
        Field max = ChatController.class.getDeclaredField("rateLimitMaxRequests");
        max.setAccessible(true);
        max.setLong(controller, 20L);
        when(tokenService.increment("chat:" + PROFILE_ID)).thenReturn(21L);

        assertThrows(RateLimitException.class, () -> invoke("checkRateLimit", new Class<?>[]{}));
    }

    @Test
    void buildResponse_withUrgentDraftAndNoLevel_defaultsToHospitalization() throws Exception {
        when(chatDraftService.getDraft(PROFILE_ID)).thenReturn(draftWithData());
        when(chatDraftService.isUrgent(PROFILE_ID)).thenReturn(true);
        when(chatDraftService.urgencyLevel(PROFILE_ID)).thenReturn(null);

        ChatTurnResponse response = (ChatTurnResponse) invoke("buildResponse",
                new Class<?>[]{UUID.class, String.class}, PROFILE_ID, "urgent reply");
        assertThat(response.messageType().name(), is("URGENT"));
        assertThat(response.urgency().level(), is("HOSPITALIZATION"));
    }

    @Test
    void handleAiServiceError_returns503() {
        ResponseEntity<Map<String, String>> response =
                controller.handleAiServiceError(new ApiException(500, "INTERNAL", "provider down"));
        assertThat(response.getStatusCode(), is(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(response.getBody().get("error"), is("AI_SERVICE_UNAVAILABLE"));
    }

    @Test
    void handleAiNetworkError_returns503() {
        ResponseEntity<Map<String, String>> response =
                controller.handleAiNetworkError(new GenAiIOException("network down"));
        assertThat(response.getStatusCode(), is(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(response.getBody().get("error"), is("AI_SERVICE_UNAVAILABLE"));
    }

    private ChatClient.ChatClientRequestSpec specOf() {
        return chatClient.prompt();
    }
}