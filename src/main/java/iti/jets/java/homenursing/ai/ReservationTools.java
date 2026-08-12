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
            Records the service the user chooses for their reservation.
            Allowed fields: serviceTypeId (the exact UUID from listServiceTypes)
            and careDescription (a short description of the care needed or the medical
            situation, in the user's own words).
            Use this whenever the user picks a service or describes their care needs.
            """)
    public String updateReservationDraft(
            @ToolParam(description = "One of: serviceTypeId, careDescription") String field,
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

    @Tool(description = "Clears the reservation draft the user no longer wants. "
            + "scope=service clears only the chosen service (keeps any urgency flag). "
            + "scope=all clears the entire draft and any urgency flag. "
            + "Use scope=service when the user changes their mind about the service but may still book. "
            + "Use scope=all when the user abandons booking entirely or indicates reported symptoms were not real.")
    public String resetDraft(
            @ToolParam(description = "service = clear only the chosen service; all = clear the whole draft and urgency") DraftResetScope scope,
            ToolContext context) {
        UUID profileId = resolveProfileId(context);
        if (scope == DraftResetScope.ALL) {
            chatDraftService.reset(profileId);
            return "Booking draft and urgency cleared.";
        }
        chatDraftService.clearServiceType(profileId);
        return "Service choice cleared. Ask the user what service they would like instead.";
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
                + ", serviceTypeName=" + nullSafe(draft.serviceTypeName())
                + ", careDescription=" + nullSafe(draft.careDescription())
                + ", complete=" + draft.complete();
    }

    private static String nullSafe(Object value) {
        return value == null ? "not set" : String.valueOf(value);
    }
}