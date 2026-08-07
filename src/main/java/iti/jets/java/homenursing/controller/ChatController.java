package iti.jets.java.homenursing.controller;

import com.openai.errors.BadRequestException;
import com.openai.errors.OpenAIException;
import iti.jets.java.homenursing.exception.RateLimitException;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String AI_SERVICE_UNAVAILABLE_MESSAGE =
            "I'm sorry, I couldn't process that request right now. Please try rephrasing your question or ask something else.";

    private final ChatClient chatClient;
    private final TokenService tokenService;

    @Value("${chat.rate-limit.window-seconds:300}")
    private long rateLimitWindowSeconds;

    @Value("${chat.rate-limit.max-requests:20}")
    private long rateLimitMaxRequests;

    public ChatController(ChatClient chatClient, TokenService tokenService) {
        this.chatClient = chatClient;
        this.tokenService = tokenService;
    }

    public record ChatRequest(
            @NotBlank(message = "message must not be empty")
            @Size(max = 2000, message = "message must not exceed 2000 characters")
            String message) {}

    public record ChatResponse(String reply) {}

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        checkRateLimit();

        String conversationId = SecurityUtils.currentUserId().toString();

        for (int attempt = 0; ; attempt++) {
            try {
                return new ChatResponse(callAi(conversationId, request.message()));
            } catch (BadRequestException | IllegalStateException ex) {
                if (attempt >= 2) {
                    log.warn("AI call failed after {} attempts: {}", attempt + 1, ex.getMessage());
                    throw ex;
                }
                log.warn("AI call failed, retrying (attempt {}): {}", attempt + 1, ex.getMessage());
            }
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        checkRateLimit();

        String conversationId = SecurityUtils.currentUserId().toString();

        return Flux.defer(() -> streamAi(conversationId, request.message()))
                .retryWhen(Retry.max(2)
                        .filter(e -> e instanceof BadRequestException || e instanceof IllegalStateException)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorResume(e -> {
                    log.warn("AI stream failed: {}", e.getMessage());
                    return Flux.just("AI_SERVICE_UNAVAILABLE: " + AI_SERVICE_UNAVAILABLE_MESSAGE);
                });
    }

    private String callAi(String conversationId, String message) {
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .call()
                .content();
    }

    private Flux<String> streamAi(String conversationId, String message) {
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .stream()
                .content();
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

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleAiToolCallError(BadRequestException ex) {
        log.warn("AI tool call rejected by provider: {}", ex.getMessage());
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

    @ExceptionHandler(OpenAIException.class)
    public ResponseEntity<Map<String, String>> handleAiServiceError(OpenAIException ex) {
        log.warn("AI service error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", "I'm sorry, I couldn't process that request right now. Please try again in a moment."
                ));
    }
}
