package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.UserResponse;
import iti.jets.java.homenursing.dto.UserUpdateRequest;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.UserMapper;
import iti.jets.java.homenursing.repository.AddressRepository;
import iti.jets.java.homenursing.repository.EmergencyContactRepository;
import iti.jets.java.homenursing.repository.MedicalHistoryRepository;
import iti.jets.java.homenursing.repository.ProfileRepository;
import iti.jets.java.homenursing.repository.ReviewRatingRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    .forEach(reviewRatingRepository::delete);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
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
}
