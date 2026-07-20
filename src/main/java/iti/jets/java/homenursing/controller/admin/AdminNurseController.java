package iti.jets.java.homenursing.controller.admin;

import iti.jets.java.homenursing.dto.nurse.NurseRejectionRequest;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import iti.jets.java.homenursing.service.AdminNurseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/nurses")
public class AdminNurseController {

    private final AdminNurseService adminNurseService;

    public AdminNurseController(AdminNurseService adminNurseService) {
        this.adminNurseService = adminNurseService;
    }

    @GetMapping
    public ResponseEntity<List<NurseResponse>> listByStatus(
            @RequestParam(required = false) VerificationStatus status) {
        if (status == null) {
            status = VerificationStatus.UNDER_REVIEW;
        }
        return ResponseEntity.ok(adminNurseService.listByStatus(status));
    }

    @PatchMapping("/{nurseId}/approve")
    public ResponseEntity<NurseResponse> approve(@PathVariable UUID nurseId) {
        return ResponseEntity.ok(adminNurseService.approve(nurseId));
    }

    @PatchMapping("/{nurseId}/reject")
    public ResponseEntity<NurseResponse> reject(
            @PathVariable UUID nurseId,
            @Valid @RequestBody NurseRejectionRequest request) {
        return ResponseEntity.ok(adminNurseService.reject(nurseId, request));
    }
}
