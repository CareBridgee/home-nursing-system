package iti.jets.java.homenursing.controller;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.GenAiIOException;
import iti.jets.java.homenursing.dto.chat.ChatMessageType;
import iti.jets.java.homenursing.dto.chat.ChatTurnResponse;
import iti.jets.java.homenursing.dto.chat.ReservationDraft;
import iti.jets.java.homenursing.dto.chat.UrgencySignal;
import iti.jets.java.homenursing.exception.RateLimitException;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ChatDraftService;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String AI_SERVICE_UNAVAILABLE_MESSAGE =
            "I'm sorry, I couldn't process that request right now. Please try rephrasing your question or ask something else.";

    private static final String URGENCY_ADVICE =
            "If this is a medical emergency, please call emergency services (123) or go to the nearest hospital immediately. "
                    + "The platform is not a substitute for emergency medical care.";

    private final ChatClient chatClient;
    private final TokenService tokenService;
    private final ProfileService profileService;
    private final ChatDraftService chatDraftService;
    private final ChatMemory chatMemory;

    @Value("${chat.rate-limit.window-seconds:300}")
    private long rateLimitWindowSeconds;

    @Value("${chat.rate-limit.max-requests:20}")
    private long rateLimitMaxRequests;

    public ChatController(ChatClient chatClient, TokenService tokenService,
                          ProfileService profileService, ChatDraftService chatDraftService,
                          ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.tokenService = tokenService;
        this.profileService = profileService;
        this.chatDraftService = chatDraftService;
        this.chatMemory = chatMemory;
    }

    public record ChatRequest(
            @NotNull(message = "profileId must not be null")
            UUID profileId,
            @NotBlank(message = "message must not be empty")
            @Size(max = 2000, message = "message must not exceed 2000 characters")
            String message) {}

    public record ResetRequest(
            @NotNull(message = "profileId must not be null")
            UUID profileId) {}

    @PostMapping
    public ChatTurnResponse chat(@Valid @RequestBody ChatRequest request) {
        checkRateLimit();

        UUID profileId = ownedProfileId(request.profileId());
        String conversationId = profileId.toString();

        for (int attempt = 0; ; attempt++) {
            try {
                ReservationDraft draftBefore = chatDraftService.getDraft(profileId);
                String reply = callAi(conversationId, request.message(), profileId, draftBefore);
                return buildResponse(profileId, reply);
            } catch (IllegalStateException ex) {
                if (attempt >= 2) {
                    log.warn("AI call failed after {} attempts: {}", attempt + 1, ex.getMessage());
                    throw ex;
                }
                log.warn("AI call failed, retrying (attempt {}): {}", attempt + 1, ex.getMessage());
            }
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest request) {
        UUID profileId = ownedProfileId(request.profileId());
        chatDraftService.reset(profileId);
        chatMemory.clear(profileId.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        checkRateLimit();

        UUID profileId = ownedProfileId(request.profileId());
        String conversationId = profileId.toString();
        ReservationDraft draftBefore = chatDraftService.getDraft(profileId);

        return Flux.defer(() -> streamAi(conversationId, request.message(), profileId, draftBefore))
                .retryWhen(Retry.max(2).filter(e -> e instanceof IllegalStateException)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorResume(e -> {
                    log.warn("AI stream failed: {}", e.getMessage());
                    return Flux.just("AI_SERVICE_UNAVAILABLE: " + AI_SERVICE_UNAVAILABLE_MESSAGE);
                });
    }

    private ChatTurnResponse buildResponse(UUID profileId, String reply) {
        ReservationDraft draft = chatDraftService.getDraft(profileId);

        if (chatDraftService.isUrgent(profileId)) {
            String level = chatDraftService.urgencyLevel(profileId);
            UrgencySignal urgency = new UrgencySignal(true, level == null ? "HOSPITALIZATION" : level, URGENCY_ADVICE);
            return new ChatTurnResponse(ChatMessageType.URGENT, reply, draft, urgency);
        }
        if (draft.complete()) {
            return new ChatTurnResponse(ChatMessageType.CONFIRM, reply, draft, null);
        }
        if (draft.hasAnyData() || (reply != null && reply.trim().endsWith("?"))) {
            return new ChatTurnResponse(ChatMessageType.INPUT, reply, draft, null);
        }
        return new ChatTurnResponse(ChatMessageType.TEXT, reply, null, null);
    }

    private UUID ownedProfileId(UUID profileId) {
        profileService.getOwnedProfileEntity(profileId, SecurityUtils.currentUserId());
        return profileId;
    }

    private String callAi(String conversationId, String message, UUID profileId, ReservationDraft draftBefore) {
        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("profileId", profileId));
        String draftInfo = draftInstruction(draftBefore);
        if (!draftInfo.isBlank()) {
            prompt.system(draftInfo);
        }
        try {
            return prompt.user(message).call().content();
        } catch (RuntimeException ex) {
            throw unwrapProviderException(ex);
        }
    }

    private Flux<String> streamAi(String conversationId, String message, UUID profileId, ReservationDraft draftBefore) {
        ChatClient.ChatClientRequestSpec prompt = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("profileId", profileId));
        String draftInfo = draftInstruction(draftBefore);
        if (!draftInfo.isBlank()) {
            prompt.system(draftInfo);
        }
        return prompt.user(message).stream().content()
                .onErrorMap(e -> unwrapProviderException(e)); 
    }

    /**
     * Google GenAI (and the OpenAI-compatible path) wrap provider failures in a
     * generic RuntimeException ("Failed to generate content"). Unwrap the cause
     * chain so the real provider error surfaces to the typed handlers below.
     */
    private RuntimeException unwrapProviderException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ApiException || current instanceof ClientException
                    || current instanceof GenAiIOException) {
                return (RuntimeException) current;
            }
            Throwable next = current.getCause();
            if (next == current) break;
            current = next;
        }
        return ex instanceof RuntimeException re ? re : new RuntimeException(ex.getMessage(), ex);
    }

    private String draftInstruction(ReservationDraft draft) {
        if (draft == null || !draft.hasAnyData()) {
            return "";
        }
        return "Current reservation draft state (do not repeat these back; only collect what is missing): "
                + "serviceTypeId=" + (draft.serviceTypeId() == null ? "not set" : draft.serviceTypeId())
                + ", preferredDate=" + (draft.preferredDate() == null ? "not set" : draft.preferredDate())
                + ", preferredTime=" + (draft.preferredTime() == null ? "not set" : draft.preferredTime())
                + ", complete=" + draft.complete();
    }

    private void checkRateLimit() {
        String key = "chat:" + SecurityUtils.currentUserId();
        Long count = tokenService.increment(key);
        if (count != null && count == 1) {
            tokenService.expire(key, Duration.ofSeconds(rateLimitWindowSeconds));
        }
        if (count != null && count > rateLimitMaxRequests) {
            throw new RateLimitException("Too many chat messages. Please try again in a few minutes.");
        }
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<Map<String, String>> handleAiToolCallError(ClientException ex) {
        log.warn("AI tool call rejected by provider: {}", ex.getMessage());
        if (ex.code() == 429) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "AI_SERVICE_UNAVAILABLE",
                            "message", "I'm sorry, I couldn't process that request right now. Please try again in a moment."
                    ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", AI_SERVICE_UNAVAILABLE_MESSAGE
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleAiFrameworkError(IllegalStateException ex) {
        log.warn("AI framework error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", AI_SERVICE_UNAVAILABLE_MESSAGE
                ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleAiServiceError(ApiException ex) {
        log.warn("AI service error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", "I'm sorry, I couldn't process that request right now. Please try again in a moment."
                ));
    }

    @ExceptionHandler(GenAiIOException.class)
    public ResponseEntity<Map<String, String>> handleAiNetworkError(GenAiIOException ex) {
        log.warn("AI network error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", "I'm sorry, I couldn't process that request right now. Please try again in a moment."
                ));
    }
}