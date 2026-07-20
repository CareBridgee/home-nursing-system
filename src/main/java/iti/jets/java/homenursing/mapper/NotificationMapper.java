package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.notification.NotificationRequest;
import iti.jets.java.homenursing.dto.notification.NotificationResponse;
import iti.jets.java.homenursing.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Notification toEntity(NotificationRequest request);

    @Mapping(target = "userId", source = "user.id")
    NotificationResponse toResponse(Notification notification);
}
