package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.profile.ProfileAllergyRequest;
import iti.jets.java.homenursing.dto.profile.ProfileMedicalConditionRequest;
import iti.jets.java.homenursing.dto.profile.ProfileMedicationRequest;
import iti.jets.java.homenursing.testutil.ApiIntegrationTestBase;
import iti.jets.java.homenursing.testutil.DevOtpAuth;
import iti.jets.java.homenursing.testutil.Json;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileFlowIntegrationTest extends ApiIntegrationTestBase {

    private record CatalogIds(UUID allergy, UUID condition, UUID medication, UUID serviceType) {
    }

    private CatalogIds seedCatalog() throws Exception {
        String stamp = "P" + System.nanoTime();
        String allergyBody = mvc.perform(post("/api/v1/admin/catalog/allergies")
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Penicillin " + stamp, "type", "DRUG"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID allergy = Json.read(allergyBody, iti.jets.java.homenursing.dto.catalog.AllergyResponse.class).id();

        String condBody = mvc.perform(post("/api/v1/admin/catalog/medical-conditions")
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Asthma " + stamp, "description", "resp"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID condition = Json.read(condBody, iti.jets.java.homenursing.dto.catalog.MedicalConditionResponse.class).id();

        String medBody = mvc.perform(post("/api/v1/admin/catalog/medications")
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Ventolin " + stamp))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID medication = Json.read(medBody, iti.jets.java.homenursing.dto.catalog.MedicationResponse.class).id();

        String stBody = mvc.perform(multipart("/api/v1/admin/catalog/service-types")
                        .file(image("file"))
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .param("name", "General Nursing " + stamp)
                        .param("estimatedDurationMinutes", "120")
                        .param("basePrice", "500.00"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID serviceType = Json.read(stBody, iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse.class).id();
        return new CatalogIds(allergy, condition, medication, serviceType);
    }

    private void seedCatalogState() throws Exception {
        seedCatalog();
    }

    @Test
    void user_me_andList() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200001");
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber", is("+201111200001")));
        mvc.perform(get("/api/v1/users").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())));
        mvc.perform(get("/api/v1/users").param("page", "0").param("size", "1").param("sort", "firstName,asc")
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk());
        DevOtpAuth.Tokens other = DevOtpAuth.loginPatient(mvc, "+201111200002");
        String id = Json.read(mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(tokens)))
                .andReturn().getResponse().getContentAsString(), iti.jets.java.homenursing.dto.user.UserResponse.class).getId().toString();
        mvc.perform(get("/api/v1/users/{id}", id).header("Authorization", bearer(other)))
                .andExpect(status().isOk());
    }

    @Test
    void user_updateAndDeleteMe() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200003");
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/users/me")
                        .file(image("profile"))
                        .header("Authorization", bearer(tokens))
                        .param("firstName", "Mona")
                        .param("lastName", "Ali")
                        .param("email", "mona@test.io"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Mona")))
                .andExpect(jsonPath("$.email", is("mona@test.io")));
        mvc.perform(delete("/api/v1/users/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/profile").param("phoneNumber", "+201111200003"))
                .andExpect(status().isNotFound());
    }

    @Test
    void profile_fullLifecycle() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200004");
        String created = mvc.perform(multipart("/api/v1/profiles")
                        .file(image("profile"))
                        .header("Authorization", bearer(tokens))
                        .param("firstName", "Sara")
                        .param("lastName", "Hassan")
                        .param("gender", "FEMALE")
                        .param("dateOfBirth", "1995-05-05")
                        .param("bloodType", "O+"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/profiles/")))
                .andReturn().getResponse().getContentAsString();
        String id = Json.read(created, iti.jets.java.homenursing.dto.profile.ProfileResponse.class).getId().toString();

        mvc.perform(get("/api/v1/profiles").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/v1/profiles/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Sara")));
        mvc.perform(get("/api/v1/profiles/default").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk());

        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/profiles/{id}", id)
                        .file(image("profile"))
                        .header("Authorization", bearer(tokens))
                        .param("firstName", "Sara Updated")
                        .param("gender", "FEMALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Sara Updated")));

        mvc.perform(delete("/api/v1/profiles/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/profiles/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
    }

    @Test
    void address_crud() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200005");
        UUID profileId = defaultProfileId(tokens);

        mvc.perform(post("/api/v1/profiles/{profileId}/address", profileId)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("country", "Egypt", "city", "Cairo",
                                "street", "Main St", "latitude", 30.1, "longitude", 31.2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.city", is("Cairo")));

        mvc.perform(get("/api/v1/profiles/{profileId}/address", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city", is("Cairo")));

        mvc.perform(put("/api/v1/profiles/{profileId}/address", profileId)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("country", "Egypt", "city", "Giza"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city", is("Giza")));

        mvc.perform(delete("/api/v1/profiles/{profileId}/address", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/profiles/{profileId}/address", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
    }

    @Test
    void emergencyContacts_crud() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200006");
        UUID profileId = defaultProfileId(tokens);

        String body = mvc.perform(post("/api/v1/profiles/{profileId}/emergency-contacts", profileId)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("contactName", "Mom", "relationship", "Mother",
                                "phoneNumber", "+201000000001"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/emergency-contacts/")))
                .andReturn().getResponse().getContentAsString();
        String id = Json.read(body, iti.jets.java.homenursing.dto.profile.EmergencyContactResponse.class).getId().toString();

        mvc.perform(get("/api/v1/profiles/{profileId}/emergency-contacts", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/emergency-contacts/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName", is("Mom")));

        mvc.perform(put("/api/v1/emergency-contacts/{id}", id)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("contactName", "Dad", "relationship", "Father",
                                "phoneNumber", "+201000000002"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName", is("Dad")));

        mvc.perform(delete("/api/v1/emergency-contacts/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());
    }

    @Test
    void medicalHistory_crud() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200007");
        UUID profileId = defaultProfileId(tokens);

        String body = mvc.perform(post("/api/v1/profiles/{profileId}/medical-history", profileId)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("type", "SURGERY", "description", "Appendix removal"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", is("Appendix removal")))
                .andReturn().getResponse().getContentAsString();
        String id = Json.read(body, iti.jets.java.homenursing.dto.profile.MedicalHistoryResponse.class).getId().toString();

        mvc.perform(get("/api/v1/profiles/{profileId}/medical-history", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/medical-history/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/medical-history/{id}", id)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("type", "OTHER", "description", "Checkup"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Checkup")));

        mvc.perform(delete("/api/v1/medical-history/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());
    }

    @Test
    void profileAllergy_addListRemove() throws Exception {
        seedCatalogState();
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200008");
        UUID profileId = defaultProfileId(tokens);

        mvc.perform(post("/api/v1/profiles/{profileId}/allergies", profileId)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(new ProfileAllergyRequest(
                                findAllergyId(tokens), "Peanuts", null))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/profiles/{profileId}/allergies", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/v1/profiles/{profileId}/allergies/{allergyId}", profileId, findAllergyId(tokens))
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/profiles/{profileId}/allergies", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void profileMedicalCondition_addListRemove() throws Exception {
        seedCatalogState();
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200009");
        UUID profileId = defaultProfileId(tokens);

        mvc.perform(post("/api/v1/profiles/{profileId}/medical-conditions", profileId)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(new ProfileMedicalConditionRequest(
                                findConditionId(tokens), "Asthma", null))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/profiles/{profileId}/medical-conditions", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/v1/profiles/{profileId}/medical-conditions/{cid}", profileId, findConditionId(tokens))
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());
    }

    @Test
    void profileMedication_addListRemove() throws Exception {
        seedCatalogState();
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200010");
        UUID profileId = defaultProfileId(tokens);

        mvc.perform(post("/api/v1/profiles/{profileId}/medications", profileId)
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(new ProfileMedicationRequest(
                                findMedicationId(tokens), "Ventolin"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/profiles/{profileId}/medications", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/v1/profiles/{profileId}/medications/{mid}", profileId, findMedicationId(tokens))
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());
    }

    @Test
    void profileReport_generates() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200011");
        UUID profileId = defaultProfileId(tokens);
        mvc.perform(get("/api/v1/profiles/report/{profileId}/report", profileId)
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId", is(profileId.toString())));
    }

    @Test
    void crossUserAccess_forbidden() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111200012");
        DevOtpAuth.Tokens other = DevOtpAuth.loginPatient(mvc, "+201111200013");
        UUID otherProfile = defaultProfileId(other);

        mvc.perform(get("/api/v1/profiles/{id}", otherProfile).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/profiles/{id}", otherProfile).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
    }

    private UUID defaultProfileId(DevOtpAuth.Tokens tokens) throws Exception {
        String body = mvc.perform(get("/api/v1/profiles/default").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.profile.ProfileResponse.class).getId();
    }

    private UUID findAllergyId(DevOtpAuth.Tokens tokens) throws Exception {
        return Json.read(mvc.perform(get("/api/v1/allergies").param("source", "ADMIN"))
                .andReturn().getResponse().getContentAsString(),
                iti.jets.java.homenursing.dto.catalog.AllergyResponse[].class)[0].id();
    }

    private UUID findConditionId(DevOtpAuth.Tokens tokens) throws Exception {
        return Json.read(mvc.perform(get("/api/v1/medical-conditions").param("source", "ADMIN"))
                .andReturn().getResponse().getContentAsString(),
                iti.jets.java.homenursing.dto.catalog.MedicalConditionResponse[].class)[0].id();
    }

    private UUID findMedicationId(DevOtpAuth.Tokens tokens) throws Exception {
        return Json.read(mvc.perform(get("/api/v1/medications").param("source", "ADMIN"))
                .andReturn().getResponse().getContentAsString(),
                iti.jets.java.homenursing.dto.catalog.MedicationResponse[].class)[0].id();
    }
}
