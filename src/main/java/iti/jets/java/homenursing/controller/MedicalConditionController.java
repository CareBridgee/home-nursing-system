package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.MedicalConditionResponse;
import iti.jets.java.homenursing.service.MedicalConditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-conditions")
@RequiredArgsConstructor
public class MedicalConditionController {

    private final MedicalConditionService medicalConditionService;

    @GetMapping
    public List<MedicalConditionResponse> listMedicalConditions() {
        return medicalConditionService.findAll();
    }
}
