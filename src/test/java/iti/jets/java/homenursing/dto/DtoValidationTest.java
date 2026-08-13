package iti.jets.java.homenursing.dto;

import iti.jets.java.homenursing.dto.auth.RefreshRequest;
import iti.jets.java.homenursing.dto.auth.VerifyOtpRequest;
import iti.jets.java.homenursing.dto.chat.SendMessageRequest;
import iti.jets.java.homenursing.dto.nurse.NurseRegistrationRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;
import iti.jets.java.homenursing.dto.profile.AddressRequest;
import iti.jets.java.homenursing.dto.profile.EmergencyContactRequest;
import iti.jets.java.homenursing.dto.profile.MedicalHistoryRequest;
import iti.jets.java.homenursing.dto.review.ReviewRatingRequest;
import iti.jets.java.homenursing.dto.servicerequest.CompleteServiceRequestRequest;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestRequest;
import iti.jets.java.homenursing.dto.user.UserUpdateRequest;
import iti.jets.java.homenursing.dto.ws.AvailabilityPayload;
import iti.jets.java.homenursing.dto.ws.ChatSendPayload;
import iti.jets.java.homenursing.dto.ws.LocationPayload;
import iti.jets.java.homenursing.dto.ws.OfferActionPayload;
import iti.jets.java.homenursing.dto.ws.OfferUpdatePayload;
import iti.jets.java.homenursing.dto.ws.ServiceRequestIdPayload;
import iti.jets.java.homenursing.entity.enums.MedicalHistoryType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Constraint coverage for the request DTOs (dto/** is excluded from the JaCoCo gate,
 * but the @Valid contract must still be proven). Covers every constraint kind used
 * across the dto tree: NotBlank, Pattern, Size, NotNull, Positive, PositiveOrZero,
 * Min, Max, DecimalMin, DecimalMax, Email, Past, AssertTrue.
 */
@Tag("unit")
class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static <T> Set<jakarta.validation.ConstraintViolation<T>> validate(T bean) {
        return validator.validate(bean);
    }

    private static <T> Set<jakarta.validation.ConstraintViolation<T>> validateField(T bean, String field) {
        return validator.validateProperty(bean, field);
    }

    // --- auth ---

    @Test
    void verifyOtpRequestValidPhoneAndOtpPass() {
        var req = VerifyOtpRequest.builder()
                .phoneNumber("+201234567890").otp("123456").build();
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void verifyOtpRequestRejectsMalformedPhone() {
        var req = VerifyOtpRequest.builder()
                .phoneNumber("not-a-phone").otp("123456").build();
        assertThat(validateField(req, "phoneNumber")).isNotEmpty();
    }

    @Test
    void verifyOtpRequestRejectsNonSixDigitOtp() {
        var req = VerifyOtpRequest.builder()
                .phoneNumber("+201234567890").otp("12").build();
        assertThat(validateField(req, "otp")).isNotEmpty();
    }

    @Test
    void refreshRequestRejectsBlankToken() {
        var req = new RefreshRequest("   ");
        assertThat(validate(req)).isNotEmpty();
    }

    // --- chat ---

    @Test
    void sendMessageRequestRejectsBlankContent() {
        assertThat(validate(new SendMessageRequest("  "))).isNotEmpty();
        assertThat(validate(new SendMessageRequest(null))).isNotEmpty();
    }

    // --- nurse registration ---

    @Test
    void nurseRegistrationRequestValidInputPasses() {
        MockMultipartFile file = new MockMultipartFile("file", "f.png", "image/png", new byte[]{1});
        var req = NurseRegistrationRequest.builder()
                .nationalId("12345678901234")
                .licenseNumber("LIC-1")
                .nationalIdFront(file)
                .nationalIdBack(file)
                .licenseImage(file)
                .professionalCertificate(file)
                .specialization("Cardiology")
                .yearsOfExperience(5)
                .build();
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void nurseRegistrationRequestRejectsInvalidNationalId() {
        var req = NurseRegistrationRequest.builder()
                .nationalId("123")
                .licenseNumber("LIC-1")
                .build();
        assertThat(validateField(req, "nationalId")).hasSize(1);
        assertThat(validateField(req, "nationalId").iterator().next().getMessage())
                .isEqualTo("National ID must contain exactly 14 digits");
    }

    @Test
    void nurseRegistrationRequestRequiresDocumentsAndExperience() {
        var req = NurseRegistrationRequest.builder().nationalId("12345678901234").build();
        Set<jakarta.validation.ConstraintViolation<NurseRegistrationRequest>> violations = validate(req);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains(
                "nationalIdFront", "nationalIdBack", "licenseImage", "professionalCertificate",
                "specialization", "yearsOfExperience");
    }

    @Test
    void nurseRegistrationRequestRejectsNegativeExperience() {
        var req = NurseRegistrationRequest.builder().yearsOfExperience(-1).build();
        assertThat(validateField(req, "yearsOfExperience")).isNotEmpty();
    }

    @Test
    void nurseRegistrationRequestRejectsOversizedLicenseNumber() {
        var req = NurseRegistrationRequest.builder()
                .licenseNumber("X".repeat(101))
                .build();
        assertThat(validateField(req, "licenseNumber")).isNotEmpty();
    }

    // --- profile: address ---

    @Test
    void addressRequestValidCityAndCoordinatesPass() {
        var req = AddressRequest.builder()
                .city("Cairo")
                .latitude(new BigDecimal("30.0444"))
                .longitude(new BigDecimal("31.2357"))
                .build();
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void addressRequestRejectsBlankCity() {
        var req = AddressRequest.builder().city(" ").build();
        assertThat(validateField(req, "city")).isNotEmpty();
    }

    @Test
    void addressRequestRejectsOutOfRangeLatitude() {
        var req = AddressRequest.builder().latitude(new BigDecimal("91.0")).build();
        assertThat(validateField(req, "latitude")).isNotEmpty();
        var below = AddressRequest.builder().latitude(new BigDecimal("-91.0")).build();
        assertThat(validateField(below, "latitude")).isNotEmpty();
    }

    @Test
    void addressRequestRejectsOutOfRangeLongitude() {
        var req = AddressRequest.builder().longitude(new BigDecimal("181.0")).build();
        assertThat(validateField(req, "longitude")).isNotEmpty();
        var below = AddressRequest.builder().longitude(new BigDecimal("-181.0")).build();
        assertThat(validateField(below, "longitude")).isNotEmpty();
    }

    @Test
    void addressRequestAcceptsBoundaryCoordinates() {
        var req = AddressRequest.builder()
                .latitude(new BigDecimal("90.0"))
                .longitude(new BigDecimal("180.0"))
                .build();
        assertThat(validateField(req, "latitude")).isEmpty();
        assertThat(validateField(req, "longitude")).isEmpty();
    }

    // --- profile: emergency contact ---

    @Test
    void emergencyContactRequestValidPasses() {
        var req = EmergencyContactRequest.builder()
                .contactName("Mother")
                .relationship("Parent")
                .phoneNumber("+20 123 456 7890")
                .build();
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void emergencyContactRequestRejectsBlankNameAndBadPhone() {
        var req = EmergencyContactRequest.builder().contactName("").phoneNumber("abc").build();
        assertThat(validateField(req, "contactName")).isNotEmpty();
        assertThat(validateField(req, "phoneNumber")).isNotEmpty();
    }

    // --- profile: medical history ---

    @Test
    void medicalHistoryRequestRejectsNullType() {
        var req = new MedicalHistoryRequest(null, "desc");
        assertThat(validate(req)).isNotEmpty();
    }

    @Test
    void medicalHistoryRequestAcceptsValidType() {
        var req = new MedicalHistoryRequest(MedicalHistoryType.SURGERY, "desc");
        assertThat(validate(req)).isEmpty();
    }

    // --- user update ---

    @Test
    void userUpdateRequestValidPasses() {
        var req = UserUpdateRequest.builder()
                .firstName("Ahmed")
                .lastName("Ali")
                .email("ahmed@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void userUpdateRequestRejectsBlankNames() {
        var req = UserUpdateRequest.builder().firstName(" ").lastName(null).build();
        assertThat(validateField(req, "firstName")).isNotEmpty();
        assertThat(validateField(req, "lastName")).isNotEmpty();
    }

    @Test
    void userUpdateRequestRejectsInvalidEmail() {
        var req = UserUpdateRequest.builder().email("not-an-email").build();
        assertThat(validateField(req, "email")).isNotEmpty();
    }

    @Test
    void userUpdateRequestRejectsFutureBirthDate() {
        var req = UserUpdateRequest.builder().dateOfBirth(LocalDate.now().plusDays(1)).build();
        assertThat(validateField(req, "dateOfBirth")).isNotEmpty();
    }

    // --- review ---

    @Test
    void reviewRatingRequestRequiresRatingInRange() {
        var req = ReviewRatingRequest.builder().serviceRequestId(UUID.randomUUID()).rating(7).build();
        assertThat(validateField(req, "rating")).isNotEmpty();
        var zero = ReviewRatingRequest.builder().rating(0).build();
        assertThat(validateField(zero, "rating")).isNotEmpty();
    }

    @Test
    void reviewRatingRequestRejectsNullServiceRequestId() {
        var req = ReviewRatingRequest.builder().rating(5).build();
        assertThat(validateField(req, "serviceRequestId")).isNotEmpty();
    }

    @Test
    void reviewRatingRequestAcceptsBoundaryRating() {
        var req = ReviewRatingRequest.builder().serviceRequestId(UUID.randomUUID()).rating(5).build();
        assertThat(validateField(req, "rating")).isEmpty();
    }

    @Test
    void reviewRatingRequestRejectsOversizedReviewText() {
        var req = ReviewRatingRequest.builder().reviewText("X".repeat(2001)).build();
        assertThat(validateField(req, "reviewText")).isNotEmpty();
    }

    // --- nurse offer ---

    @Test
    void nurseOfferRequestValidPasses() {
        var req = new NurseOfferRequest(
                UUID.randomUUID(), new BigDecimal("500.00"), LocalDate.now(), java.time.LocalTime.of(10, 0), "msg");
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void nurseOfferRequestRejectsNullAndNonPositivePrice() {
        var req = new NurseOfferRequest(UUID.randomUUID(), new BigDecimal("0"), null, null, null);
        Set<jakarta.validation.ConstraintViolation<NurseOfferRequest>> violations = validate(req);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains(
                "proposedPrice", "proposedDate", "proposedTime");
        var negative = new NurseOfferRequest(UUID.randomUUID(), new BigDecimal("-1"), LocalDate.now(),
                java.time.LocalTime.of(10, 0), null);
        assertThat(validate(negative)).isNotEmpty();
    }

    @Test
    void nurseOfferUpdateRequestRejectsNonPositivePrice() {
        var req = new NurseOfferUpdateRequest(new BigDecimal("-5"), null, null, null);
        assertThat(validateField(req, "proposedPrice")).isNotEmpty();
    }

    // --- service request ---

    @Test
    void nearbyServiceRequestRequestValidPasses() {
        var req = new NearbyServiceRequestRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("30.0"), new BigDecimal("31.0"), null, null, null);
        assertThat(validate(req)).isEmpty();
    }

    @Test
    void nearbyServiceRequestRequestRejectsMissingFields() {
        var req = new NearbyServiceRequestRequest(null, null, null, null, null, null, null);
        assertThat(validate(req)).hasSize(4);
    }

    @Test
    void nearbyServiceRequestRequestRejectsOutOfRangeCoordinates() {
        var req = new NearbyServiceRequestRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("95.0"), new BigDecimal("200.0"), null, null, null);
        Set<jakarta.validation.ConstraintViolation<NearbyServiceRequestRequest>> violations = validate(req);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("latitude", "longitude");
    }

    @Test
    void completeServiceRequestRequestRejectsBlankVisitCode() {
        assertThat(validate(new CompleteServiceRequestRequest(""))).isNotEmpty();
        assertThat(validate(new CompleteServiceRequestRequest(null))).isNotEmpty();
    }

    // --- ws payloads ---

    @Test
    void availabilityPayloadValidPasses() {
        assertThat(validate(new AvailabilityPayload(false, null, null))).isEmpty();
        assertThat(validate(new AvailabilityPayload(true, new BigDecimal("30.0"), new BigDecimal("31.0"))))
                .isEmpty();
    }

    @Test
    void availabilityPayloadRequiresLocationWhenAvailable() {
        Set<jakarta.validation.ConstraintViolation<AvailabilityPayload>> violations =
                validate(new AvailabilityPayload(true, null, null));
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage())
                .contains("lat and lng are required");
    }

    @Test
    void availabilityPayloadRejectsNullFlag() {
        assertThat(validate(new AvailabilityPayload(null, null, null))).isNotEmpty();
    }

    @Test
    void locationPayloadRejectsNullCoordinates() {
        assertThat(validate(new LocationPayload(null, null))).isNotEmpty();
    }

    @Test
    void chatSendPayloadRejectsBlankContent() {
        assertThat(validate(new ChatSendPayload("  "))).isNotEmpty();
    }

    @Test
    void offerActionPayloadRejectsNullOfferId() {
        assertThat(validate(new OfferActionPayload(null))).isNotEmpty();
    }

    @Test
    void offerUpdatePayloadRejectsNullOfferId() {
        assertThat(validate(new OfferUpdatePayload(null, null, null, null, null))).isNotEmpty();
    }

    @Test
    void serviceRequestIdPayloadRejectsNullId() {
        assertThat(validate(new ServiceRequestIdPayload(null))).isNotEmpty();
    }
}
