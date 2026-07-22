package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.nurse.NurseLocation;

import java.util.List;


//Provide current nurse locations (id + lat/lng)
public interface NurseLocationProvider {

    List<NurseLocation> getNurseLocations();
}
