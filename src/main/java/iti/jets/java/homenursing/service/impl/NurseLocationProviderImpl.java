package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurse.NurseLocation;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.service.NurseLocationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class NurseLocationProviderImpl implements NurseLocationProvider {

    private final WebSocketPresenceService webSocketPresenceService;
    private final NurseRepository nurseRepository;

    @Override
    public List<NurseLocation> getNurseLocations() {
        Set<String> onlineUserIds = webSocketPresenceService.getOnlineNurses();
        if (onlineUserIds == null || onlineUserIds.isEmpty()) {
            return List.of();
        }

        return onlineUserIds.stream()
                .map(this::toNurseLocation)
                .filter(Objects::nonNull)
                .toList();
    }

    private NurseLocation toNurseLocation(String userIdValue) {
        UUID userId;
        try {
            userId = UUID.fromString(userIdValue);
        } catch (IllegalArgumentException exception) {
            return null;
        }

        Nurse nurse = nurseRepository.findByUser_Id(userId).orElse(null);
        if (nurse == null
                || nurse.getVerificationStatus() != VerificationStatus.APPROVED
                || !Boolean.TRUE.equals(nurse.getIsAvailable())) {
            return null;
        }

        Point location = webSocketPresenceService.getAvailableLocation(userIdValue).orElse(null);
        if (location == null) {
            return null;
        }

        return new NurseLocation(
                nurse.getId(),
                BigDecimal.valueOf(location.getY()),
                BigDecimal.valueOf(location.getX()));
    }
}
