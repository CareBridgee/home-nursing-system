package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.nurse.NurseLocation;
import iti.jets.java.homenursing.service.NurseLocationProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;


//temporary, replace with redis/webSocket location
@Service
public class NurseLocationProviderImpl implements NurseLocationProvider {

    @Override
    public List<NurseLocation> getNurseLocations() {
        return Collections.emptyList();
    }
}
