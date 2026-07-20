package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.service.MedicalConditionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-conditions")
public class MedicalConditionController {

    private final MedicalConditionService medicalConditionService;

    public MedicalConditionController(MedicalConditionService medicalConditionService) {
        this.medicalConditionService = medicalConditionService;
    }

    @GetMapping
    public List<MedicalConditionResponse> listMedicalConditions() {
        return medicalConditionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalConditionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(medicalConditionService.getById(id));
    }
}
