package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.servicerequest.NearbyServiceRequestResponse;
import iti.jets.java.homenursing.dto.servicerequest.VisitCodeResponse;
import iti.jets.java.homenursing.service.impl.WebSocketPresenceService;
import iti.jets.java.homenursing.testutil.ApiIntegrationTestBase;
import iti.jets.java.homenursing.testutil.DevOtpAuth;
import iti.jets.java.homenursing.testutil.Json;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 3: nurse registration/approval, service requests, offers,
 * visit codes, completion and reviews - mirrors happy-offer/lifecycle/review e2e scripts.
 */
class NurseFlowIntegrationTest extends ApiIntegrationTestBase {

    private static final String LAT = "30.0444";
    private static final String LNG = "31.2357";

    @Autowired
    private WebSocketPresenceService presenceService;

    private record NurseAndId(String nurseId, UUID serviceTypeId) {
    }

    private static String nationalIdFor(String phone) {
        return "30000000" + phone.substring(phone.length() - 6);
    }

    private void markNurseAvailable(DevOtpAuth.Tokens nurseTokens) throws Exception {
        String body = mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String userId = Json.read(body, iti.jets.java.homenursing.dto.user.UserResponse.class).getId().toString();
        presenceService.markAvailable(userId, Double.parseDouble(LAT), Double.parseDouble(LNG));
    }

    private UUID seedServiceType() throws Exception {
        String stamp = "N" + System.nanoTime();
        String body = mvc.perform(multipart("/api/v1/admin/catalog/service-types")
                        .file(image("file"))
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY)
                        .param("name", "Nurse Flow " + stamp)
                        .param("description", "basic")
                        .param("category", "NURSING")
                        .param("estimatedDurationMinutes", "120")
                        .param("basePrice", "500.00"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse.class).id();
    }

    private String registerNurse(DevOtpAuth.Tokens nurseTokens, String nationalId) throws Exception {
        String body = mvc.perform(multipart("/api/v1/nurses/register")
                        .file(image("nationalIdFront"))
                        .file(image("nationalIdBack"))
                        .file(image("licenseImage"))
                        .file(image("professionalCertificate"))
                        .file(image("profileImage"))
                        .header("Authorization", bearer(nurseTokens))
                        .param("nationalId", nationalId)
                        .param("licenseNumber", "LIC-" + nationalId)
                        .param("specialization", "General Nursing")
                        .param("yearsOfExperience", "5")
                        .param("bio", "Test nurse"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, NurseResponse.class).getId().toString();
    }

    private void approveNurse(String nurseId) throws Exception {
        mvc.perform(patch("/api/v1/admin/nurses/{nurseId}/approve", nurseId)
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("APPROVED")));
    }

    private NurseAndId registeredNurse(DevOtpAuth.Tokens nurseTokens, String phone) throws Exception {
        UUID serviceTypeId = seedServiceType();
        String nurseId = registerNurse(nurseTokens, nationalIdFor(phone));
        approveNurse(nurseId);
        mvc.perform(post("/api/v1/nurses/{nurseId}/services", nurseId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(java.util.List.of(Map.of("serviceTypeId", serviceTypeId.toString())))))
                .andExpect(status().isOk());
        return new NurseAndId(nurseId, serviceTypeId);
    }

    private UUID createServiceRequest(DevOtpAuth.Tokens tokens, UUID profileId, UUID serviceTypeId) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "profileId", profileId.toString(),
                                "serviceTypeId", serviceTypeId.toString(),
                                "latitude", LAT,
                                "longitude", LNG,
                                "preferredDate", LocalDate.now().plusDays(3).toString(),
                                "preferredTime", "11:00"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/service-requests/")))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return Json.read(body, NearbyServiceRequestResponse.class).serviceRequestId();
    }

    private UUID createOffer(DevOtpAuth.Tokens nurseTokens, UUID serviceRequestId, String price) throws Exception {
        String body = mvc.perform(post("/api/v1/nurse-offers")
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", serviceRequestId.toString(),
                                "proposedPrice", price,
                                "proposedDate", LocalDate.now().plusDays(4).toString(),
                                "proposedTime", "10:00",
                                "message", "I can help"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, NurseOfferResponse.class).id();
    }

    @Test
    void nurseRegistration_adminApproval_andUpdate() throws Exception {
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300001");
        String nurseId = registerNurse(nurseTokens, "30000000130001");

        mvc.perform(get("/api/v1/nurses/{nurseId}", nurseId)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("UNDER_REVIEW")));
        mvc.perform(multipart("/api/v1/nurses/register")
                        .file(image("nationalIdFront"))
                        .file(image("nationalIdBack"))
                        .file(image("licenseImage"))
                        .file(image("professionalCertificate"))
                        .header("Authorization", bearer(nurseTokens))
                        .param("nationalId", "30000000130001")
                        .param("licenseNumber", "LIC-DUP")
                        .param("specialization", "X")
                        .param("yearsOfExperience", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialization", is("X")));

        DevOtpAuth.Tokens otherNurse = DevOtpAuth.loginNurse(mvc, "+201111300022");
        mvc.perform(multipart("/api/v1/nurses/register")
                        .file(image("nationalIdFront"))
                        .file(image("nationalIdBack"))
                        .file(image("licenseImage"))
                        .file(image("professionalCertificate"))
                        .header("Authorization", bearer(otherNurse))
                        .param("nationalId", "30000000130001")
                        .param("licenseNumber", "LIC-OTHER")
                        .param("yearsOfExperience", "3"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/admin/nurses")
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + nurseId + "')]", hasSize(1)));
        mvc.perform(get("/api/v1/admin/nurses").param("status", "APPROVED")
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + nurseId + "')]", hasSize(0)));

        approveNurse(nurseId);
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/nurses/{nurseId}", nurseId)
                        .file(image("profileImage"))
                        .header("Authorization", bearer(nurseTokens))
                        .param("specialization", "Pediatric Care")
                        .param("yearsOfExperience", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization", is("Pediatric Care")))
                .andExpect(jsonPath("$.yearsOfExperience", is(7)));

        mvc.perform(get("/api/v1/nurses")
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + nurseId + "')]", hasSize(1)));

        DevOtpAuth.Tokens rejectedNurse = DevOtpAuth.loginNurse(mvc, "+201111300023");
        String rejectedId = registerNurse(rejectedNurse, nationalIdFor("+201111300023"));
        mvc.perform(patch("/api/v1/admin/nurses/{nurseId}/reject", rejectedId)
                        .header(DevOtpAuth.ADMIN_KEY_HEADER, DevOtpAuth.ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "overallReason", "Missing documents",
                                "failedSteps", List.of(Map.of("step", "DOCUMENTS", "reason", "unreadable"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("REJECTED")));
    }

    @Test
    void nurse_removeService_and_nearbyOffersListing() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300024");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300025");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111300025");

        UUID requestId = createServiceRequest(patient, profileId, nurse.serviceTypeId());
        UUID offerId = createOffer(nurseTokens, requestId, "500.00");
        markNurseAvailable(nurseTokens);

        mvc.perform(get("/api/v1/nurse-offers/nearby")
                        .param("serviceRequestId", requestId.toString())
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + offerId + "')]", hasSize(1)));

        mvc.perform(delete("/api/v1/nurses/{nurseId}/services/{serviceTypeId}", nurse.nurseId(), nurse.serviceTypeId())
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isNoContent());
    }

    @Test
    void serviceRequest_offerAccept_visitCode_complete_review_fullCycle() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300002");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300003");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111300003");

        UUID requestId = createServiceRequest(patient, profileId, nurse.serviceTypeId());

        UUID offerId = createOffer(nurseTokens, requestId, "500.00");
        mvc.perform(post("/api/v1/nurse-offers")
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(),
                                "proposedPrice", "600.00",
                                "proposedDate", LocalDate.now().plusDays(4).toString(),
                                "proposedTime", "12:00"))))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/nurse-offers").param("serviceRequestId", requestId.toString())
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nurse.id", is(nurse.nurseId)));

        mvc.perform(patch("/api/v1/nurse-offers/{offerId}/accept", offerId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        mvc.perform(get("/api/v1/service-requests/{id}", requestId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.nurse.id", is(nurse.nurseId)));

        mvc.perform(get("/api/v1/service-requests/current")
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRequestId", is(requestId.toString())));

        String codeBody = mvc.perform(post("/api/v1/service-requests/{id}/visit-code", requestId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String code = Json.read(codeBody, VisitCodeResponse.class).code();

        mvc.perform(post("/api/v1/service-requests/{id}/complete", requestId)
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("visitCode", code))))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/service-requests/{id}/complete", requestId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("visitCode", "XXXX0000"))))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/v1/service-requests/{id}/complete", requestId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("visitCode", code))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/service-requests/current")
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isNotFound());

        MvcResult reviewCreated = mvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(),
                                "rating", 5,
                                "reviewText", "Excellent care",
                                "isAnonymous", false))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/reviews/")))
                .andReturn();
        String reviewId = reviewCreated.getResponse().getHeader("Location").substring(
                reviewCreated.getResponse().getHeader("Location").lastIndexOf('/') + 1);

        mvc.perform(get("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", is(5)));

        mvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(),
                                "rating", 4))))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/nurses/{nurseId}/reviews", nurse.nurseId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].rating", is(5)));
        mvc.perform(get("/api/v1/nurses/{nurseId}", nurse.nurseId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingAvg", notNullValue()))
                .andExpect(jsonPath("$.totalReviews", is(1)));
    }

    @Test
    void offer_rules_counter_update_reject_withdraw() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300004");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300005");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111300005");
        UUID requestId = createServiceRequest(patient, profileId, nurse.serviceTypeId());

        UUID offerId = createOffer(nurseTokens, requestId, "500.00");

        mvc.perform(patch("/api/v1/nurse-offers/{id}/counter", offerId)
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("proposedPrice", "450.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposedPrice", is(450.0)));

        mvc.perform(put("/api/v1/nurse-offers/{id}", offerId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("message", "New terms agreed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("New terms agreed")));

        mvc.perform(patch("/api/v1/nurse-offers/{id}/reject", offerId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isNoContent());

        UUID offer2 = createOffer(nurseTokens, requestId, "550.00");
        mvc.perform(delete("/api/v1/nurse-offers/{id}", offer2)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/nurse-offers/{id}", offer2)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("WITHDRAWN")));
    }

    @Test
    void request_cancel_andStatusRules() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300006");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300007");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111300007");
        DevOtpAuth.Tokens stranger = DevOtpAuth.loginPatient(mvc, "+201111300008");

        mvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "profileId", profileId.toString(),
                                "serviceTypeId", nurse.serviceTypeId().toString(),
                                "latitude", LAT,
                                "longitude", LNG,
                                "preferredDate", LocalDate.now().minusDays(1).toString()))))
                .andExpect(status().isBadRequest());

        UUID requestId = createServiceRequest(patient, profileId, nurse.serviceTypeId());

        mvc.perform(patch("/api/v1/service-requests/{id}/cancel", requestId)
                        .header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/service-requests/{id}/cancel", requestId)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/api/v1/service-requests/{id}/cancel", requestId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/service-requests/{id}", requestId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
        mvc.perform(patch("/api/v1/service-requests/{id}/cancel", requestId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isBadRequest());

        UUID request2 = createServiceRequest(patient, profileId, nurse.serviceTypeId());
        UUID offerId = createOffer(nurseTokens, request2, "500.00");
        mvc.perform(patch("/api/v1/nurse-offers/{id}/accept", offerId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/service-requests")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "profileId", profileId.toString(),
                                "serviceTypeId", nurse.serviceTypeId().toString(),
                                "latitude", LAT,
                                "longitude", LNG,
                                "preferredDate", LocalDate.now().plusDays(2).toString()))))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/service-requests/{id}/cancel", request2)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/service-requests/{id}", request2)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void eligibility_rules_unapprovedAndActiveVisit() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300009");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300010");
        UUID serviceTypeId = seedServiceType();

        String unapprovedId = registerNurse(nurseTokens, "30000000130010");
        mvc.perform(post("/api/v1/nurses/{nurseId}/services", unapprovedId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(java.util.List.of(Map.of("serviceTypeId", serviceTypeId.toString())))))
                .andExpect(status().isOk());

        UUID requestId = createServiceRequest(patient, profileId, serviceTypeId);
        mvc.perform(post("/api/v1/nurse-offers")
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(),
                                "proposedPrice", "500.00",
                                "proposedDate", LocalDate.now().plusDays(4).toString(),
                                "proposedTime", "10:00"))))
                .andExpect(status().isBadRequest());

        DevOtpAuth.Tokens nurse2 = DevOtpAuth.loginNurse(mvc, "+201111300011");
        String nurse2Id = registerNurse(nurse2, "30000000130011");
        approveNurse(nurse2Id);
        mvc.perform(post("/api/v1/nurses/{nurseId}/services", nurse2Id)
                        .header("Authorization", bearer(nurse2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(java.util.List.of(Map.of("serviceTypeId", serviceTypeId.toString())))))
                .andExpect(status().isOk());

        UUID offerId = createOffer(nurse2, requestId, "500.00");
        mvc.perform(patch("/api/v1/nurse-offers/{id}/accept", offerId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk());

        DevOtpAuth.Tokens patient2 = DevOtpAuth.loginPatient(mvc, "+201111300021");
        UUID profile2 = defaultProfileId(patient2);
        UUID request2 = createServiceRequest(patient2, profile2, serviceTypeId);
        mvc.perform(post("/api/v1/nurse-offers")
                        .header("Authorization", bearer(nurse2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", request2.toString(),
                                "proposedPrice", "500.00",
                                "proposedDate", LocalDate.now().plusDays(6).toString(),
                                "proposedTime", "10:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serviceRequest_nurseAndPreviewEndpoints() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300012");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300013");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111300013");

        UUID requestId = createServiceRequest(patient, profileId, nurse.serviceTypeId());

        mvc.perform(get("/api/v1/service-requests/nearby")
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/service-requests/current")
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/service-requests/{id}/preview", requestId)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/service-requests/{id}/profile", requestId)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/service-requests/confirmed")
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));

        markNurseAvailable(nurseTokens);
        mvc.perform(get("/api/v1/service-requests/{id}/preview", requestId)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRequestId", is(requestId.toString())));

        UUID offerId = createOffer(nurseTokens, requestId, "500.00");
        mvc.perform(patch("/api/v1/nurse-offers/{id}/accept", offerId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/service-requests/{id}/preview", requestId)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/service-requests/{id}/profile", requestId)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/service-requests/nurse/history")
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/service-requests/nurse/history")
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].serviceRequestId", is(requestId.toString())))
                .andExpect(jsonPath("$[0].status", is("ACCEPTED")))
                .andExpect(jsonPath("$[0].patientFirstName", notNullValue()));
    }

    @Test
    void visitCode_rules_requireAcceptedRequest() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300014");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300015");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111300015");
        UUID requestId = createServiceRequest(patient, profileId, nurse.serviceTypeId());

        mvc.perform(post("/api/v1/service-requests/{id}/visit-code", requestId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isBadRequest());

        UUID offerId = createOffer(nurseTokens, requestId, "500.00");
        mvc.perform(patch("/api/v1/nurse-offers/{id}/accept", offerId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/service-requests/{id}/visit-code", requestId)
                        .header("Authorization", bearer(nurseTokens)))
                .andExpect(status().isNotFound());
    }

    @Test
    void review_rules_requireOwnershipAndCompletion() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300016");
        UUID profileId = defaultProfileId(patient);
        DevOtpAuth.Tokens nurseTokens = DevOtpAuth.loginNurse(mvc, "+201111300017");
        NurseAndId nurse = registeredNurse(nurseTokens, "+201111300017");
        DevOtpAuth.Tokens stranger = DevOtpAuth.loginPatient(mvc, "+201111300018");

        UUID requestId = createServiceRequest(patient, profileId, nurse.serviceTypeId());

        mvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(), "rating", 5))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(), "rating", 5))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(), "rating", 6))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", UUID.randomUUID().toString(), "rating", 5))))
                .andExpect(status().isNotFound());

        UUID offerId = createOffer(nurseTokens, requestId, "500.00");
        mvc.perform(patch("/api/v1/nurse-offers/{id}/accept", offerId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk());
        String code = Json.read(mvc.perform(post("/api/v1/service-requests/{id}/visit-code", requestId)
                .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), VisitCodeResponse.class).code();
        mvc.perform(post("/api/v1/service-requests/{id}/complete", requestId)
                        .header("Authorization", bearer(nurseTokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of("visitCode", code))))
                .andExpect(status().isNoContent());

        MvcResult created = mvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(),
                                "rating", 4,
                                "reviewText", "Good",
                                "isAnonymous", true))))
                .andExpect(status().isCreated())
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        String reviewId = location.substring(location.lastIndexOf('/') + 1);

        mvc.perform(put("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(),
                                "rating", 1))))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "serviceRequestId", requestId.toString(),
                                "rating", 4,
                                "reviewText", "Updated text"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewText", is("Updated text")));
        mvc.perform(delete("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/reviews/{id}", reviewId)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isNoContent());
    }

    @Test
    void notifications_crud_andOwnership() throws Exception {
        DevOtpAuth.Tokens patient = DevOtpAuth.loginPatient(mvc, "+201111300019");
        DevOtpAuth.Tokens other = DevOtpAuth.loginPatient(mvc, "+201111300020");

        MvcResult created = mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(Json.write(Map.of(
                                "title", "Booking update",
                                "message", "Your request is confirmed",
                                "type", "BOOKING"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/notifications/")))
                .andReturn();
        String id = created.getResponse().getHeader("Location").substring(
                created.getResponse().getHeader("Location").lastIndexOf('/') + 1);

        mvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Booking update")));
        mvc.perform(get("/api/v1/notifications")
                        .param("after", "2026-01-01T00:00:00")
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title", is("Booking update")));
        mvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/notifications/{id}", id)
                        .header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/notifications/{id}", id)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(false)));
        mvc.perform(patch("/api/v1/notifications/{id}/read", id)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)));
        mvc.perform(delete("/api/v1/notifications/{id}", id)
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isNoContent());
    }

    private UUID defaultProfileId(DevOtpAuth.Tokens tokens) throws Exception {
        String body = mvc.perform(get("/api/v1/profiles/default").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Json.read(body, iti.jets.java.homenursing.dto.profile.ProfileResponse.class).getId();
    }
}
