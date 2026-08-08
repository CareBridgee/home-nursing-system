package iti.jets.java.homenursing.ai;

import iti.jets.java.homenursing.dto.chat.ReservationDraft;
import iti.jets.java.homenursing.service.ChatDraftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReservationTools {

    private static final Logger log = LoggerFactory.getLogger(ReservationTools.class);

    private final ChatDraftService chatDraftService;

    public ReservationTools(ChatDraftService chatDraftService) {
        this.chatDraftService = chatDraftService;
    }

    @Tool(description = """
            Records reservation details the user provides in natural language, one field at a time.
            Allowed fields: serviceTypeId (a UUID from listServiceTypes), preferredDate (yyyy-MM-dd),
            preferredTime (HH:mm). Use this whenever the user provides any of those.
            """)
    public String updateReservationDraft(
            @ToolParam(description = "One of: serviceTypeId, preferredDate, preferredTime") String field,
            @ToolParam(description = "The value for the given field") String value,
            ToolContext context) {
        UUID profileId = resolveProfileId(context);
        try {
            chatDraftService.updateField(profileId, field, value);
        } catch (IllegalArgumentException ex) {
            log.warn("Draft update rejected for profile {}: {}", profileId, ex.getMessage());
            return "Rejected: " + ex.getMessage() + ". Ask the user again for a valid value.";
        }
        ReservationDraft draft = chatDraftService.getDraft(profileId);
        return "Draft updated: " + field + " = " + value + ". "
                + summarize(draft, "Current draft");
    }

    @Tool(description = "Marks the user's case as urgent (requires hospitalization or immediate medical attention). "
            + "Use when the user describes a critical or life-threatening condition.")
    public String setUrgency(
            @ToolParam(description = "Urgency level, one of: HOSPITALIZATION, EMERGENCY") UrgencyLevel level,
            @ToolParam(description = "Short reason for urgent care") String reason,
            ToolContext context) {
        UUID profileId = resolveProfileId(context);
        chatDraftService.setUrgency(profileId, true, level.name(), reason);
        return "Urgency recorded (level=" + level.name() + "). Advise the user about emergency steps. "
                + "The platform does not send any external hospital notification; the user must contact "
                + "emergency services directly.";
    }

    @Tool(description = "Clears any urgency flag previously set for this session. Only use if the user "
            + "explicitly says the condition is no longer urgent.")
    public String clearUrgency(ToolContext context) {
        UUID profileId = resolveProfileId(context);
        chatDraftService.setUrgency(profileId, false, null, null);
        return "Urgency cleared.";
    }

    private static UUID resolveProfileId(ToolContext context) {
        Object value = context.getContext().get("profileId");
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text && !text.isBlank()) {
            return UUID.fromString(text);
        }
        throw new IllegalArgumentException("Tool invoked without a profile context");
    }

    private static String summarize(ReservationDraft draft, String heading) {
        return heading + " serviceTypeId=" + nullSafe(draft.serviceTypeId())
                + ", preferredDate=" + (draft.preferredDate() == null ? "not set" : draft.preferredDate())
                + ", preferredTime=" + (draft.preferredTime() == null ? "not set" : draft.preferredTime())
                + ", complete=" + draft.complete();
    }

    private static String nullSafe(Object value) {
        return value == null ? "not set" : String.valueOf(value);
    }
}