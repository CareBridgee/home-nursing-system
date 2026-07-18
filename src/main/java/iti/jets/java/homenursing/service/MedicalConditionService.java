package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.MedicalConditionResponse;

import java.util.List;

public interface MedicalConditionService {

    List<MedicalConditionResponse> findAll();
}
