package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.testutil.ApiIntegrationTestBase;
import iti.jets.java.homenursing.testutil.DevOtpAuth;
import iti.jets.java.homenursing.testutil.Json;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogIntegrationTest extends ApiIntegrationTestBase {

    private String allergyId() throws Exception {
        String body = mvc.perform(post("/api/v1/admin/catalog/allergies")
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Peanuts " + System.nanoTime(), "type", "FOOD"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", not(blankString())))
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.catalog.AllergyResponse.class).id().toString();
    }

    @Test
    void publicCatalog_readsAndFilters() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111150001");
        String id = allergyId();
        mvc.perform(get("/api/v1/allergies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/allergies").param("source", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/allergies").param("source", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/allergies/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", not(blankString())));
    }

    @Test
    void adminAllergy_crud() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111150002");
        String id = allergyId();
        mvc.perform(put("/api/v1/admin/catalog/allergies/{id}", id)
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Peanuts Updated", "type", "FOOD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Peanuts Updated")));
        mvc.perform(delete("/api/v1/admin/catalog/allergies/{id}", id)
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/allergies/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminAllergy_requiresApiKey() throws Exception {
        mvc.perform(post("/api/v1/admin/catalog/allergies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "No Key", "type", "FOOD"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/catalog/allergies")
                        .header("X-Admin-API-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Bad Key", "type", "FOOD"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void medicalCondition_crudAndReads() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111150004");
        String body = mvc.perform(post("/api/v1/admin/catalog/medical-conditions")
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Condition " + System.nanoTime(), "description", "d"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = Json.read(body, iti.jets.java.homenursing.dto.catalog.MedicalConditionResponse.class).id().toString();

        mvc.perform(get("/api/v1/medical-conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/medical-conditions").param("source", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/medical-conditions/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/admin/catalog/medical-conditions/{id}", id)
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Condition Updated", "description", "d2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Condition Updated")));
        mvc.perform(delete("/api/v1/admin/catalog/medical-conditions/{id}", id)
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isNoContent());
    }

    @Test
void medication_crudAndReads() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111150003");
        String body = mvc.perform(post("/api/v1/admin/catalog/medications")
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Med " + System.nanoTime()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = Json.read(body, iti.jets.java.homenursing.dto.catalog.MedicationResponse.class).id().toString();

        mvc.perform(get("/api/v1/medications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/medications/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isOk());

        mvc.perform(put("/api/v1/admin/catalog/medications/{id}", id)
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("name", "Med Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Med Updated")));
        mvc.perform(delete("/api/v1/admin/catalog/medications/{id}", id)
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isNoContent());
    }

    @Test
    void serviceType_publicReadsAndAdminCrud() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111150005");
        String body = mvc.perform(multipart("/api/v1/admin/catalog/service-types")
                        .file(image("file"))
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .param("name", "General Nursing")
                        .param("description", "Basic nursing")
                        .param("category", "NURSING")
                        .param("estimatedDurationMinutes", "120")
                        .param("basePrice", "500.00"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = Json.read(body, iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse.class).id().toString();

        mvc.perform(get("/api/v1/service-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/v1/service-types/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isOk());

        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/catalog/service-types/{id}", id)
                        .file(image("file"))
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY)
                        .param("name", "General Nursing Renamed")
                        .param("estimatedDurationMinutes", "90")
                        .param("basePrice", "600.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("General Nursing Renamed")));

        mvc.perform(delete("/api/v1/admin/catalog/service-types/{id}", id)
                        .header("X-Admin-API-Key", DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/service-types/{id}", id).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
    }

    @Test
    void serviceTypeCreate_publicEndpoint() throws Exception {
        mvc.perform(multipart("/api/v1/service-types")
                        .file(image("file"))
                        .param("name", "Publicly Created " + System.nanoTime())
                        .param("basePrice", "100.00"))
                .andExpect(status().isOk());
    }

    @Test
    void catalogNotFound_details() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111150006");
        mvc.perform(get("/api/v1/allergies/{id}", java.util.UUID.randomUUID()).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/medical-conditions/{id}", java.util.UUID.randomUUID()).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/medications/{id}", java.util.UUID.randomUUID()).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/service-types/{id}", java.util.UUID.randomUUID()).header("Authorization", bearer(tokens)))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadEndpoint_returnsUrl() throws Exception {
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111100070");
        mvc.perform(multipart("/api/v1/upload").file(image("file"))
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", not(blankString())));
    }

    @Test
    void uploadEndpoint_requiresAuthAndFile() throws Exception {
        mvc.perform(multipart("/api/v1/upload").file(image("file")))
                .andExpect(status().isForbidden());
        DevOtpAuth.Tokens tokens = DevOtpAuth.loginPatient(mvc, "+201111100071");
        mvc.perform(multipart("/api/v1/upload")
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isBadRequest());
    }
}
