package iti.jets.java.homenursing.ai;

import iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse;
import iti.jets.java.homenursing.service.ServiceTypeService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class HomeNursingToolsTest {

    @Mock
    private ServiceTypeService serviceTypeService;

    @InjectMocks
    private HomeNursingTools tools;

    @Test
    void listsEveryServiceTypeWithNameDescriptionPriceAndId() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        when(serviceTypeService.findAll()).thenReturn(List.of(
                serviceType(firstId, "General Nursing", "Daily care", "120.50"),
                serviceType(secondId, "ICU Care", "Critical care", "450.00")));

        String result = tools.listServiceTypes();

        assertThat(result).contains(
                "General Nursing - Daily care - 120.50 EGP (id: " + firstId + ")",
                "ICU Care - Critical care - 450.00 EGP (id: " + secondId + ")");
    }

    @Test
    void returnsUnavailableMessageWhenNoServiceTypesExist() {
        when(serviceTypeService.findAll()).thenReturn(List.of());

        assertThat(tools.listServiceTypes())
                .isEqualTo("No service types are currently available.");
    }

    @Test
    void returnsFallbackMessageWhenServiceLookupFails() {
        when(serviceTypeService.findAll()).thenThrow(new IllegalStateException("db down"));

        assertThat(tools.listServiceTypes())
                .isEqualTo("The service list is temporarily unavailable. Please try again later.");
    }

    private static ServiceTypeResponse serviceType(UUID id, String name, String description, String price) {
        return new ServiceTypeResponse(
                id,
                name,
                description,
                "https://example.com/img.png",
                "general",
                30,
                60,
                new BigDecimal(price),
                List.of("item"),
                "note",
                LocalDateTime.of(2026, 8, 1, 10, 0));
    }
}
