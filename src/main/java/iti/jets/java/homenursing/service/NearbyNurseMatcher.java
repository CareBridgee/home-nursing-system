package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NearbyNurse;
import iti.jets.java.homenursing.dto.nurse.NurseLocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface NearbyNurseMatcher {

    List<NearbyNurse> findNearbyNurse(
            List<NurseLocation> nurseLocations,
            BigDecimal userLatitude,
            BigDecimal userLongitude,
            Set<UUID> nursesForRequiredService);
}
