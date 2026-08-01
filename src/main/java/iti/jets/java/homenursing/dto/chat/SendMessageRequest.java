package iti.jets.java.homenursing.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
        @NotBlank(message = "Message content is required")
        String content
) {
}
