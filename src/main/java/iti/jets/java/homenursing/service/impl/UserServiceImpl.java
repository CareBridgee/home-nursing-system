package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.user.CreditUpdateRequest;
import iti.jets.java.homenursing.dto.user.CreditUpdateResponse;
import iti.jets.java.homenursing.dto.user.UserResponse;
import iti.jets.java.homenursing.dto.user.UserUpdateRequest;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.CreditOperation;
import iti.jets.java.homenursing.exception.InsufficientCreditException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.UserMapper;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.repository.ProfileRepository;
import iti.jets.java.homenursing.repository.ReviewRatingRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.CloudinaryService;
import iti.jets.java.homenursing.service.UserService;
import iti.jets.java.homenursing.util.NurseRatingUpdater;
import iti.jets.java.homenursing.util.SortSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProfileRepository profileRepository;
    private final AddressRepository addressRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final ReviewRatingRepository reviewRatingRepository;
    private final NurseRatingUpdater nurseRatingUpdater;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = getActiveUser(userId);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        User user = getActiveUser(userId);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUser(UUID userId, UserUpdateRequest request) {
        User user = getActiveUser(userId);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());
        uploadProfileImage(user, request.getProfileImage());
        syncPrimaryProfileImage(user);

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCurrentUser(UUID userId) {
        User user = getActiveUser(userId);

        user.setIsDeleted(true);
        userRepository.save(user);

        var profiles = profileRepository.findByUserIdAndIsDeletedFalse(userId);
        for (var profile : profiles) {
            profile.setIsDeleted(true);
            profileRepository.save(profile);

            addressRepository.findByProfileId(profile.getId())
                    .ifPresent(addressRepository::delete);

            medicalHistoryRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId())
                    .forEach(medicalHistoryRepository::delete);

            emergencyContactRepository.findByProfileId(profile.getId())
                    .forEach(emergencyContactRepository::delete);

            reviewRatingRepository.findByProfileId(profile.getId())
                    .forEach(review -> {
                        nurseRatingUpdater.onReviewDeleted(review.getNurse(), review.getRating());
                        reviewRatingRepository.delete(review);
                    });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(SortSanitizer.sanitize(pageable, SortSanitizer.asSet(SortSanitizer.USER_SORTABLE)))
                .map(userMapper::toResponse);
    }

    private User getActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return user;
    }

    private void uploadProfileImage(User user, MultipartFile profileImage) {
        if (profileImage != null && !profileImage.isEmpty()) {
            user.setProfileImageUrl(cloudinaryService.upload(profileImage));
        }
    }

    private void syncPrimaryProfileImage(User user) {
        profileRepository.findByUserIdAndIsPrimaryTrueAndIsDeletedFalse(user.getId())
                .ifPresent(primary -> {
                    primary.setProfileImageUrl(user.getProfileImageUrl());
                    profileRepository.save(primary);
                });
    }
    @Override
    @Transactional
    public CreditUpdateResponse updateCredit(
            UUID userId,
            CreditUpdateRequest request) {

        User user = getActiveUser(userId);

        BigDecimal currentCredit = user.getCredit();

        if (request.getOperation() == CreditOperation.ADD) {

            user.setCredit(
                    currentCredit.add(request.getAmount())
            );

        } else if (request.getOperation() == CreditOperation.DEDUCT) {

            if (currentCredit.compareTo(request.getAmount()) < 0) {
                throw new InsufficientCreditException(
                        "Insufficient credit"
                );
            }

            user.setCredit(
                    currentCredit.subtract(request.getAmount())
            );
        }

        User saved = userRepository.save(user);

        return CreditUpdateResponse.builder()
                .credit(saved.getCredit())
                .build();
    }
}
