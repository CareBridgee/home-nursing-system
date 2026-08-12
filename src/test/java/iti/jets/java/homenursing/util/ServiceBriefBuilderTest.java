package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.dto.servicerequest.PatientMedicalSummary;
import iti.jets.java.homenursing.entity.Profile;
import iti.jets.java.homenursing.entity.enums.Gender;
import iti.jets.java.homenursing.service.ProfileService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ServiceBriefBuilderTest {

    @Mock
    private ProfileService profileService;
    @Mock
    private PatientMedicalSummaryAssembler patientMedicalSummaryAssembler;

    @InjectMocks
    private ServiceBriefBuilder builder;

    private final UUID profileId = UUID.randomUUID();

    private Profile stubProfile() {
        Profile profile = Profile.builder().id(profileId).build();
        when(profileService.getProfile(profileId)).thenReturn(profile);
        return profile;
    }

    private PatientMedicalSummary summaryFor(Profile profile, PatientMedicalSummary template) {
        when(patientMedicalSummaryAssembler.build(profile, false)).thenReturn(template);
        return template;
    }

    @Test
    void buildIncludesAgeGenderAndBloodTypeWithSeparators() {
        LocalDate dob = LocalDate.now().minusYears(34);
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, dob, Gender.FEMALE, "O+",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, "General Nursing");

        int age = Period.between(dob, LocalDate.now()).getYears();
        assertThat(brief).isEqualTo("Patient: " + age + "-year-old female, blood type O+."
                + " Requested service: General Nursing.");
    }

    @Test
    void buildOmitsAgeWhenDateOfBirthIsNull() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, Gender.MALE, "A-",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, null);

        assertThat(brief).isEqualTo("Patient: male, blood type A-.");
    }

    @Test
    void buildOmitsAgeWhenBornToday() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, LocalDate.now(), Gender.MALE, "A-",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, null);

        assertThat(brief).isEqualTo("Patient: male, blood type A-.");
    }

    @Test
    void buildOmitsGenderWhenNull() {
        LocalDate dob = LocalDate.now().minusYears(50);
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, dob, null, "AB+",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, "Physiotherapy");

        int age = Period.between(dob, LocalDate.now()).getYears();
        assertThat(brief).isEqualTo("Patient: " + age + "-year-old, blood type AB+."
                + " Requested service: Physiotherapy.");
    }

    @Test
    void buildOmitsBloodTypeWhenBlank() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, Gender.FEMALE, " ",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, null);

        assertThat(brief).isEqualTo("Patient: female.");
    }

    @Test
    void buildFallsBackWhenNothingIsDescribed() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, null, null,
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, null);

        assertThat(brief).isEqualTo("Patient: no profile details recorded.");
    }

    @Test
    void buildAppendsDistinctListSections() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, Gender.FEMALE, "B+",
                null, null, null, null, null, null,
                List.of("Penicillin", "Dust"),
                List.of("Diabetes", "Asthma"),
                List.of("Metformin", "Metformin", "Insulin"),
                List.of(), List.of()));

        String brief = builder.build(profileId, null);

        assertThat(brief).isEqualTo("Patient: female, blood type B+."
                + " Conditions: Diabetes, Asthma."
                + " Allergies: Penicillin, Dust."
                + " Medications: Metformin, Insulin.");
    }

    @Test
    void buildSkipsEmptyListSections() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, Gender.FEMALE, "B+",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, "Wound Care");

        assertThat(brief).isEqualTo("Patient: female, blood type B+."
                + " Requested service: Wound Care.");
    }

    @Test
    void buildCommasBloodTypeAfterAgeWhenGenderMissing() {
        LocalDate dob = LocalDate.now().minusYears(28);
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, dob, null, "B-",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, null);

        int age = Period.between(dob, LocalDate.now()).getYears();
        assertThat(brief).isEqualTo("Patient: " + age + "-year-old, blood type B-.");
    }

    @Test
    void buildStartsWithBloodTypeWhenNoAgeOrGender() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, null, "O+",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, null);

        assertThat(brief).isEqualTo("Patient: blood type O+.");
    }

    @Test
    void buildAppendsChatCareDetailsBetweenListsAndService() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, Gender.FEMALE, "B+",
                null, null, null, null, null, null,
                List.of("Penicillin"), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, "Wound Care", "  wound on right leg needs daily dressing  ");

        assertThat(brief).isEqualTo("Patient: female, blood type B+."
                + " Allergies: Penicillin."
                + " User description: wound on right leg needs daily dressing."
                + " Requested service: Wound Care.");
    }

    @Test
    void buildOmitsChatCareDetailsWhenBlank() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, Gender.FEMALE, "B+",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, "Wound Care", "   ");

        assertThat(brief).isEqualTo("Patient: female, blood type B+."
                + " Requested service: Wound Care.");
    }

    @Test
    void buildOmitsServiceWhenRequestedServiceNameIsBlank() {
        Profile profile = stubProfile();
        summaryFor(profile, new PatientMedicalSummary(
                profileId, "Mona", "Hassan", null, null, Gender.FEMALE, "B+",
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        String brief = builder.build(profileId, "   ");

        assertThat(brief).isEqualTo("Patient: female, blood type B+.");
        verify(profileService).getProfile(profileId);
    }
}
