package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.AllergyResponse;
import iti.jets.java.homenursing.service.AllergyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/allergies")
public class AllergyController {

    private final AllergyService allergyService;

    public AllergyController(AllergyService allergyService) {
        this.allergyService = allergyService;
    }

    @GetMapping
    public List<AllergyResponse> listAllergies() {
        return allergyService.findAll();
    }
}
