package iti.jets.java.homenursing.ai;

import iti.jets.java.homenursing.dto.ServiceTypeResponse;
import iti.jets.java.homenursing.service.ServiceTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class HomeNursingTools {

    private static final Logger log = LoggerFactory.getLogger(HomeNursingTools.class);

    private final ServiceTypeService serviceTypeService;

    public HomeNursingTools(ServiceTypeService serviceTypeService) {
        this.serviceTypeService = serviceTypeService;
    }

    @Tool(description = "List all home nursing service types offered on the platform, with description and price.")
    public String listServiceTypes() {
        try {
            List<ServiceTypeResponse> types = serviceTypeService.findAll();
            if (types.isEmpty()) {
                return "No service types are currently available.";
            }
            return types.stream()
                    .map(t -> t.name() + " - " + t.description() + " - " + t.basePrice() + " EGP"
                            + " (id: " + t.id() + ")")
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("Service type list failed: {}", e.getMessage());
            return "The service list is temporarily unavailable. Please try again later.";
        }
    }
}
