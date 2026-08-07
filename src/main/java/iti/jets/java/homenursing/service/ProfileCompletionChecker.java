package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.repository.ProfileAllergyRepository;
import iti.jets.java.homenursing.repository.ProfileMedicalConditionRepository;
import iti.jets.java.homenursing.repository.ProfileMedicationRepository;
import iti.jets.java.homenursing.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfileCompletionChecker {

    private final ProfileRepository profileRepository;
    private final AddressRepository addressRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final ProfileAllergyRepository profileAllergyRepository;
    private final ProfileMedicationRepository profileMedicationRepository;
    private final ProfileMedicalConditionRepository profileMedicalConditionRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;

    @Named("isUserProfileCompleted")
    public boolean isUserProfileCompleted(User user) {
        if (!hasBasicUserInfo(user)) {
            return false;
        }

        Profile profile = profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(user.getId())
                .orElse(null);
        if (profile == null || !hasCompleteProfileDetails(profile)) {
            return false;
        }

        UUID profileId = profile.getId();
        return hasCompleteAddress(profileId)
                && emergencyContactRepository.existsByProfileId(profileId)
                && profileAllergyRepository.existsByProfileId(profileId)
                && profileMedicationRepository.existsByProfileId(profileId)
                && profileMedicalConditionRepository.existsByProfileId(profileId)
                && medicalHistoryRepository.existsByProfileId(profileId);
    }

    private boolean hasBasicUserInfo(User user) {
        return hasText(user.getFirstName())
                && !isPlaceholderFirstName(user.getFirstName())
                && hasText(user.getLastName())
                && user.getDateOfBirth() != null
                && user.getGender() != null;
    }

    private boolean hasCompleteProfileDetails(Profile profile) {
        return hasText(profile.getBloodType())
                && profile.getHeight() != null
                && profile.getWeight() != null
                && hasText(profile.getMobilityStatus());
    }

    private boolean hasCompleteAddress(UUID profileId) {
        return addressRepository.findByProfileId(profileId)
                .map(address -> hasText(address.getCity()))
                .orElse(false);
    }

    private boolean isPlaceholderFirstName(String firstName) {
        String trimmed = firstName.trim();
        return "User".equalsIgnoreCase(trimmed) || "Nurse".equalsIgnoreCase(trimmed);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
