package iti.jets.java.homenursing.ai;

import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.dto.nurse.NurseResponse;
import iti.jets.java.homenursing.service.NurseService;
import iti.jets.java.homenursing.service.ServiceTypeService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HomeNursingTools {

    private final ServiceTypeService serviceTypeService;
    private final NurseService nurseService;

    public HomeNursingTools(ServiceTypeService serviceTypeService, NurseService nurseService) {
        this.serviceTypeService = serviceTypeService;
        this.nurseService = nurseService;
    }

    @Tool(description = "List all home nursing service types offered on the platform, with description and price.")
    public List<ServiceTypeResponse> listServiceTypes(
            @ToolParam(description = "Always pass an empty string \"\" or omit this parameter", required = false) String dummy) {
        return serviceTypeService.findAll();
    }

    @Tool(description = "Find verified nurses who offer a given service type. Service type name must match one returned by listServiceTypes.")
    public List<NurseResponse> findNursesForService(
            @ToolParam(description = "Exact or close service type name, e.g. 'Wound Care', 'Elderly Care'") String serviceTypeName) {
        return nurseService.findVerifiedNursesByServiceTypeName(serviceTypeName);
    }
}