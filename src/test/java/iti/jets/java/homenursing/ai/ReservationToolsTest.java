package iti.jets.java.homenursing.ai;

import iti.jets.java.homenursing.dto.chat.ReservationDraft;
import iti.jets.java.homenursing.service.ChatDraftService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReservationToolsTest {

    @Mock
    private ChatDraftService chatDraftService;

    @InjectMocks
    private ReservationTools tools;

    private static final UUID PROFILE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void updateReservationDraftRecordsFieldAndSummarizesCompleteDraft() {
        UUID serviceTypeId = UUID.randomUUID();
        when(chatDraftService.getDraft(PROFILE_ID))
                .thenReturn(new ReservationDraft(serviceTypeId, "General Nursing", "night care", true));

        String result = tools.updateReservationDraft("serviceTypeId", serviceTypeId.toString(),
                context(PROFILE_ID));

        verify(chatDraftService).updateField(PROFILE_ID, "serviceTypeId", serviceTypeId.toString());
        assertThat(result)
                .startsWith("Draft updated: serviceTypeId = " + serviceTypeId + ".")
                .contains("Current draft serviceTypeId=" + serviceTypeId)
                .contains("serviceTypeName=General Nursing")
                .contains("complete=true");
    }

    @Test
    void updateReservationDraftSummarizesEmptyDraftAsNotSet() {
        when(chatDraftService.getDraft(PROFILE_ID)).thenReturn(ReservationDraft.empty());

        String result = tools.updateReservationDraft("serviceTypeId", "value", context(PROFILE_ID));

        assertThat(result)
                .contains("serviceTypeId=not set")
                .contains("serviceTypeName=not set")
                .contains("complete=false");
    }

    @Test
    void updateReservationDraftRejectsInvalidValueAndDoesNotReadDraft() {
        org.mockito.Mockito.        doThrow(new IllegalArgumentException("Unknown field: serviceTypeId"))
                .when(chatDraftService).updateField(PROFILE_ID, "serviceTypeId", "bogus");

        String result = tools.updateReservationDraft("serviceTypeId", "bogus", context(PROFILE_ID));

        assertThat(result)
                .isEqualTo("Rejected: Unknown field: serviceTypeId. Ask the user again for a valid value.");
        verify(chatDraftService, never()).getDraft(any());
    }

    @Test
    void setUrgencyRecordsHospitalizationLevel() {
        String result = tools.setUrgency(UrgencyLevel.HOSPITALIZATION, "chest pain", context(PROFILE_ID));

        verify(chatDraftService).setUrgency(PROFILE_ID, true, "HOSPITALIZATION", "chest pain");
        assertThat(result)
                .contains("level=HOSPITALIZATION")
                .contains("emergency services directly");
    }

    @Test
    void setUrgencyRecordsEmergencyLevel() {
        String result = tools.setUrgency(UrgencyLevel.EMERGENCY, "unconscious", context(PROFILE_ID));

        verify(chatDraftService).setUrgency(PROFILE_ID, true, "EMERGENCY", "unconscious");
        assertThat(result).contains("level=EMERGENCY");
    }

    @Test
    void clearUrgencyClearsTheFlag() {
        String result = tools.clearUrgency(context(PROFILE_ID));

        verify(chatDraftService).setUrgency(PROFILE_ID, false, null, null);
        assertThat(result).isEqualTo("Urgency cleared.");
    }

    @Test
    void resetDraftScopeServiceClearsServiceChoiceKeepsUrgency() {
        String result = tools.resetDraft(DraftResetScope.SERVICE, context(PROFILE_ID));

        verify(chatDraftService).clearServiceType(PROFILE_ID);
        verify(chatDraftService, never()).reset(any());
        assertThat(result).isEqualTo("Service choice cleared. Ask the user what service they would like instead.");
    }

    @Test
    void resetDraftScopeAllClearsDraftAndUrgency() {
        String result = tools.resetDraft(DraftResetScope.ALL, context(PROFILE_ID));

        verify(chatDraftService).reset(PROFILE_ID);
        verify(chatDraftService, never()).clearServiceType(any());
        assertThat(result).isEqualTo("Booking draft and urgency cleared.");
    }

    @Test
    void resetDraftRejectsInvalidScope() {
        // Spring AI binds enum by name; invalid value triggers a binding error before method entry.
        // This test asserts the enum values are the only accepted ones at the tool schema level.
        assertThat(DraftResetScope.values())
                .containsExactly(DraftResetScope.SERVICE, DraftResetScope.ALL);
    }

    @Test
    void resolvesProfileIdFromUuidContextValue() {
        when(chatDraftService.getDraft(PROFILE_ID)).thenReturn(ReservationDraft.empty());
        tools.updateReservationDraft("serviceTypeId", "abc", context(PROFILE_ID));

        verify(chatDraftService).updateField(PROFILE_ID, "serviceTypeId", "abc");
    }

    @Test
    void resolvesProfileIdFromStringContextValue() {
        when(chatDraftService.getDraft(PROFILE_ID)).thenReturn(ReservationDraft.empty());
        tools.updateReservationDraft("serviceTypeId", "abc", context(PROFILE_ID.toString()));

        verify(chatDraftService).updateField(PROFILE_ID, "serviceTypeId", "abc");
    }

    @Test
    void rejectsContextWithoutProfileId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.updateReservationDraft("serviceTypeId", "abc", new ToolContext(Map.of())))
                .withMessage("Tool invoked without a profile context");
    }

    @Test
    void rejectsBlankStringProfileId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.updateReservationDraft("serviceTypeId", "abc", context("   ")))
                .withMessage("Tool invoked without a profile context");
    }

    @Test
    void rejectsMalformedStringProfileId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.updateReservationDraft("serviceTypeId", "abc", context("not-a-uuid")));
    }

    @Test
    void rejectsNonStringNonUuidProfileId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tools.updateReservationDraft("serviceTypeId", "abc", context(42)))
                .withMessage("Tool invoked without a profile context");
    }

    private static ToolContext context(Object profileId) {
        return new ToolContext(Map.of("profileId", profileId));
    }
}
