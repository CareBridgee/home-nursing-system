package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReservationParticipantHelperTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @InjectMocks
    private ReservationParticipantHelper helper;

    @Test
    void delegatesToRepositoryAndReturnsTrueWhenParticipant() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(serviceRequestRepository.isParticipant(reservationId, userId)).thenReturn(true);

        assertThat(helper.isParticipant(reservationId, userId)).isTrue();
        verify(serviceRequestRepository).isParticipant(reservationId, userId);
    }

    @Test
    void delegatesToRepositoryAndReturnsFalseWhenNotParticipant() {
        UUID reservationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(serviceRequestRepository.isParticipant(reservationId, userId)).thenReturn(false);

        assertThat(helper.isParticipant(reservationId, userId)).isFalse();
        verify(serviceRequestRepository).isParticipant(reservationId, userId);
    }
}
