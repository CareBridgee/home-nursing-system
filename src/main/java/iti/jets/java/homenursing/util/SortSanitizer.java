package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class SortSanitizer {

    public static final String REVIEW_RATING_SORTABLE = "id,rating,reviewText,isAnonymous,createdAt,updatedAt";

    public static final String USER_SORTABLE =
            "id,phoneNumber,email,firstName,lastName,dateOfBirth,gender,createdAt,updatedAt";

    private SortSanitizer() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedProperties) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new BadRequestException("Invalid sort property: " + order.getProperty());
            }
        }
        return pageable;
    }

    public static Set<String> asSet(String properties) {
        return Set.of(properties.split(","));
    }
}
