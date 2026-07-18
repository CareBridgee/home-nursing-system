package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.MedicationResponse;
import iti.jets.java.homenursing.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    @GetMapping
    public List<MedicationResponse> listMedications() {
        return medicationService.findAll();
    }
}
