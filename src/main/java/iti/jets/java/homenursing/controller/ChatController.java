package iti.jets.java.homenursing.controller;

import com.openai.errors.BadRequestException;
import com.openai.errors.OpenAiException;
import iti.jets.java.homenursing.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public record ChatRequest(@NotBlank(message = "message must not be empty") String message) {}
    public record ChatResponse(String reply) {}

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String conversationId = SecurityUtils.currentUserId().toString();

        String reply = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(request.message())
                .call()
                .content();

        return new ChatResponse(reply);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleAiToolCallError(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", "I'm sorry, I couldn't process that request right now. Please try rephrasing your question or ask something else."
                ));
    }

    @ExceptionHandler(OpenAiException.class)
    public ResponseEntity<Map<String, String>> handleAiServiceError(OpenAiException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message", "I'm sorry, I couldn't process that request right now. Please try again in a moment."
                ));
    }
}