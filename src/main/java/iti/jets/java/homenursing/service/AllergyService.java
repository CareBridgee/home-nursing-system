package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.AllergyResponse;

import java.util.List;

public interface AllergyService {

    List<AllergyResponse> findAll();
}
