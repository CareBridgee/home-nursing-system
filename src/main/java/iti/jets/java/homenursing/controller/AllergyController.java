package iti.jets.java.homenursing.controller;

import iti.jets.java.homenursing.dto.catalog.AllergyResponse;
import iti.jets.java.homenursing.entity.enums.CatalogSource;
import iti.jets.java.homenursing.service.AllergyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<AllergyResponse> listAllergies(@RequestParam(required = false) CatalogSource source) {
        return source == null ? allergyService.findAll() : allergyService.findAll(source);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AllergyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(allergyService.getById(id));
    }
}
