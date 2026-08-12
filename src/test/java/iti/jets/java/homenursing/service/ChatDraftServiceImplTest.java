package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.chat.ReservationDraft;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.impl.ChatDraftServiceImpl;
import iti.jets.java.homenursing.util.ServiceBriefBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatDraftServiceImplTest {

    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final UUID SERVICE_TYPE_ID = UUID.randomUUID();

    @Mock
    private ServiceTypeRepository serviceTypeRepository;
    @Mock
    private ServiceBriefBuilder serviceBriefBuilder;

    private ChatDraftServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatDraftServiceImpl(serviceTypeRepository, serviceBriefBuilder);
    }

    private ServiceType serviceType() {
        return ServiceType.builder()
                .id(SERVICE_TYPE_ID)
                .name("Home Nursing")
                .basePrice(BigDecimal.valueOf(150))
                .build();
    }

    @Test
    void getDraft_noState_returnsEmptyDraft() {
        ReservationDraft draft = service.getDraft(PROFILE_ID);
        assertNull(draft.serviceTypeId());
        assertNull(draft.serviceTypeName());
        assertNull(draft.serviceDescription());
        assertFalse(draft.complete());
        assertFalse(draft.hasAnyData());
    }

    @Test
    void updateField_blankFieldOrValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.updateField(PROFILE_ID, null, "x"));
        assertThrows(IllegalArgumentException.class, () -> service.updateField(PROFILE_ID, "   ", "x"));
        assertThrows(IllegalArgumentException.class, () -> service.updateField(PROFILE_ID, "serviceTypeId", null));
        assertThrows(IllegalArgumentException.class, () -> service.updateField(PROFILE_ID, "serviceTypeId", "   "));
        verify(serviceTypeRepository, never()).findById(any());
    }

    @Test
    void updateField_serviceTypeId_setsFieldsAndBuildsDescription() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceBriefBuilder.build(eq(PROFILE_ID), eq("Home Nursing"))).thenReturn("A full nursing brief");

        service.updateField(PROFILE_ID, "serviceTypeId", SERVICE_TYPE_ID.toString());

        ReservationDraft draft = service.getDraft(PROFILE_ID);
        assertEquals(SERVICE_TYPE_ID, draft.serviceTypeId());
        assertEquals("Home Nursing", draft.serviceTypeName());
        assertEquals("A full nursing brief", draft.serviceDescription());
        assertTrue(draft.complete());
        assertTrue(draft.hasAnyData());
    }

    @Test
    void updateField_trimsValueBeforeParsing() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceBriefBuilder.build(any(), any())).thenReturn("brief");

        service.updateField(PROFILE_ID, "serviceTypeId", " " + SERVICE_TYPE_ID + " ");

        assertEquals(SERVICE_TYPE_ID, service.getDraft(PROFILE_ID).serviceTypeId());
        verify(serviceBriefBuilder).build(eq(PROFILE_ID), eq("Home Nursing"));
    }

    @Test
    void updateField_invalidUuid_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateField(PROFILE_ID, "serviceTypeId", "not-a-uuid"));
        assertEquals("Invalid service type id: not-a-uuid", ex.getMessage());
    }

    @Test
    void updateField_unknownServiceType_throwsIllegalArgumentException() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateField(PROFILE_ID, "serviceTypeId", SERVICE_TYPE_ID.toString()));
        assertEquals("Unknown service type id: " + SERVICE_TYPE_ID, ex.getMessage());
    }

    @Test
    void updateField_unknownField_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateField(PROFILE_ID, "nickname", "Momo"));
        assertEquals("Unknown draft field: nickname", ex.getMessage());
        verify(serviceTypeRepository, never()).findById(any());
    }

    @Test
    void updateField_serviceTypeId_overwritesPreviousSelection() {
        UUID secondTypeId = UUID.randomUUID();
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID))
                .thenReturn(Optional.of(ServiceType.builder().id(SERVICE_TYPE_ID).name("Home Nursing").build()));
        when(serviceTypeRepository.findById(secondTypeId))
                .thenReturn(Optional.of(ServiceType.builder().id(secondTypeId).name("Physiotherapy").build()));
        when(serviceBriefBuilder.build(eq(PROFILE_ID), eq("Home Nursing"))).thenReturn("first brief");
        when(serviceBriefBuilder.build(eq(PROFILE_ID), eq("Physiotherapy"))).thenReturn("second brief");

        service.updateField(PROFILE_ID, "serviceTypeId", SERVICE_TYPE_ID.toString());
        service.updateField(PROFILE_ID, "serviceTypeId", secondTypeId.toString());

        ReservationDraft draft = service.getDraft(PROFILE_ID);
        assertEquals(secondTypeId, draft.serviceTypeId());
        assertEquals("Physiotherapy", draft.serviceTypeName());
        assertEquals("second brief", draft.serviceDescription());
    }

    @Test
    void isUrgent_noState_false() {
        assertFalse(service.isUrgent(PROFILE_ID));
    }

    @Test
    void setUrgency_andUrgencyLevel_exposeValues() {
        assertNull(service.urgencyLevel(PROFILE_ID));

        service.setUrgency(PROFILE_ID, true, "HIGH", "Chest pain");

        assertTrue(service.isUrgent(PROFILE_ID));
        assertEquals("HIGH", service.urgencyLevel(PROFILE_ID));
        ReservationDraft draft = service.getDraft(PROFILE_ID);
        assertFalse(draft.complete());
        assertFalse(draft.hasAnyData());
    }

    @Test
    void setUrgency_false_overwritesPreviousValues() {
        service.setUrgency(PROFILE_ID, true, "HIGH", "Chest pain");
        service.setUrgency(PROFILE_ID, false, "LOW", "Check-up");

        assertFalse(service.isUrgent(PROFILE_ID));
        assertEquals("LOW", service.urgencyLevel(PROFILE_ID));
    }

    @Test
    void reset_clearsDraftData() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceBriefBuilder.build(any(), any())).thenReturn("brief");
        service.updateField(PROFILE_ID, "serviceTypeId", SERVICE_TYPE_ID.toString());
        service.setUrgency(PROFILE_ID, true, "HIGH", "reason");

        service.reset(PROFILE_ID);

        ReservationDraft draft = service.getDraft(PROFILE_ID);
        assertNull(draft.serviceTypeId());
        assertFalse(draft.complete());
        assertFalse(service.isUrgent(PROFILE_ID));
        assertNull(service.urgencyLevel(PROFILE_ID));
    }

    @Test
    void clearServiceType_clearsOnlyServiceFieldsKeepsUrgency() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceBriefBuilder.build(any(), any())).thenReturn("brief");
        service.updateField(PROFILE_ID, "serviceTypeId", SERVICE_TYPE_ID.toString());
        service.setUrgency(PROFILE_ID, true, "HIGH", "Chest pain");

        service.clearServiceType(PROFILE_ID);

        ReservationDraft draft = service.getDraft(PROFILE_ID);
        assertNull(draft.serviceTypeId());
        assertNull(draft.serviceTypeName());
        assertNull(draft.serviceDescription());
        assertFalse(draft.complete());
        assertFalse(draft.hasAnyData());
        assertTrue(service.isUrgent(PROFILE_ID));
        assertEquals("HIGH", service.urgencyLevel(PROFILE_ID));
    }

    @Test
    void clearServiceType_noState_isNoOp() {
        service.clearServiceType(PROFILE_ID);

        ReservationDraft draft = service.getDraft(PROFILE_ID);
        assertNull(draft.serviceTypeId());
        assertFalse(draft.complete());
        assertFalse(service.isUrgent(PROFILE_ID));
        assertNull(service.urgencyLevel(PROFILE_ID));
    }

    @Test
    void clearServiceType_afterReset_isNoOp() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(serviceType()));
        when(serviceBriefBuilder.build(any(), any())).thenReturn("brief");
        service.updateField(PROFILE_ID, "serviceTypeId", SERVICE_TYPE_ID.toString());
        service.reset(PROFILE_ID);

        service.clearServiceType(PROFILE_ID);

        ReservationDraft draft = service.getDraft(PROFILE_ID);
        assertNull(draft.serviceTypeId());
        assertFalse(draft.complete());
    }
}