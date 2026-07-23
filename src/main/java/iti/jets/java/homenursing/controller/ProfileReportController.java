package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.security.SecurityUtils;
import iti.jets.java.homenursing.service.ProfileReportService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/report")
public class ProfileReportController {

    private final ProfileReportService profileReportService;

    public ProfileReportController(ProfileReportService profileReportService) {
        this.profileReportService = profileReportService;
    }

    public record ReportResponse(UUID profileId, String report) {}

    @GetMapping("/{profileId}/report")
    public ReportResponse generateReport(@PathVariable UUID profileId) {
        UUID userId = SecurityUtils.currentUserId();
        String report = profileReportService.generateReport(profileId, userId);
        return new ReportResponse(profileId, report);
    }
}