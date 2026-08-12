package iti.jets.java.homenursing.service.impl;


import iti.jets.java.homenursing.dto.auth.DevOtpResponse;
import iti.jets.java.homenursing.dto.auth.NurseTokenPair;
import iti.jets.java.homenursing.dto.nurse.NurseUserResponse;
import iti.jets.java.homenursing.dto.auth.TokenPair;
import iti.jets.java.homenursing.dto.user.UserResponse;
import iti.jets.java.homenursing.dto.nurse.NurseAuthResponse;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.ConflictException;
import iti.jets.java.homenursing.exception.InvalidOtpException;
import iti.jets.java.homenursing.exception.RateLimitException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.UserMapper;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.AuthService;
import iti.jets.java.homenursing.service.ProfileService;
import iti.jets.java.homenursing.service.TokenService;
import iti.jets.java.homenursing.service.TwilioSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final NurseRepository nurseRepository;
    private final UserMapper userMapper;
    private final TwilioSmsService twilioSmsService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;

    @Override
    public void requestOtp(String rawPhone) {
        String phone = normalizePhoneNumber(rawPhone);

        String phoneKey = "rate_limit:phone:" + phone;
        if (isRateLimited(phoneKey, 600, 3)) {
            throw new RateLimitException("Too many OTP requests. Please try again later.");
        }

        String otp = generateOtp();
        String hashedOtp = passwordEncoder.encode(otp);

        tokenService.set(
                "otp:" + phone, hashedOtp, Duration.ofSeconds(300));
        tokenService.set(
                "otp_attempts:" + phone, "0", Duration.ofSeconds(300));

        twilioSmsService.sendOtp(phone, otp);
    }

    @Override
    public DevOtpResponse requestOtpDev(String rawPhone) {
        String phone = normalizePhoneNumber(rawPhone);
        String otp = generateOtp();
        String hashedOtp = passwordEncoder.encode(otp);

        tokenService.set("otp:" + phone, hashedOtp, Duration.ofSeconds(300));
        tokenService.set("otp_attempts:" + phone, "0", Duration.ofSeconds(300));

        return new DevOtpResponse(phone, otp);
    }

    @Override
    public TokenPair verifyOtpAndLogin(String rawPhone, String otp) {
        String phone = normalizePhoneNumber(rawPhone);
        verifyOtp(phone, otp);

        User user = findOrCreateUser(phone, false);
        return loginUser(user);
    }

    @Override
    public NurseTokenPair verifyNurseOtpAndLogin(String rawPhone, String otp) {
        String phone = normalizePhoneNumber(rawPhone);
        verifyOtp(phone, otp);

        User user = userRepository.findByPhoneNumberWithProfiles(phone).orElse(null);

        if (user != null) {
            if (Boolean.TRUE.equals(user.getIsDeleted())) {
                throw new ResourceNotFoundException("User not found");
            }
            Optional<Nurse> nurseOpt = nurseRepository.findByUser_Id(user.getId());
            if (nurseOpt.isEmpty()) {
                throw new ConflictException("This phone is already registered as a regular user. Please use the user login.");
            }
        } else {
            user = createNurseUser(phone);
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String userId = user.getId().toString();
        Nurse nurse = nurseRepository.findByUser_Id(user.getId()).orElse(null);
        String role = nurse != null ? "NURSE" : "USER";
        String accessToken = tokenService.generateAccessToken(userId, role);
        String refreshToken = tokenService.generateRefreshToken(userId);

        NurseAuthResponse nurseData = null;
        if (nurse != null) {
            nurseData = NurseAuthResponse.builder()
                    .id(nurse.getId())
                    .nationalId(nurse.getNationalId())
                    .nationalIdFrontUrl(nurse.getNationalIdFrontUrl())
                    .nationalIdBackUrl(nurse.getNationalIdBackUrl())
                    .licenseImageUrl(nurse.getLicenseImageUrl())
                    .professionalCertificateUrl(nurse.getProfessionalCertificateUrl())
                    .specialization(nurse.getSpecialization())
                    .yearsOfExperience(nurse.getYearsOfExperience())
                    .bio(nurse.getBio())
                    .rejectionReason(nurse.getRejectionReason())
                    .ratingAvg(nurse.getRatingAvg())
                    .totalReviews(nurse.getTotalReviews())
                    .verificationStatus(nurse.getVerificationStatus())
                    .build();
        }

        UserResponse userResponse = userMapper.toResponse(user);
        NurseUserResponse nurseUser = NurseUserResponse.builder()
                .id(userResponse.getId())
                .phoneNumber(userResponse.getPhoneNumber())
                .email(userResponse.getEmail())
                .firstName(userResponse.getFirstName())
                .lastName(userResponse.getLastName())
                .dateOfBirth(userResponse.getDateOfBirth())
                .gender(userResponse.getGender())
                .profileImageUrl(userResponse.getProfileImageUrl())
                .isDeleted(userResponse.getIsDeleted())
                .createdAt(userResponse.getCreatedAt())
                .updatedAt(userResponse.getUpdatedAt())
                .lastLoginAt(userResponse.getLastLoginAt())
                .defaultProfileId(userResponse.getDefaultProfileId())
                .nurse(nurseData)
                .build();

        return new NurseTokenPair(accessToken, refreshToken, tokenService.getAccessTokenTtlSeconds(), nurseUser);
    }

    private User findOrCreateUser(String phone, boolean isNurse) {
        User user = userRepository.findByPhoneNumberWithProfiles(phone).orElse(null);

        if (user != null) {
            if (Boolean.TRUE.equals(user.getIsDeleted())) {
                throw new ResourceNotFoundException("User not found");
            }
            boolean hasNurseRecord = nurseRepository.existsByUser_Id(user.getId());
            if (hasNurseRecord && !isNurse) {
                throw new ConflictException("This phone is already registered as a nurse. Please use the nurse login.");
            }
            if (!hasNurseRecord && isNurse) {
                throw new ConflictException("This phone is already registered as a regular user. Please use the user login.");
            }
        } else {
            user = isNurse ? createNurseUser(phone) : createUser(phone);
        }

        return user;
    }

    @Override
    public TokenPair refreshToken(String refreshToken) {
        tokenService.validateRefreshToken(refreshToken);
        String userId = tokenService.getUserIdFromRefreshToken(refreshToken);
        tokenService.revokeRefreshToken(refreshToken);

        User user = userRepository.findByIdWithProfiles(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ResourceNotFoundException("User not found");
        }

        String role = nurseRepository.existsByUser_Id(user.getId()) ? "NURSE" : "USER";
        String newAccessToken = tokenService.generateAccessToken(userId, role);
        String newRefreshToken = tokenService.generateRefreshToken(userId);
        UserResponse userResponse = userMapper.toResponse(user);

        return new TokenPair(newAccessToken, newRefreshToken, tokenService.getAccessTokenTtlSeconds(), userResponse);
    }

    @Override
    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    @Override
    public UserResponse getUserProfile(String phoneNumber) {
        String phone = normalizePhoneNumber(phoneNumber);
        User user = userRepository.findByPhoneNumberWithProfiles(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ResourceNotFoundException("User not found");
        }
        return userMapper.toResponse(user);
    }

    private boolean isRateLimited(String key, int windowSeconds, int maxRequests) {
        Long count = tokenService.increment(key);
        if (count != null && count == 1) {
            tokenService.expire(key, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count > maxRequests;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        String digits = phoneNumber.replaceAll("\\D+", "");
        return "+" + digits;
    }

    private String generateOtp() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
    }

    private User createUser(String phoneNumber) {
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .firstName("User")
                .lastName("")
                .isDeleted(false)
                .build();
        user = userRepository.save(user);
        profileService.createDefaultProfile(user);
        return user;
    }

    private User createNurseUser(String phoneNumber) {
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .firstName("Nurse")
                .lastName("")
                .isDeleted(false)
                .build();
        user = userRepository.save(user);

        Nurse nurse = Nurse.builder()
                .user(user)
                .verificationStatus(VerificationStatus.UNDER_REVIEW)
                .build();
        nurseRepository.save(nurse);

        return user;
    }

    private void verifyOtp(String phone, String otp) {
        String storedHash = tokenService.get("otp:" + phone);
        if (storedHash == null) {
            throw new InvalidOtpException("OTP has expired or is invalid.");
        }

        if (!passwordEncoder.matches(otp, storedHash)) {
            String attemptsKey = "otp_attempts:" + phone;
            Long attempts = tokenService.increment(attemptsKey);
            if (attempts != null && attempts >= 3) {
                tokenService.delete("otp:" + phone);
                tokenService.delete(attemptsKey);
                throw new InvalidOtpException("Too many failed attempts. OTP has been invalidated.");
            }
            throw new InvalidOtpException("Invalid OTP.");
        }

        tokenService.delete("otp:" + phone);
        tokenService.delete("otp_attempts:" + phone);
    }

    private TokenPair loginUser(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String userId = user.getId().toString();
        String role = nurseRepository.existsByUser_Id(user.getId()) ? "NURSE" : "USER";
        String accessToken = tokenService.generateAccessToken(userId, role);
        String refreshToken = tokenService.generateRefreshToken(userId);
        UserResponse userResponse = userMapper.toResponse(user);

        return new TokenPair(accessToken, refreshToken, tokenService.getAccessTokenTtlSeconds(), userResponse);
    }
}
