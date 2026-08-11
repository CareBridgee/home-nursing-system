package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.catalog.ServiceTypeRequest;
import iti.jets.java.homenursing.dto.catalog.ServiceTypeResponse;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.exception.BadRequestException;
import iti.jets.java.homenursing.exception.ResourceNotFoundException;
import iti.jets.java.homenursing.mapper.ServiceTypeMapper;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.impl.ServiceTypeServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ServiceTypeServiceImplTest {

    @Mock
    private ServiceTypeRepository serviceTypeRepository;
    @Mock
    private ServiceTypeMapper serviceTypeMapper;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ServiceTypeServiceImpl serviceTypeService;

    private static final UUID SERVICE_TYPE_ID = UUID.randomUUID();

    private static ServiceType serviceType() {
        return ServiceType.builder()
                .id(SERVICE_TYPE_ID)
                .name("Nursing Care")
                .description("Home nursing")
                .category("CARE")
                .minimumDurationMinutes(120)
                .estimatedDurationMinutes(180)
                .basePrice(new BigDecimal("500.00"))
                .includedItems(List.of("Vitals check"))
                .preparationNote("Prepare bed")
                .imageUrl("old-image-url")
                .build();
    }

    private static ServiceTypeResponse response() {
        return new ServiceTypeResponse(
                SERVICE_TYPE_ID, "Nursing Care", "Home nursing", "old-image-url", "CARE",
                120, 180, new BigDecimal("500.00"), List.of("Vitals check"), "Prepare bed", null);
    }

    private static MockMultipartFile image(String contentType) {
        return new MockMultipartFile("image", "photo.png", contentType, new byte[]{1, 2, 3});
    }

    @Test
    void findAllMapsEveryType() {
        ServiceType first = serviceType();
        ServiceType second = ServiceType.builder().id(UUID.randomUUID()).name("Physio").build();
        when(serviceTypeRepository.findAll()).thenReturn(List.of(first, second));
        when(serviceTypeMapper.toResponse(first)).thenReturn(response());
        when(serviceTypeMapper.toResponse(second)).thenReturn(
                new ServiceTypeResponse(second.getId(), "Physio", null, null, null, null, null, null, null, null, null));

        List<ServiceTypeResponse> responses = serviceTypeService.findAll();

        assertThat(responses).extracting(ServiceTypeResponse::name).containsExactly("Nursing Care", "Physio");
    }

    @Test
    void getByIdReturnsMappedType() {
        ServiceType entity = serviceType();
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(entity));
        when(serviceTypeMapper.toResponse(entity)).thenReturn(response());

        ServiceTypeResponse result = serviceTypeService.getById(SERVICE_TYPE_ID);

        assertThat(result.id()).isEqualTo(SERVICE_TYPE_ID);
    }

    @Test
    void getByIdWhenMissingThrows() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceTypeService.getById(SERVICE_TYPE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service type not found");
    }

    @Test
    void createWithoutImageSavesMappedEntity() {
        ServiceTypeRequest request = new ServiceTypeRequest(
                "Nursing Care", "Home nursing", "CARE", 120, 180,
                new BigDecimal("500.00"), List.of("Vitals check"), "Prepare bed", null);
        ServiceType mapped = ServiceType.builder().name("Nursing Care").build();
        ServiceType saved = serviceType();
        when(serviceTypeMapper.toEntity(request)).thenReturn(mapped);
        when(serviceTypeRepository.save(mapped)).thenReturn(saved);
        when(serviceTypeMapper.toResponse(saved)).thenReturn(response());

        ServiceTypeResponse result = serviceTypeService.create(request);

        assertThat(result.name()).isEqualTo("Nursing Care");
        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
    }

    @Test
    void createWithImageUploadsAndSetsImageUrl() {
        MockMultipartFile image = image("image/png");
        ServiceTypeRequest request = new ServiceTypeRequest(
                "Nursing Care", "Home nursing", "CARE", 120, 180,
                new BigDecimal("500.00"), List.of("Vitals check"), "Prepare bed", image);
        ServiceType mapped = ServiceType.builder().name("Nursing Care").build();
        ServiceType saved = serviceType();
        when(serviceTypeMapper.toEntity(request)).thenReturn(mapped);
        when(cloudinaryService.upload(image)).thenReturn("cloud-image-url");
        when(serviceTypeRepository.save(mapped)).thenReturn(saved);
        when(serviceTypeMapper.toResponse(saved)).thenReturn(response());

        serviceTypeService.create(request);

        assertThat(mapped.getImageUrl()).isEqualTo("cloud-image-url");
        verify(cloudinaryService).upload(image);
    }

    @Test
    void createWithNonImageContentTypeThrows() {
        MockMultipartFile image = image("text/plain");
        ServiceTypeRequest request = new ServiceTypeRequest(
                "Nursing Care", null, null, null, null, null, null, null, image);
        when(serviceTypeMapper.toEntity(request)).thenReturn(ServiceType.builder().build());

        assertThatThrownBy(() -> serviceTypeService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be an image file");

        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
    }

    @Test
    void createWithNullContentTypeThrows() {
        MockMultipartFile image = image(null);
        ServiceTypeRequest request = new ServiceTypeRequest(
                "Nursing Care", null, null, null, null, null, null, null, image);
        when(serviceTypeMapper.toEntity(request)).thenReturn(ServiceType.builder().build());

        assertThatThrownBy(() -> serviceTypeService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be an image file");
    }

    @Test
    void createWithEmptyImageSkipsUpload() {
        MockMultipartFile empty = new MockMultipartFile("image", new byte[0]);
        ServiceTypeRequest request = new ServiceTypeRequest(
                "Nursing Care", null, null, null, null, null, null, null, empty);
        when(serviceTypeMapper.toEntity(request)).thenReturn(ServiceType.builder().build());
        when(serviceTypeRepository.save(any(ServiceType.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceTypeMapper.toResponse(any(ServiceType.class))).thenReturn(response());

        serviceTypeService.create(request);

        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
    }

    @Test
    void updateAppliesAllFieldsWithoutImage() {
        ServiceType entity = serviceType();
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(entity));
        ServiceTypeRequest request = new ServiceTypeRequest(
                "New Name", "New desc", "NEW_CAT", 60, 90,
                new BigDecimal("250.00"), List.of("Item A"), "Note", null);
        when(serviceTypeRepository.save(entity)).thenReturn(entity);
        when(serviceTypeMapper.toResponse(entity)).thenReturn(response());

        serviceTypeService.update(SERVICE_TYPE_ID, request);

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getDescription()).isEqualTo("New desc");
        assertThat(entity.getCategory()).isEqualTo("NEW_CAT");
        assertThat(entity.getMinimumDurationMinutes()).isEqualTo(60);
        assertThat(entity.getEstimatedDurationMinutes()).isEqualTo(90);
        assertThat(entity.getBasePrice()).isEqualByComparingTo("250.00");
        assertThat(entity.getIncludedItems()).containsExactly("Item A");
        assertThat(entity.getPreparationNote()).isEqualTo("Note");
        assertThat(entity.getImageUrl()).isEqualTo("old-image-url");
        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
        verify(cloudinaryService, never()).delete(any());
    }

    @Test
    void updateWithImageUploadsAndDeletesOldUrl() {
        ServiceType entity = serviceType();
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(entity));
        MockMultipartFile image = image("image/jpeg");
        ServiceTypeRequest request = new ServiceTypeRequest(
                "New Name", null, null, null, null, null, null, null, image);
        when(cloudinaryService.upload(image)).thenReturn("new-image-url");
        when(serviceTypeRepository.save(entity)).thenReturn(entity);
        when(serviceTypeMapper.toResponse(entity)).thenReturn(response());

        serviceTypeService.update(SERVICE_TYPE_ID, request);

        assertThat(entity.getImageUrl()).isEqualTo("new-image-url");
        verify(cloudinaryService).delete("old-image-url");
    }

    @Test
    void updateWithInvalidImageThrowsBeforeUpload() {
        ServiceType entity = serviceType();
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(entity));
        ServiceTypeRequest request = new ServiceTypeRequest(
                "New Name", null, null, null, null, null, null, null, image("application/pdf"));

        assertThatThrownBy(() -> serviceTypeService.update(SERVICE_TYPE_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be an image file");

        verify(cloudinaryService, never()).upload(any(MultipartFile.class));
        verify(cloudinaryService, never()).delete(any());
    }

    @Test
    void updateWhenMissingThrows() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceTypeService.update(
                SERVICE_TYPE_ID, new ServiceTypeRequest("X", null, null, null, null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service type not found");

        verify(serviceTypeRepository, never()).save(any(ServiceType.class));
    }

    @Test
    void deleteRemovesEntityAndImage() {
        ServiceType entity = serviceType();
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.of(entity));

        serviceTypeService.delete(SERVICE_TYPE_ID);

        verify(serviceTypeRepository).delete(entity);
        verify(serviceTypeRepository).flush();
        verify(cloudinaryService).delete("old-image-url");
    }

    @Test
    void deleteWhenMissingThrows() {
        when(serviceTypeRepository.findById(SERVICE_TYPE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceTypeService.delete(SERVICE_TYPE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Service type not found");

        verify(cloudinaryService, never()).delete(any());
    }
}
