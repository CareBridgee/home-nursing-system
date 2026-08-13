package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.auth.DevOtpResponse;
import iti.jets.java.homenursing.dto.auth.GoogleAuthResponse;
import iti.jets.java.homenursing.dto.auth.NurseTokenPair;
import iti.jets.java.homenursing.dto.auth.PendingAuth;
import iti.jets.java.homenursing.dto.auth.TokenPair;
import iti.jets.java.homenursing.dto.nurse.NurseAuthResponse;
import iti.jets.java.homenursing.dto.user.UserResponse;
import iti.jets.java.homenursing.entity.Nurse;
import iti.jets.java.homenursing.entity.User;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.exception.ConflictException;
import iti.jets.java.homenursing.exception.InvalidOtpException;
import iti.jets.java.homenursing.exception.RateLimitException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.exception.UnauthorizedException;
import iti.jets.java.homenursing.mapper.UserMapper;
import iti.jets.java.homenursing.repository.NurseRepository;
import iti.jets.java.homenursing.repository.UserRepository;
import iti.jets.java.homenursing.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private NurseRepository nurseRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private TwilioSmsService twilioSmsService;
    @Mock
    private TokenService tokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ProfileService profileService;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final String PHONE = "+201234567890";
    private static final String GOOGLE_SUB = "google-sub-123";
    private static final String GOOGLE_EMAIL = "jane@example.com";
    private static final GoogleTokenVerifier.GoogleUserInfo GOOGLE_INFO =
            new GoogleTokenVerifier.GoogleUserInfo(GOOGLE_SUB, GOOGLE_EMAIL, "Jane", "Doe", "https://pic");

    private static User user(UUID id, boolean deleted) {
        return User.builder()
                .id(id)
                .phoneNumber(PHONE)
                .firstName("First")
                .lastName("Last")
                .isDeleted(deleted)
                .build();
    }

    private static UserResponse userResponse(UUID id) {
        return UserResponse.builder()
                .id(id)
                .phoneNumber(PHONE)
                .firstName("First")
                .lastName("Last")
                .build();
    }

    @Test
    void requestOtpStoresHashedOtpAndAttemptsCounterThenSendsSms() {
        when(tokenService.increment("rate_limit:phone:" + PHONE)).thenReturn(1L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        authService.requestOtp(PHONE);

        verify(tokenService).expire("rate_limit:phone:" + PHONE, Duration.ofSeconds(600));
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenService).set(eq("otp:" + PHONE), eq("hashed"), eq(Duration.ofSeconds(300)));
        verify(tokenService).set(eq("otp_attempts:" + PHONE), eq("0"), eq(Duration.ofSeconds(300)));
        verify(twilioSmsService).sendOtp(eq(PHONE), otpCaptor.capture());
        assertThat(otpCaptor.getValue()).matches("\\d{6}");
    }

    @Test
    void requestOtpNormalizesPhoneDigits() {
        when(tokenService.increment(anyString())).thenReturn(1L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        authService.requestOtp("+20 (123) 456-7890");

        verify(tokenService).set(eq("otp:" + PHONE), anyString(), any(Duration.class));
        verify(twilioSmsService).sendOtp(eq(PHONE), anyString());
    }

    @Test
    void requestOtpRejectsWhenRateLimited() {
        when(tokenService.increment("rate_limit:phone:" + PHONE)).thenReturn(4L);

        assertThatThrownBy(() -> authService.requestOtp(PHONE))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Too many OTP requests");

        verify(twilioSmsService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void requestOtpWithinLimitDoesNotTouchExpiry() {
        when(tokenService.increment("rate_limit:phone:" + PHONE)).thenReturn(2L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        authService.requestOtp(PHONE);

        verify(tokenService, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void requestOtpWithNullPhoneProceedsWithoutRateLimit() {
        when(tokenService.increment("rate_limit:phone:null")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        authService.requestOtp(null);

        verify(tokenService).set(eq("otp:null"), eq("hashed"), eq(Duration.ofSeconds(300)));
        verify(twilioSmsService).sendOtp(eq(null), anyString());
    }

    @Test
    void requestOtpDevReturnsNormalizedPhoneAndSixDigitOtp() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        DevOtpResponse response = authService.requestOtpDev("+20 (123) 456-7890");

        assertThat(response.phoneNumber()).isEqualTo(PHONE);
        assertThat(response.otp()).matches("\\d{6}");
        verify(tokenService).set(eq("otp:" + PHONE), eq("hashed"), eq(Duration.ofSeconds(300)));
        verify(tokenService).set(eq("otp_attempts:" + PHONE), eq("0"), eq(Duration.ofSeconds(300)));
    }

    @Test
    void verifyOtpAndLoginCreatesNewUserAndLogsIn() {
        UUID userId = UUID.randomUUID();
        User created = user(userId, false);
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(created);
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);
        when(tokenService.generateAccessToken(userId.toString(), "USER")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(created)).thenReturn(userResponse(userId));

        TokenPair pair = authService.verifyOtpAndLogin(PHONE, "123456");

        assertThat(pair.getAccessToken()).isEqualTo("access-t");
        assertThat(pair.getRefreshToken()).isEqualTo("refresh-t");
        assertThat(pair.getExpiresIn()).isEqualTo(900L);
        assertThat(pair.getUser().getId()).isEqualTo(userId);
        assertThat(created.getLastLoginAt()).isNotNull();
        verify(profileService).createDefaultProfile(created);
        verify(tokenService).delete("otp:" + PHONE);
        verify(tokenService).delete("otp_attempts:" + PHONE);
    }

    @Test
    void verifyOtpAndLoginExistingUserWithoutNurseRecordLogsIn() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);
        when(tokenService.generateAccessToken(userId.toString(), "USER")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        TokenPair pair = authService.verifyOtpAndLogin(PHONE, "123456");

        assertThat(pair.getUser().getPhoneNumber()).isEqualTo(PHONE);
        verify(userRepository).save(existing);
        verify(profileService, never()).createDefaultProfile(any(User.class));
    }

    @Test
    void verifyOtpAndLoginDeletedUserThrows() {
        UUID userId = UUID.randomUUID();
        User deleted = user(userId, true);
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> authService.verifyOtpAndLogin(PHONE, "123456"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void verifyOtpAndLoginUserWithNurseRecordThrowsConflict() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(true);

        assertThatThrownBy(() -> authService.verifyOtpAndLogin(PHONE, "123456"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("registered as a nurse");
    }

    @Test
    void verifyOtpAndLoginUsesNurseRoleInTokenWhenUserHasNurseRecord() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false, true);
        when(tokenService.generateAccessToken(userId.toString(), "NURSE")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        TokenPair pair = authService.verifyOtpAndLogin(PHONE, "123456");

        assertThat(pair.getAccessToken()).isEqualTo("access-t");
    }

    @Test
    void verifyOtpAndLoginWithExpiredOtpThrows() {
        when(tokenService.get("otp:" + PHONE)).thenReturn(null);

        assertThatThrownBy(() -> authService.verifyOtpAndLogin(PHONE, "123456"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).findByPhoneNumberWithProfiles(anyString());
    }

    @Test
    void verifyOtpAndLoginWithWrongOtpIncrementsAttempts() {
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(false);
        when(tokenService.increment("otp_attempts:" + PHONE)).thenReturn(2L);

        assertThatThrownBy(() -> authService.verifyOtpAndLogin(PHONE, "999999"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void verifyOtpAndLoginThirdWrongOtpInvalidatesStoredOtp() {
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(false);
        when(tokenService.increment("otp_attempts:" + PHONE)).thenReturn(3L);

        assertThatThrownBy(() -> authService.verifyOtpAndLogin(PHONE, "999999"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Too many failed attempts");

        verify(tokenService).delete("otp:" + PHONE);
        verify(tokenService).delete("otp_attempts:" + PHONE);
    }

    @Test
    void verifyOtpAndLoginWithNullAttemptsCounterStillRejects() {
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(false);
        when(tokenService.increment("otp_attempts:" + PHONE)).thenReturn(null);

        assertThatThrownBy(() -> authService.verifyOtpAndLogin(PHONE, "999999"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void verifyNurseOtpAndLoginCreatesNurseUserWithUnderReviewStatus() {
        UUID userId = UUID.randomUUID();
        User created = user(userId, false);
        Nurse nurse = nurse(userId, "NURSE-1");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(created);
        when(nurseRepository.findByUser_Id(userId)).thenReturn(Optional.of(nurse));
        when(tokenService.generateAccessToken(userId.toString(), "NURSE")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(created)).thenReturn(userResponse(userId));

        NurseTokenPair pair = authService.verifyNurseOtpAndLogin(PHONE, "123456");

        ArgumentCaptor<Nurse> nurseCaptor = ArgumentCaptor.forClass(Nurse.class);
        verify(nurseRepository).save(nurseCaptor.capture());
        assertThat(nurseCaptor.getValue().getUser()).isSameAs(created);
        assertThat(nurseCaptor.getValue().getVerificationStatus()).isEqualTo(VerificationStatus.UNDER_REVIEW);
        assertThat(pair.getAccessToken()).isEqualTo("access-t");
        assertThat(pair.getUser().getNurse().getNationalId()).isEqualTo("NURSE-1");
        assertThat(created.getLastLoginAt()).isNotNull();
    }

    @Test
    void verifyNurseOtpAndLoginExistingNurseLogsIn() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        Nurse nurse = nurse(userId, "NURSE-2");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(nurseRepository.findByUser_Id(userId)).thenReturn(Optional.of(nurse));
        when(tokenService.generateAccessToken(userId.toString(), "NURSE")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        NurseTokenPair pair = authService.verifyNurseOtpAndLogin(PHONE, "123456");

        assertThat(pair.getUser().getNurse().getNationalId()).isEqualTo("NURSE-2");
        verify(userRepository).save(existing);
    }

    @Test
    void verifyNurseOtpAndLoginDeletedUserThrows() {
        UUID userId = UUID.randomUUID();
        User deleted = user(userId, true);
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> authService.verifyNurseOtpAndLogin(PHONE, "123456"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void verifyNurseOtpAndLoginRegularUserThrowsConflict() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(nurseRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyNurseOtpAndLogin(PHONE, "123456"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("registered as a regular user");
    }

    @Test
    void verifyNurseOtpAndLoginWithoutNurseRecordFallsBackToUserRole() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        Nurse nurse = nurse(userId, "NURSE-3");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(nurseRepository.findByUser_Id(userId)).thenReturn(Optional.of(nurse), Optional.empty());
        when(tokenService.generateAccessToken(userId.toString(), "USER")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        NurseTokenPair pair = authService.verifyNurseOtpAndLogin(PHONE, "123456");

        assertThat(pair.getAccessToken()).isEqualTo("access-t");
        assertThat(pair.getUser().getNurse()).isNull();
    }

    @Test
    void refreshTokenRotatesBothTokens() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(tokenService.getUserIdFromRefreshToken("old-refresh")).thenReturn(userId.toString());
        when(userRepository.findByIdWithProfiles(userId)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);
        when(tokenService.generateAccessToken(userId.toString(), "USER")).thenReturn("new-access");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("new-refresh");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        TokenPair pair = authService.refreshToken("old-refresh");

        assertThat(pair.getAccessToken()).isEqualTo("new-access");
        assertThat(pair.getRefreshToken()).isEqualTo("new-refresh");
        assertThat(pair.getExpiresIn()).isEqualTo(900L);
        verify(tokenService).validateRefreshToken("old-refresh");
        verify(tokenService).revokeRefreshToken("old-refresh");
    }

    @Test
    void refreshTokenWithUnknownUserThrows() {
        UUID userId = UUID.randomUUID();
        when(tokenService.getUserIdFromRefreshToken("old-refresh")).thenReturn(userId.toString());
        when(userRepository.findByIdWithProfiles(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("old-refresh"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refreshTokenForDeletedUserThrows() {
        UUID userId = UUID.randomUUID();
        User deleted = user(userId, true);
        when(tokenService.getUserIdFromRefreshToken("old-refresh")).thenReturn(userId.toString());
        when(userRepository.findByIdWithProfiles(userId)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> authService.refreshToken("old-refresh"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refreshTokenUsesNurseRoleWhenNurseRecordExists() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(tokenService.getUserIdFromRefreshToken("old-refresh")).thenReturn(userId.toString());
        when(userRepository.findByIdWithProfiles(userId)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(true);
        when(tokenService.generateAccessToken(userId.toString(), "NURSE")).thenReturn("new-access");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("new-refresh");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        TokenPair pair = authService.refreshToken("old-refresh");

        assertThat(pair.getAccessToken()).isEqualTo("new-access");
    }

    @Test
    void logoutRevokesRefreshToken() {
        authService.logout("refresh-t");

        verify(tokenService).revokeRefreshToken("refresh-t");
    }

    @Test
    void findOrCreateUserInNurseModeWithNurseRecordPassesThrough() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(true);

        User result = ReflectionTestUtils.invokeMethod(authService, "findOrCreateUser", PHONE, true);

        assertThat(result).isSameAs(existing);
        verify(nurseRepository, never()).save(any(Nurse.class));
    }

    @Test
    void findOrCreateUserInNurseModeWithoutNurseRecordThrowsConflict() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(authService, "findOrCreateUser", PHONE, true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("registered as a regular user");
    }

    @Test
    void findOrCreateUserInNurseModeCreatesNurseUser() {
        UUID userId = UUID.randomUUID();
        User created = user(userId, false);
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(created);

        User result = ReflectionTestUtils.invokeMethod(authService, "findOrCreateUser", PHONE, true);

        assertThat(result).isSameAs(created);
        ArgumentCaptor<Nurse> nurseCaptor = ArgumentCaptor.forClass(Nurse.class);
        verify(nurseRepository).save(nurseCaptor.capture());
        assertThat(nurseCaptor.getValue().getUser()).isSameAs(created);
        assertThat(nurseCaptor.getValue().getVerificationStatus()).isEqualTo(VerificationStatus.UNDER_REVIEW);
        verify(profileService, never()).createDefaultProfile(any(User.class));
    }

    @Test
    void getUserProfileReturnsMappedUser() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(existing));
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        UserResponse response = authService.getUserProfile("+20 (123) 456-7890");

        assertThat(response.getPhoneNumber()).isEqualTo(PHONE);
    }

    @Test
    void getUserProfileWithUnknownPhoneThrows() {
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserProfile(PHONE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void handleGoogleLoginNewUserWithoutPhoneReturnsPhoneRequired() {
        when(googleTokenVerifier.verify("google-token")).thenReturn(GOOGLE_INFO);
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmailWithProfiles(GOOGLE_EMAIL)).thenReturn(Optional.empty());
        User created = User.builder()
                .id(UUID.randomUUID())
                .email(GOOGLE_EMAIL)
                .firstName("Jane")
                .lastName("Doe")
                .profileImageUrl("https://pic")
                .googleSub(GOOGLE_SUB)
                .isDeleted(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(created);
        when(tokenService.generatePendingToken(any(PendingAuth.class))).thenReturn("pending-t");

        GoogleAuthResponse response = authService.handleGoogleLogin("google-token", false);

        assertThat(response.getStatus()).isEqualTo(GoogleAuthResponse.STATUS_PHONE_REQUIRED);
        assertThat(response.getPendingToken()).isEqualTo("pending-t");
        assertThat(response.getEmail()).isEqualTo(GOOGLE_EMAIL);
        assertThat(response.getFirstName()).isEqualTo("Jane");
        verify(profileService).createDefaultProfile(created);
        verify(tokenService).generatePendingToken(new PendingAuth(
                GOOGLE_SUB, GOOGLE_EMAIL, "Jane", "Doe", "https://pic", "USER"));
    }

    @Test
    void handleGoogleLoginExistingUserBySubWithPhoneAuthenticates() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        existing.setGoogleSub(GOOGLE_SUB);
        existing.setEmail(GOOGLE_EMAIL);
        when(googleTokenVerifier.verify("google-token")).thenReturn(GOOGLE_INFO);
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);
        when(tokenService.generateAccessToken(userId.toString(), "USER")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        GoogleAuthResponse response = authService.handleGoogleLogin("google-token", false);

        assertThat(response.getStatus()).isEqualTo(GoogleAuthResponse.STATUS_AUTHENTICATED);
        assertThat(response.getAccessToken()).isEqualTo("access-t");
        assertThat(response.getUser().getId()).isEqualTo(userId);
    }

    @Test
    void handleGoogleLoginExistingUserByEmailLinksGoogleSub() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        existing.setEmail(GOOGLE_EMAIL);
        when(googleTokenVerifier.verify("google-token")).thenReturn(GOOGLE_INFO);
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmailWithProfiles(GOOGLE_EMAIL)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);
        when(tokenService.generateAccessToken(userId.toString(), "USER")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(existing)).thenReturn(userResponse(userId));

        GoogleAuthResponse response = authService.handleGoogleLogin("google-token", false);

        assertThat(response.getStatus()).isEqualTo(GoogleAuthResponse.STATUS_AUTHENTICATED);
        assertThat(existing.getGoogleSub()).isEqualTo(GOOGLE_SUB);
        verify(userRepository, atLeastOnce()).save(existing);
    }

    @Test
    void handleGoogleLoginExistingUserByEmailWithoutPhoneReturnsPhoneRequired() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        existing.setPhoneNumber(null);
        existing.setEmail(GOOGLE_EMAIL);
        when(googleTokenVerifier.verify("google-token")).thenReturn(GOOGLE_INFO);
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmailWithProfiles(GOOGLE_EMAIL)).thenReturn(Optional.of(existing));
        when(tokenService.generatePendingToken(any(PendingAuth.class))).thenReturn("pending-t");

        GoogleAuthResponse response = authService.handleGoogleLogin("google-token", false);

        assertThat(response.getStatus()).isEqualTo(GoogleAuthResponse.STATUS_PHONE_REQUIRED);
        assertThat(response.getPendingToken()).isEqualTo("pending-t");
        assertThat(existing.getGoogleSub()).isEqualTo(GOOGLE_SUB);
    }

    @Test
    void handleGoogleLoginDeletedUserThrows() {
        UUID userId = UUID.randomUUID();
        User deleted = user(userId, true);
        deleted.setGoogleSub(GOOGLE_SUB);
        when(googleTokenVerifier.verify("google-token")).thenReturn(GOOGLE_INFO);
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> authService.handleGoogleLogin("google-token", false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void handleGoogleLoginNurseIntentOnRegularUserThrowsConflict() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        existing.setGoogleSub(GOOGLE_SUB);
        when(googleTokenVerifier.verify("google-token")).thenReturn(GOOGLE_INFO);
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);

        assertThatThrownBy(() -> authService.handleGoogleLogin("google-token", true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("regular user");
    }

    @Test
    void handleGoogleLoginUserIntentOnNurseThrowsConflict() {
        UUID userId = UUID.randomUUID();
        User existing = user(userId, false);
        existing.setGoogleSub(GOOGLE_SUB);
        when(googleTokenVerifier.verify("google-token")).thenReturn(GOOGLE_INFO);
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.of(existing));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(true);

        assertThatThrownBy(() -> authService.handleGoogleLogin("google-token", false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("nurse");
    }

    @Test
    void handleGoogleLoginNurseIntentNewUserCreatesNurseRecordAndReturnsPhoneRequired() {
        when(googleTokenVerifier.verify("google-token")).thenReturn(GOOGLE_INFO);
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userRepository.findByEmailWithProfiles(GOOGLE_EMAIL)).thenReturn(Optional.empty());
        User created = User.builder()
                .id(UUID.randomUUID())
                .email(GOOGLE_EMAIL)
                .firstName("Jane")
                .lastName("Doe")
                .googleSub(GOOGLE_SUB)
                .isDeleted(false)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(created);
        when(tokenService.generatePendingToken(any(PendingAuth.class))).thenReturn("pending-t");

        GoogleAuthResponse response = authService.handleGoogleLogin("google-token", true);

        ArgumentCaptor<Nurse> nurseCaptor = ArgumentCaptor.forClass(Nurse.class);
        verify(nurseRepository).save(nurseCaptor.capture());
        assertThat(nurseCaptor.getValue().getUser()).isSameAs(created);
        assertThat(nurseCaptor.getValue().getVerificationStatus()).isEqualTo(VerificationStatus.UNDER_REVIEW);
        assertThat(response.getStatus()).isEqualTo(GoogleAuthResponse.STATUS_PHONE_REQUIRED);
        verify(profileService, never()).createDefaultProfile(any(User.class));
    }

    @Test
    void handleGoogleLoginWithInvalidTokenThrows() {
        when(googleTokenVerifier.verify("bad-token")).thenThrow(
                new UnauthorizedException("Invalid Google ID token"));

        assertThatThrownBy(() -> authService.handleGoogleLogin("bad-token", false))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void completeGoogleLinkingLinksPhoneToGoogleUserAndAuthenticates() {
        UUID userId = UUID.randomUUID();
        User created = user(userId, false);
        created.setPhoneNumber(null);
        created.setEmail(GOOGLE_EMAIL);
        created.setGoogleSub(GOOGLE_SUB);
        created.setFirstName("User");
        created.setLastName("");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(tokenService.parsePendingToken("pending-t"))
                .thenReturn(new PendingAuth(GOOGLE_SUB, GOOGLE_EMAIL, "Jane", "Doe", "https://pic", "USER"));
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.empty());
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.of(created));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);
        when(tokenService.generateAccessToken(userId.toString(), "USER")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(created)).thenReturn(userResponse(userId));

        GoogleAuthResponse response = authService.completeGoogleLinking(PHONE, "123456", "pending-t");

        assertThat(response.getStatus()).isEqualTo(GoogleAuthResponse.STATUS_AUTHENTICATED);
        assertThat(response.getAccessToken()).isEqualTo("access-t");
        assertThat(created.getPhoneNumber()).isEqualTo(PHONE);
        assertThat(created.getGoogleSub()).isEqualTo(GOOGLE_SUB);
        assertThat(created.getFirstName()).isEqualTo("Jane");
        assertThat(created.getLastName()).isEqualTo("Doe");
        verify(tokenService).delete("otp:" + PHONE);
    }

    @Test
    void completeGoogleLinkingCreatesPhoneUserWhenPendingSubUnknown() {
        UUID userId = UUID.randomUUID();
        User created = user(userId, false);
        created.setFirstName("User");
        created.setLastName("");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(tokenService.parsePendingToken("pending-t"))
                .thenReturn(new PendingAuth(GOOGLE_SUB, GOOGLE_EMAIL, "Jane", "Doe", null, "USER"));
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.empty());
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(created);
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(false);
        when(tokenService.generateAccessToken(userId.toString(), "USER")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(created)).thenReturn(userResponse(userId));

        GoogleAuthResponse response = authService.completeGoogleLinking(PHONE, "123456", "pending-t");

        assertThat(response.getStatus()).isEqualTo(GoogleAuthResponse.STATUS_AUTHENTICATED);
        assertThat(created.getGoogleSub()).isEqualTo(GOOGLE_SUB);
        assertThat(created.getEmail()).isEqualTo(GOOGLE_EMAIL);
        assertThat(created.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void completeGoogleLinkingWithPhoneTakenByAnotherAccountThrowsConflict() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User other = user(otherUserId, false);
        other.setGoogleSub("different-google-sub");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(tokenService.parsePendingToken("pending-t"))
                .thenReturn(new PendingAuth(GOOGLE_SUB, GOOGLE_EMAIL, "Jane", "Doe", null, "USER"));
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> authService.completeGoogleLinking(PHONE, "123456", "pending-t"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered to another account");
    }

    @Test
    void completeGoogleLinkingNursePendingReturnsNurseUser() {
        UUID userId = UUID.randomUUID();
        User created = user(userId, false);
        created.setPhoneNumber(null);
        created.setGoogleSub(GOOGLE_SUB);
        created.setFirstName("Jane");
        created.setLastName("Doe");
        Nurse nurse = nurse(userId, "NURSE-G");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(tokenService.parsePendingToken("pending-t"))
                .thenReturn(new PendingAuth(GOOGLE_SUB, GOOGLE_EMAIL, "Jane", "Doe", null, "NURSE"));
        when(userRepository.findByPhoneNumberWithProfiles(PHONE)).thenReturn(Optional.empty());
        when(userRepository.findByGoogleSubWithProfiles(GOOGLE_SUB)).thenReturn(Optional.of(created));
        when(nurseRepository.existsByUser_Id(userId)).thenReturn(true);
        when(nurseRepository.findByUser_Id(userId)).thenReturn(Optional.of(nurse));
        when(tokenService.generateAccessToken(userId.toString(), "NURSE")).thenReturn("access-t");
        when(tokenService.generateRefreshToken(userId.toString())).thenReturn("refresh-t");
        when(tokenService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(userMapper.toResponse(created)).thenReturn(userResponse(userId));

        GoogleAuthResponse response = authService.completeGoogleLinking(PHONE, "123456", "pending-t");

        assertThat(response.getStatus()).isEqualTo(GoogleAuthResponse.STATUS_AUTHENTICATED);
        assertThat(response.getNurseUser()).isNotNull();
        assertThat(response.getNurseUser().getNurse().getNationalId()).isEqualTo("NURSE-G");
        assertThat(response.getAccessToken()).isEqualTo("access-t");
    }

    @Test
    void completeGoogleLinkingWithWrongOtpThrows() {
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(false);
        when(tokenService.increment("otp_attempts:" + PHONE)).thenReturn(1L);

        assertThatThrownBy(() -> authService.completeGoogleLinking(PHONE, "999999", "pending-t"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void completeGoogleLinkingWithInvalidPendingTokenThrows() {
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);
        when(tokenService.get("otp:" + PHONE)).thenReturn("hash");
        when(tokenService.parsePendingToken("bad-token"))
                .thenThrow(new UnauthorizedException("Invalid pending token"));

        assertThatThrownBy(() -> authService.completeGoogleLinking(PHONE, "123456", "bad-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid pending token");
    }

    private static Nurse nurse(UUID userId, String nationalId) {
        return Nurse.builder()
                .id(UUID.randomUUID())
                .nationalId(nationalId)
                .nationalIdFrontUrl("front-url")
                .nationalIdBackUrl("back-url")
                .licenseImageUrl("license-url")
                .professionalCertificateUrl("cert-url")
                .specialization("Specialization")
                .yearsOfExperience(5)
                .bio("Bio")
                .rejectionReason(null)
                .ratingAvg(new BigDecimal("4.50"))
                .totalReviews(7)
                .verificationStatus(VerificationStatus.APPROVED)
                .build();
    }
}
