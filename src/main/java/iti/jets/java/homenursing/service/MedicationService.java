package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.MedicationResponse;

import java.util.List;

public interface MedicationService {

    List<MedicationResponse> findAll();
}
