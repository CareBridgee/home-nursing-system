package iti.jets.java.homenursing.ai;

import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.service.ServiceTypeService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HomeNursingTools {

    private final ServiceTypeService serviceTypeService;

    public HomeNursingTools(ServiceTypeService serviceTypeService) {
        this.serviceTypeService = serviceTypeService;
    }

    @Tool(description = "List all home nursing service types offered on the platform, with description and price.")
    public List<ServiceTypeResponse> listServiceTypes() {
        return serviceTypeService.findAll();
    }
}