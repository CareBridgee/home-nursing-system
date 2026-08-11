package iti.jets.java.homenursing.annotation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AnnotationContractTest {

    @Test
    void sortableFieldsTargetsMethodsAndIsRetainedAtRuntime() throws Exception {
        assertThat(SortableFields.class.getAnnotation(java.lang.annotation.Target.class).value())
                .containsExactly(ElementType.METHOD);
        assertThat(SortableFields.class.getAnnotation(java.lang.annotation.Retention.class).value())
                .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void sortableFieldsValueIsReadableFromAnnotatedMethod() throws Exception {
        SortableFields annotation = Controller.class.getMethod("list").getAnnotation(SortableFields.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("id,rating,createdAt");
    }

    @Test
    void allowedValuesTargetsFieldsAndParametersAndIsRetainedAtRuntime() {
        assertThat(AllowedValues.class.getAnnotation(java.lang.annotation.Target.class).value())
                .containsExactlyInAnyOrder(ElementType.FIELD, ElementType.PARAMETER);
        assertThat(AllowedValues.class.getAnnotation(java.lang.annotation.Retention.class).value())
                .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void allowedValuesIsReadableFromAnnotatedField() throws Exception {
        AllowedValues annotation = Dto.class.getDeclaredField("status").getAnnotation(AllowedValues.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly("PENDING", "COMPLETED", "CANCELLED");
    }

    @Test
    void allowedValuesIsReadableFromAnnotatedParameter() throws Exception {
        AllowedValues annotation = Controller.class.getMethod("status", String.class)
                .getParameters()[0].getAnnotation(AllowedValues.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly("OPEN", "CLOSED");
    }

    static class Controller {
        @SortableFields("id,rating,createdAt")
        public void list() {
        }

        public void status(@AllowedValues({"OPEN", "CLOSED"}) String value) {
        }
    }

    static class Dto {
        @AllowedValues({"PENDING", "COMPLETED", "CANCELLED"})
        public String status;
    }
}
