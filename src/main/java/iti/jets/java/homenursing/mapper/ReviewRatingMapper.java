package iti.jets.java.homenursing.mapper;

import iti.jets.java.homenursing.dto.ReviewRatingRequest;
import iti.jets.java.homenursing.dto.ReviewRatingResponse;
import iti.jets.java.homenursing.entity.ReviewRating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ReviewRatingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "profile", ignore = true)
    @Mapping(target = "nurse", ignore = true)
    ReviewRating toEntity(ReviewRatingRequest request);

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(target = "nurseId", source = "nurse.id")
    ReviewRatingResponse toResponse(ReviewRating reviewRating);
}
