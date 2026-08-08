package iti.jets.java.homenursing.dto.ws;

import jakarta.validation.constraints.NotBlank;

public record ChatSendPayload(
        @NotBlank(message = "Message content is required")
        String content
) {
}