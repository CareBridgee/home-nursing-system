package iti.jets.java.homenursing.service;

import java.util.UUID;

public interface ProfileReportService {
    String generateReport(UUID profileId, UUID userId);
}