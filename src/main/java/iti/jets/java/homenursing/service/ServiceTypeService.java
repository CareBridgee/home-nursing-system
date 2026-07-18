package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.ServiceTypeResponse;

import java.util.List;

public interface ServiceTypeService {

    List<ServiceTypeResponse> findAll();
}
