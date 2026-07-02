package com.stowflow.inventra.dto;

import java.util.List;

public final class ChatDtos {

    public record ChatRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 2000) String message
    ) {}

    /** Navigation or UI action (option C — safe). */
    public record ChatAction(
            String type,
            String label,
            String href
    ) {}

    /** Write action requiring front-end confirmation before API call. */
    public record ConfirmAction(
            String type,
            String label,
            java.util.Map<String, Object> payload
    ) {}

    public record ChatResponse(
            String reply,
            List<String> suggestions,
            List<ChatAction> actions,
            ConfirmAction confirmAction
    ) {
        public static ChatResponse text(String reply) {
            return new ChatResponse(reply, List.of(), List.of(), null);
        }

        public static ChatResponse of(
                String reply,
                List<String> suggestions,
                List<ChatAction> actions,
                ConfirmAction confirmAction
        ) {
            return new ChatResponse(
                    reply,
                    suggestions != null ? suggestions : List.of(),
                    actions != null ? actions : List.of(),
                    confirmAction);
        }
    }

    public record ChatBootstrapResponse(
            String welcome,
            List<String> suggestions
    ) {}

    private ChatDtos() {}
}
