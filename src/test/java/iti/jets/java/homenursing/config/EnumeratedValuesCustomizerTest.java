package iti.jets.java.homenursing.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import iti.jets.java.homenursing.annotation.AllowedValues;
import iti.jets.java.homenursing.annotation.SortableFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EnumeratedValuesCustomizerTest {

    @Mock
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    private EnumeratedValuesCustomizer customizer;
    private final TestController controller = new TestController();

    @BeforeEach
    void setUp() {
        customizer = new EnumeratedValuesCustomizer(requestMappingHandlerMapping);
    }

    private static RequestMappingInfo mapping(String pattern, RequestMethod method) {
        return new RequestMappingInfo(
                new PatternsRequestCondition(pattern),
                new RequestMethodsRequestCondition(method),
                null, null, null, null, null, null);
    }

    private static HandlerMethod handler(TestController bean, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        return new HandlerMethod(bean, TestController.class.getMethod(methodName, parameterTypes));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Schema schemaWithRef(String ref) {
        return new Schema().$ref(ref);
    }

    private static Parameter sortParam(String description, Schema<?> schema) {
        return new Parameter().name("sort").description(description).schema(schema);
    }

    private static Parameter queryParam(String name, Schema<?> schema) {
        return new Parameter().name(name).schema(schema);
    }

    private static Operation jsonBodyOperation(Parameter parameter, Schema<?> bodySchema) {
        Operation operation = new Operation();
        if (parameter != null) {
            operation.addParametersItem(parameter);
        }
        if (bodySchema != null) {
            operation.requestBody(new io.swagger.v3.oas.models.parameters.RequestBody().content(
                    new Content().addMediaType("application/json",
                            new MediaType().schema(bodySchema))));
        }
        return operation;
    }

    /**
     * Enum values of a component schema property. Local typed variables are used
     * because chained calls on the raw {@link Schema} type are erased by javac.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<String> enumOfProperty(Components components, String schemaName, String propertyName) {
        Map<String, Schema> schemas = components.getSchemas();
        Schema dto = schemas.get(schemaName);
        Map<String, Schema> properties = dto.getProperties();
        Schema property = properties.get(propertyName);
        return property == null ? null : (List<String>) property.getEnum();
    }

    @SuppressWarnings({"rawtypes"})
    private static String descriptionOfProperty(Components components, String schemaName, String propertyName) {
        Map<String, Schema> schemas = components.getSchemas();
        Schema dto = schemas.get(schemaName);
        Map<String, Schema> properties = dto.getProperties();
        Schema property = properties.get(propertyName);
        return property == null ? null : property.getDescription();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<String> enumOfItems(Schema<?> arraySchema) {
        Schema items = arraySchema.getItems();
        return items == null ? null : (List<String>) items.getEnum();
    }

    @Test
    void nullPathsInOpenApiIsANoOp() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));

        OpenAPI openApi = new OpenAPI();
        customizer.customise(openApi);

        assertThat(openApi.getPaths()).isNull();
    }

    @Test
    void emptyPathsWithRegisteredEndpointsIsANoOp() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));

        customizer.customise(new OpenAPI().paths(new Paths()));

        assertThat(new OpenAPI().paths(new Paths()).getPaths()).isEmpty();
    }

    @Test
    void emptyRegistryIsANoOp() {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of());

        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/x", new PathItem().get(new Operation())));
        customizer.customise(openApi);

        assertThat(openApi.getPaths().get("/api/x").getGet().getParameters()).isNull();
    }

    @Test
    void sortParameterOnArraySchemaWithoutItemsGetsEnumAndDescription() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));
        ArraySchema sortSchema = new ArraySchema();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(sortParam("Sort order", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(sortSchema.getItems()).isNotNull();
        assertThat(sortSchema.getItems().getType()).isEqualTo("string");
        assertThat(enumOfItems(sortSchema)).containsExactly("id", "rating");
        Parameter patched = openApi.getPaths().get("/api/reviews").getGet().getParameters().get(0);
        assertThat(patched.getDescription())
                .isEqualTo("Sort order. Allowed properties: id, rating.");
    }

    @Test
    void sortParameterOnArraySchemaWithItemsReplacesItemsEnum() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));
        ArraySchema sortSchema = new ArraySchema().items(new Schema<String>().type("string").example("rating"));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(sortParam("Sort order", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(enumOfItems(sortSchema)).containsExactly("id", "rating");
    }

    @Test
    void sortParameterOnPlainSchemaGetsEnum() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));
        Schema<String> sortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(sortParam("Sort order", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(sortSchema.getEnum()).containsExactly("id", "rating");
    }

    @Test
    void sortParameterWithNullSchemaStillGetsDescription() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(sortParam("Sort order", null), null))));

        customizer.customise(openApi);

        Parameter patched = openApi.getPaths().get("/api/reviews").getGet().getParameters().get(0);
        assertThat(patched.getDescription())
                .isEqualTo("Sort order. Allowed properties: id, rating.");
        assertThat(patched.getSchema()).isNull();
    }

    @Test
    void descriptionAlreadyEndingWithPeriodIsNotDoublePunctuated() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(sortParam("Sort order.", null), null))));

        customizer.customise(openApi);

        assertThat(openApi.getPaths().get("/api/reviews").getGet().getParameters().get(0).getDescription())
                .isEqualTo("Sort order. Allowed properties: id, rating.");
    }

    @Test
    void sortParameterIsNotPatchedWithoutSortableFieldsAnnotation() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/plain", RequestMethod.GET), handler(controller, "plainSort", String.class)));
        Schema<String> sortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/plain",
                new PathItem().get(jsonBodyOperation(sortParam("Sort order", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(sortSchema.getEnum()).isNull();
        assertThat(openApi.getPaths().get("/api/plain").getGet().getParameters().get(0).getDescription())
                .isEqualTo("Sort order");
    }

    @Test
    void allowedValuesQueryParameterGetsSchemaEnum() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/status", RequestMethod.GET), handler(controller, "status", String.class)));
        Schema<String> schema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/status",
                new PathItem().get(jsonBodyOperation(queryParam("status", schema), null))));

        customizer.customise(openApi);

        assertThat(schema.getEnum()).containsExactly("OPEN", "CLOSED");
    }

    @Test
    void allowedValuesWithBlankRequestParamNameUsesParameterName() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/filter", RequestMethod.GET), handler(controller, "unnamed", String.class)));
        Schema<String> schema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/filter",
                new PathItem().get(jsonBodyOperation(queryParam("filter", schema), null))));

        customizer.customise(openApi);

        assertThat(schema.getEnum()).containsExactly("A", "B");
    }

    @Test
    void allowedValuesParameterWithNullSchemaIsSkipped() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/status", RequestMethod.GET), handler(controller, "status", String.class)));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/status",
                new PathItem().get(jsonBodyOperation(queryParam("status", null), null))));

        customizer.customise(openApi);

        assertThat(openApi.getPaths().get("/api/status").getGet().getParameters().get(0).getSchema())
                .isNull();
    }

    @Test
    void operationWithoutParametersOrRequestBodyIsHandled() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/status", RequestMethod.GET), handler(controller, "status", String.class)));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/status",
                new PathItem().get(new Operation())));

        customizer.customise(openApi);

        assertThat(openApi.getPaths().get("/api/status").getGet().getParameters()).isNull();
        assertThat(openApi.getPaths().get("/api/status").getGet().getRequestBody()).isNull();
    }

    @Test
    void requestBodyDtoRefGetsAllowedValuesOnStringProperties() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.POST), handler(controller, "listReviewsPost", TestDto.class)));
        Components components = new Components()
                .addSchemas("TestDto", new Schema<>()
                        .addProperty("status", new Schema<>().type("string").description("Status filter."))
                        .addProperty("number", new Schema<>().type("integer"))
                        .addProperty("plain", new Schema<>().type("string"))
                        .addProperty("preset", new Schema<>().type("string").description("Allowed values: anything"))
                        .addProperty("emptyDesc", new Schema<>().type("string")));
        OpenAPI openApi = new OpenAPI().components(components).paths(new Paths().addPathItem("/api/reviews",
                new PathItem().post(jsonBodyOperation(null, schemaWithRef("#/components/schemas/TestDto")))));

        customizer.customise(openApi);

        Schema<?> dto = openApi.getComponents().getSchemas().get("TestDto");
        assertThat(dto.getProperties().get("status").getEnum())
                .containsExactly("PENDING", "COMPLETED", "CANCELLED");
        assertThat(dto.getProperties().get("status").getDescription())
                .isEqualTo("Status filter. Allowed values: PENDING, COMPLETED, CANCELLED (suggested, not enforced).");
        assertThat(dto.getProperties().get("number").getEnum()).isNull();
        assertThat(dto.getProperties().get("plain").getEnum()).isNull();
        assertThat(dto.getProperties().get("preset").getEnum()).containsExactly("y", "z");
        assertThat(dto.getProperties().get("preset").getDescription()).isEqualTo("Allowed values: anything");
        assertThat(dto.getProperties().get("emptyDesc").getEnum()).containsExactly("low", "high");
        assertThat(dto.getProperties().get("emptyDesc").getDescription())
                .isEqualTo("Allowed values: low, high (suggested, not enforced).");
    }

    @Test
    void refWithoutMatchingDtoValuesIsSkipped() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.POST), handler(controller, "listReviewsPost", TestDto.class)));
        Components components = new Components()
                .addSchemas("SomeOtherDto", new Schema<>()
                        .addProperty("status", new Schema<>().type("string")));
        OpenAPI openApi = new OpenAPI().components(components).paths(new Paths().addPathItem("/api/reviews",
                new PathItem().post(jsonBodyOperation(null, schemaWithRef("#/components/schemas/SomeOtherDto")))));

        customizer.customise(openApi);

        assertThat(enumOfProperty(openApi.getComponents(), "SomeOtherDto", "status")).isNull();
    }

    @Test
    void dtoValuesPointingToMissingComponentSchemaAreIgnored() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.POST), handler(controller, "listReviewsPost", TestDto.class)));
        Components components = new Components().addSchemas("Unrelated", new Schema<>());
        OpenAPI openApi = new OpenAPI().components(components).paths(new Paths().addPathItem("/api/reviews",
                new PathItem().post(jsonBodyOperation(null, schemaWithRef("#/components/schemas/TestDto")))));

        customizer.customise(openApi);

        assertThat(openApi.getComponents().getSchemas()).containsKeys("Unrelated");
    }

    @Test
    void refWithNullComponentsIsSkipped() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.POST), handler(controller, "listReviewsPost", TestDto.class)));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().post(jsonBodyOperation(null, schemaWithRef("#/components/schemas/TestDto")))));

        customizer.customise(openApi);

        assertThat(openApi.getComponents()).isNull();
    }

    @Test
    void allOfOneOfAnyOfAndNestedPropertiesAreTraversed() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/nested", RequestMethod.POST), handler(controller, "nested", NestedDto.class)));
        Components components = new Components()
                .addSchemas("NestedDto", new Schema<>()
                        .addProperty("level", new Schema<>().type("string")));
        Schema<?> shared = new Schema<>().type("string");
        Schema<?> root = new Schema<>()
                .addAllOfItem(schemaWithRef("#/components/schemas/NestedDto"))
                .addOneOfItem(schemaWithRef("#/components/schemas/NestedDto"))
                .addAnyOfItem(schemaWithRef("#/components/schemas/NestedDto"))
                .addProperty("a", shared)
                .addProperty("b", shared);
        OpenAPI openApi = new OpenAPI().components(components).paths(new Paths().addPathItem("/api/nested",
                new PathItem().post(jsonBodyOperation(null, root))));

        customizer.customise(openApi);

        assertThat(enumOfProperty(openApi.getComponents(), "NestedDto", "level"))
                .containsExactly("NEW", "OLD");
    }

    @Test
    void multipleHttpMethodsAndPatternsAreRegisteredPerKey() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class),
                mapping("/api/reviews", RequestMethod.POST), handler(controller, "listReviewsPost", TestDto.class)));
        Schema<String> getSortSchema = new Schema<>();
        Schema<String> postSortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/reviews", new PathItem()
                        .get(jsonBodyOperation(sortParam("Sort", getSortSchema), null))
                        .post(jsonBodyOperation(sortParam("Sort", postSortSchema), null))));

        customizer.customise(openApi);

        assertThat(getSortSchema.getEnum()).containsExactly("id", "rating");
        assertThat(postSortSchema.getEnum()).containsExactly("id", "rating");
    }

    @Test
    void openApiPathNotPresentInRegistryIsSkipped() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));
        Schema<String> foreignSortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/foreign",
                new PathItem().get(jsonBodyOperation(sortParam("Sort", foreignSortSchema), null))));

        customizer.customise(openApi);

        assertThat(foreignSortSchema.getEnum()).isNull();
    }

    @Test
    void mappingWithoutMethodsOrPatternsIsSkipped() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                new RequestMappingInfo(
                        new PatternsRequestCondition("/api/nomethods"),
                        new RequestMethodsRequestCondition(),
                        null, null, null, null, null, null),
                handler(controller, "noParams"),
                new RequestMappingInfo(
                        new PatternsRequestCondition(),
                        new RequestMethodsRequestCondition(RequestMethod.GET),
                        null, null, null, null, null, null),
                handler(controller, "noParams")));
        Schema<String> sortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/nomethods",
                new PathItem().get(jsonBodyOperation(sortParam("Sort", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(sortSchema.getEnum()).isNull();
    }

    @Test
    void builderBasedInfoWithPathPatternsConditionIsHandled() throws Exception {
        RequestMappingInfo info = RequestMappingInfo.paths("/api/reviews").methods(RequestMethod.GET).build();
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                info, handler(controller, "listReviews", String.class, TestDto.class)));
        Schema<String> sortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(sortParam("Sort", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(sortSchema.getEnum()).containsExactly("id", "rating");
    }

    @Test
    void sortParameterWithNullDescriptionIsHandled() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));
        Schema<String> sortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(new Parameter().name("sort").schema(sortSchema), null))));

        customizer.customise(openApi);

        assertThat(openApi.getPaths().get("/api/reviews").getGet().getParameters().get(0).getDescription())
                .isEqualTo(". Allowed properties: id, rating.");
        assertThat(sortSchema.getEnum()).containsExactly("id", "rating");
    }

    @Test
    void requestBodyWithNullSchemaIsIgnored() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.POST), handler(controller, "listReviewsPost", TestDto.class)));
        Operation operation = new Operation().requestBody(
                new io.swagger.v3.oas.models.parameters.RequestBody().content(
                        new Content().addMediaType("application/json", new MediaType())));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().post(operation)));

        customizer.customise(openApi);

        assertThat(openApi.getComponents()).isNull();
    }

    @Test
    void componentSchemaWithoutPropertiesIsLeftUntouched() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.POST), handler(controller, "listReviewsPost", TestDto.class)));
        Components components = new Components().addSchemas("TestDto", new Schema<>());
        OpenAPI openApi = new OpenAPI().components(components).paths(new Paths().addPathItem("/api/reviews",
                new PathItem().post(jsonBodyOperation(null, schemaWithRef("#/components/schemas/TestDto")))));

        customizer.customise(openApi);

        assertThat(openApi.getComponents().getSchemas().get("TestDto").getProperties()).isNull();
    }

    @Test
    void propertyWithoutExplicitTypeStillGetsEnum() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.POST), handler(controller, "listReviewsPost", TestDto.class)));
        Components components = new Components()
                .addSchemas("TestDto", new Schema<>().addProperty("status", new Schema<>()));
        OpenAPI openApi = new OpenAPI().components(components).paths(new Paths().addPathItem("/api/reviews",
                new PathItem().post(jsonBodyOperation(null, schemaWithRef("#/components/schemas/TestDto")))));

        customizer.customise(openApi);

        assertThat(enumOfProperty(openApi.getComponents(), "TestDto", "status"))
                .containsExactly("PENDING", "COMPLETED", "CANCELLED");
        assertThat(descriptionOfProperty(openApi.getComponents(), "TestDto", "status"))
                .isEqualTo("Allowed values: PENDING, COMPLETED, CANCELLED (suggested, not enforced).");
    }

    @Test
    void customiseCalledTwiceReusesTheRegistry() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/reviews", RequestMethod.GET), handler(controller, "listReviews", String.class, TestDto.class)));
        Schema<String> first = new Schema<>();
        Schema<String> second = new Schema<>();
        OpenAPI firstOpenApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(sortParam("Sort", first), null))));
        OpenAPI secondOpenApi = new OpenAPI().paths(new Paths().addPathItem("/api/reviews",
                new PathItem().get(jsonBodyOperation(sortParam("Sort", second), null))));

        customizer.customise(firstOpenApi);
        customizer.customise(secondOpenApi);

        assertThat(first.getEnum()).containsExactly("id", "rating");
        assertThat(second.getEnum()).containsExactly("id", "rating");
    }

    @Test
    void mappingWithOnlyMethodsAndNoPatternsIsSkipped() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                RequestMappingInfo.paths().methods(RequestMethod.GET).build(),
                handler(controller, "noParams")));
        Schema<String> sortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/nomethods",
                new PathItem().get(jsonBodyOperation(sortParam("Sort", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(sortSchema.getEnum()).isNull();
    }

    @Test
    void paramWithoutExplicitNameIsPatchedUsingReflectionName() throws Exception {
        MethodParameter parameter = new MethodParameter(TestController.class.getMethod("unnamed", String.class), 0);
        HandlerMethod mocked = mock(HandlerMethod.class);
        when(mocked.getMethod()).thenReturn(TestController.class.getMethod("unnamed", String.class));
        when(mocked.getMethodParameters()).thenReturn(new MethodParameter[]{parameter});
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/filter", RequestMethod.GET), mocked));
        Schema<String> schema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/filter",
                new PathItem().get(jsonBodyOperation(queryParam("filter", schema), null))));

        customizer.customise(openApi);

        assertThat(schema.getEnum()).containsExactly("A", "B");
    }

    @Test
    void paramWithUnresolvableNameIsSkipped() throws Exception {
        MethodParameter parameter = new MethodParameter(
                TestController.class.getMethod("unnamed", String.class), 0) {
            @Override
            public String getParameterName() {
                return null;
            }
        };
        HandlerMethod mocked = mock(HandlerMethod.class);
        when(mocked.getMethod()).thenReturn(TestController.class.getMethod("unnamed", String.class));
        when(mocked.getMethodParameters()).thenReturn(new MethodParameter[]{parameter});
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/filter", RequestMethod.GET), mocked));
        Schema<String> schema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/filter",
                new PathItem().get(jsonBodyOperation(queryParam("filter", schema), null))));

        customizer.customise(openApi);

        assertThat(schema.getEnum()).isNull();
    }

    @Test
    void dtoWithoutAllowedValuesFields_contributesNoDtoValues() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/plain", RequestMethod.POST), handler(controller, "plainBody", PlainDto.class)));
        Components components = new Components()
                .addSchemas("PlainDto", new Schema<>().addProperty("name", new Schema<>().type("string")));
        OpenAPI openApi = new OpenAPI().components(components).paths(new Paths().addPathItem("/api/plain",
                new PathItem().post(jsonBodyOperation(null, schemaWithRef("#/components/schemas/PlainDto")))));

        customizer.customise(openApi);

        assertThat(enumOfProperty(openApi.getComponents(), "PlainDto", "name")).isNull();
    }

    @Test
    void interfaceTypedParameter_scansToNullSuperclass() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                mapping("/api/plain", RequestMethod.POST), handler(controller, "ifaceBody", ValueFilter.class)));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/plain",
                new PathItem().post(jsonBodyOperation(null, null))));

        customizer.customise(openApi);

        assertThat(openApi.getComponents()).isNull();
    }

    @Test
    void legacyConstructorMappingWithoutPatternsAndMethodsIsSkipped() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                new RequestMappingInfo(
                        new PatternsRequestCondition(),
                        new RequestMethodsRequestCondition(),
                        null, null, null, null, null, null),
                handler(controller, "noParams")));
        Schema<String> sortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/nomethods",
                new PathItem().get(jsonBodyOperation(sortParam("Sort", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(sortSchema.getEnum()).isNull();
    }

    @Test
    void mappingWithNullPatternsConditionAndNoPatternsIsSkipped() throws Exception {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Map.of(
                new RequestMappingInfo(
                        null,
                        new RequestMethodsRequestCondition(RequestMethod.GET),
                        null, null, null, null, null, null),
                handler(controller, "noParams"),
                new RequestMappingInfo(
                        null,
                        new RequestMethodsRequestCondition(),
                        null, null, null, null, null, null),
                handler(controller, "noParams")));
        Schema<String> sortSchema = new Schema<>();
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem("/api/nomethods",
                new PathItem().get(jsonBodyOperation(sortParam("Sort", sortSchema), null))));

        customizer.customise(openApi);

        assertThat(sortSchema.getEnum()).isNull();
    }

    static class TestController {

        @SortableFields("id,rating")
        public void listReviews(@RequestParam("sort") String sort, TestDto body) {
        }

        @SortableFields("id,rating")
        public void listReviewsPost(TestDto body) {
        }

        public void plainSort(@RequestParam("sort") String sort) {
        }

        public void status(@RequestParam("status") @AllowedValues({"OPEN", "CLOSED"}) String status) {
        }

        public void unnamed(@RequestParam @AllowedValues({"A", "B"}) String filter) {
        }

        public void nested(NestedDto dto) {
        }

        public void plainBody(PlainDto dto) {
        }

        public void ifaceBody(ValueFilter filter) {
        }

        public void noParams() {
        }
    }

    static class TestDto {
        @AllowedValues({"PENDING", "COMPLETED", "CANCELLED"})
        public String status;

        @AllowedValues({"a", "b"})
        public Integer number;

        @AllowedValues({"x"})
        public String missing;

        @AllowedValues({"y", "z"})
        public String preset;

        @AllowedValues({"low", "high"})
        public String emptyDesc;

        public String plain;
    }

    static class NestedDto {
        @AllowedValues({"NEW", "OLD"})
        public String level;
    }

    interface ValueFilter {
    }

    static class PlainDto {
        public String name;
    }
}
