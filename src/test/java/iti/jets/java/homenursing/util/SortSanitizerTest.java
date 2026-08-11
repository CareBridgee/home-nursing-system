package iti.jets.java.homenursing.util;

import iti.jets.java.homenursing.exception.BadRequestException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class SortSanitizerTest {

    private static final Set<String> ALLOWED = Set.of("id", "rating", "createdAt");

    @Test
    void unsortedPageableIsReturnedAsIs() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThat(SortSanitizer.sanitize(pageable, ALLOWED)).isSameAs(pageable);
    }

    @Test
    void sortedPageableWithOnlyAllowedPropertiesIsReturned() {
        Pageable pageable = PageRequest.of(1, 20, Sort.by("rating").descending());
        assertThat(SortSanitizer.sanitize(pageable, ALLOWED)).isSameAs(pageable);
    }

    @Test
    void invalidSingleSortPropertyThrows() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("hackedColumn"));
        assertThatThrownBy(() -> SortSanitizer.sanitize(pageable, ALLOWED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid sort property: hackedColumn");
    }

    @Test
    void invalidPropertyAmongMultipleOrdersThrows() {
        Pageable pageable = PageRequest.of(0, 10,
                Sort.by(Sort.Order.desc("rating"), Sort.Order.asc("rating").withProperty("evil")));
        assertThatThrownBy(() -> SortSanitizer.sanitize(pageable, ALLOWED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid sort property: evil");
    }

    @Test
    void asSetSplitsCommaSeparatedProperties() {
        assertThat(SortSanitizer.asSet("id,rating,createdAt"))
                .containsExactlyInAnyOrder("id", "rating", "createdAt");
    }

    @Test
    void asSetOfSinglePropertyYieldsOneElement() {
        assertThat(SortSanitizer.asSet("id")).containsExactly("id");
    }

    @Test
    void reviewRatingSortableConstantIsUsableAsAllowedSet() {
        assertThat(SortSanitizer.asSet(SortSanitizer.REVIEW_RATING_SORTABLE))
                .contains("id", "rating", "reviewText", "isAnonymous", "createdAt", "updatedAt");
    }

    @Test
    void userSortableConstantIsUsableAsAllowedSet() {
        assertThat(SortSanitizer.asSet(SortSanitizer.USER_SORTABLE))
                .contains("id", "phoneNumber", "email", "firstName", "lastName",
                        "dateOfBirth", "gender", "createdAt", "updatedAt");
    }
}
