package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferRequest;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferResponse;
import iti.jets.java.homenursing.dto.nurseoffer.NurseOfferUpdateRequest;
import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.NurseOfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nurse-offers")
public class NurseOfferController {

    private final NurseOfferService nurseOfferService;

    public NurseOfferController(NurseOfferService nurseOfferService) {
        this.nurseOfferService = nurseOfferService;
    }

    @PostMapping
    public ResponseEntity<NurseOfferResponse> create(@Valid @RequestBody NurseOfferRequest request) {
        NurseOfferResponse response = nurseOfferService.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/nurse-offers/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<NurseOfferResponse>> listByServiceRequest(
            @RequestParam UUID serviceRequestId) {
        return ResponseEntity.ok(nurseOfferService.listByServiceRequest(serviceRequestId, SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NurseOfferResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(nurseOfferService.get(id, SecurityUtils.currentUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NurseOfferResponse> update(@PathVariable UUID id,
                                                     @RequestBody NurseOfferUpdateRequest request) {
        return ResponseEntity.ok(nurseOfferService.update(id, SecurityUtils.currentUserId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        nurseOfferService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }
}
