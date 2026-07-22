package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.nurse.NurseLocation;
import iti.jets.java.homenursing.service.NearbyNurseMatcher;
import iti.jets.java.homenursing.util.HaversineUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class NearbyNurseMatcherImpl implements NearbyNurseMatcher {

    @Value("${nearby.nurses.radius-km:10}")
    private double defaultRadiusKm;

    @Override
    public List<NearbyNurse> findNearbyNurse(
            List<NurseLocation> nurseLocations,
            BigDecimal userLatitude,
            BigDecimal userLongitude,
            Set<UUID> nursesForRequiredService) {

        if (nurseLocations == null || nurseLocations.isEmpty()) {
            return List.of();
        }

        return nurseLocations.stream()
                .filter(Objects::nonNull)
                .filter(loc -> loc.nurseId() != null && loc.latitude() != null && loc.longitude() != null)
                .filter(loc -> nursesForRequiredService == null || nursesForRequiredService.contains(loc.nurseId()))
                .map(loc -> {
                    double distance = HaversineUtil.distanceKm(userLatitude, userLongitude, loc.latitude(), loc.longitude());
                    return new NearbyNurse(loc.nurseId(), loc.latitude(), loc.longitude(), distance);
                })
                .filter(match -> match.distanceKm() <= defaultRadiusKm)
                .sorted(Comparator.comparingDouble(NearbyNurse::distanceKm))
                .toList();
    }
}
