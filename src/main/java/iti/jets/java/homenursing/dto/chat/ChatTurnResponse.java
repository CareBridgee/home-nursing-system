package iti.jets.java.homenursing.dto.chat;

public record ChatTurnResponse(
        ChatMessageType messageType,
        String reply,
        ReservationDraft draft,
        UrgencySignal urgency
) {
    public static ChatTurnResponse text(String reply) {
        return new ChatTurnResponse(ChatMessageType.TEXT, reply, null, null);
    }

    public static ChatTurnResponse error(String reply) {
        return new ChatTurnResponse(ChatMessageType.ERROR, reply, null, null);
    }
}