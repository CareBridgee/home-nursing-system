package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.MedicationResponse;
import iti.jets.java.homenursing.service.MedicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    public List<MedicationResponse> listMedications() {
        return medicationService.findAll();
    }
}
