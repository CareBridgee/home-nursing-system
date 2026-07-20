package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.AllergyResponse;
import iti.jets.java.homenursing.service.AllergyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/{id}")
    public ResponseEntity<AllergyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(allergyService.getById(id));
    }
}
