package iti.jets.java.homenursing.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import iti.jets.java.homenursing.annotation.AllowedValues;
import iti.jets.java.homenursing.annotation.SortableFields;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EnumeratedValuesCustomizer implements OpenApiCustomizer {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final Map<String, EndpointDocs> registry = new ConcurrentHashMap<>();

    public EnumeratedValuesCustomizer(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    @Override
    public void customise(OpenAPI openApi) {
        buildRegistry();
        if (openApi.getPaths() == null || registry.isEmpty()) {
            return;
        }
        Map<String, Map<String, String[]>> dtoValues = mergeDtoValues();
        for (Map.Entry<String, PathItem> pathEntry : openApi.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet()) {
                EndpointDocs docs = registry.get(key(opEntry.getKey(), path));
                if (docs != null) {
                    patchOperation(opEntry.getValue(), docs, openApi.getComponents(), dtoValues);
                }
            }
        }
    }

    private Map<String, Map<String, String[]>> mergeDtoValues() {
        Map<String, Map<String, String[]>> merged = new HashMap<>();
        for (EndpointDocs docs : registry.values()) {
            for (Map.Entry<String, Map<String, String[]>> e : docs.dtoValues.entrySet()) {
                merged.computeIfAbsent(e.getKey(), k -> new HashMap<>()).putAll(e.getValue());
            }
        }
        return merged;
    }

    private void patchOperation(Operation operation, EndpointDocs docs, Components components,
                                Map<String, Map<String, String[]>> dtoValues) {
        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                if ("sort".equals(parameter.getName()) && docs.sortable != null) {
                    patchSortParameter(parameter, docs.sortable);
                }
                String[] paramValues = docs.paramValues.get(parameter.getName());
                if (paramValues != null && parameter.getSchema() != null) {
                    parameter.getSchema().setEnum(asList(paramValues));
                }
            }
        }
        Content content = operation.getRequestBody() != null ? operation.getRequestBody().getContent() : null;
        if (content != null) {
            Set<Schema<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (MediaType mediaType : content.values()) {
                patchSchema(mediaType.getSchema(), components, dtoValues, visited);
            }
        }
    }

    private void patchSortParameter(Parameter parameter, String[] sortable) {
        Schema<?> schema = parameter.getSchema();
        List<String> values = asList(sortable);
        if (schema instanceof ArraySchema arraySchema) {
            if (arraySchema.getItems() == null) {
                arraySchema.setItems(new Schema<String>().type("string"));
            }
            setEnumValues(arraySchema.getItems(), values);
        } else if (schema != null) {
            setEnumValues(schema, values);
        }
        String description = parameter.getDescription() == null ? "" : parameter.getDescription();
        if (!description.endsWith(".")) {
            description += ".";
        }
        parameter.setDescription(description + " Allowed properties: " + String.join(", ", sortable) + ".");
    }

    private void patchSchema(Schema<?> schema, Components components,
                             Map<String, Map<String, String[]>> dtoValues, Set<Schema<?>> visited) {
        if (schema == null || !visited.add(schema)) {
            return;
        }
        if (schema.get$ref() != null && components != null) {
            String name = refName(schema.get$ref());
            Map<String, String[]> fieldValues = dtoValues.get(name);
            if (fieldValues != null) {
                applyFieldEnums(components.getSchemas().get(name), fieldValues);
            }
            return;
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(s -> patchSchema(s, components, dtoValues, visited));
        }
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(s -> patchSchema(s, components, dtoValues, visited));
        }
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(s -> patchSchema(s, components, dtoValues, visited));
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(s -> patchSchema(s, components, dtoValues, visited));
        }
    }

    private void applyFieldEnums(Schema<?> dtoSchema, Map<String, String[]> fieldValues) {
        if (dtoSchema == null || dtoSchema.getProperties() == null) {
            return;
        }
        for (Map.Entry<String, String[]> entry : fieldValues.entrySet()) {
            Schema<?> property = dtoSchema.getProperties().get(entry.getKey());
            if (property == null || (property.getType() != null && !"string".equals(property.getType()))) {
                continue;
            }
            String[] values = entry.getValue();
            setEnumValues(property, asList(values));
            String description = property.getDescription() == null ? "" : property.getDescription();
            String note = "Allowed values: " + String.join(", ", values) + " (suggested, not enforced).";
            if (description.contains("Allowed values:")) {
                continue;
            }
            property.setDescription(description.isEmpty() ? note : description + " " + note);
        }
    }

    private void buildRegistry() {
        if (!registry.isEmpty()) {
            return;
        }
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            Set<String> patterns = info.getPathPatternsCondition() == null
                    ? Set.of() : info.getPathPatternsCondition().getPatternValues();
            if (patterns.isEmpty() && info.getPatternsCondition() != null) {
                patterns = info.getPatternsCondition().getPatterns();
            }
            if (methods.isEmpty() || patterns.isEmpty()) {
                continue;
            }
            SortableFields sortable = handlerMethod.getMethod().getAnnotation(SortableFields.class);
            EndpointDocs methodDocs = null;
            if (sortable != null) {
                methodDocs = new EndpointDocs();
                methodDocs.sortable = sortable.value().split(",");
            }
            for (RequestMethod method : methods) {
                for (String pattern : patterns) {
                    EndpointDocs docs = registry.computeIfAbsent(key(method, pattern), k -> new EndpointDocs());
                    if (sortable != null) {
                        docs.sortable = methodDocs.sortable;
                    }
                    collectParamDocs(handlerMethod, docs);
                    collectDtoDocs(handlerMethod, docs);
                }
            }
        }
    }

    private void collectParamDocs(HandlerMethod handlerMethod, EndpointDocs docs) {
        for (var methodParameter : handlerMethod.getMethodParameters()) {
            RequestParam requestParam = methodParameter.getParameterAnnotation(RequestParam.class);
            AllowedValues allowedValues = methodParameter.getParameterAnnotation(AllowedValues.class);
            if (requestParam == null || allowedValues == null) {
                continue;
            }
            String name = requestParam.name();
            if (name.isBlank()) {
                name = methodParameter.getParameterName();
            }
            if (name != null) {
                docs.paramValues.put(name, allowedValues.value());
            }
        }
    }

    private void collectDtoDocs(HandlerMethod handlerMethod, EndpointDocs docs) {
        for (var methodParameter : handlerMethod.getMethodParameters()) {
            Map<String, String[]> fieldValues = scanDtoFields(methodParameter.getParameterType());
            if (fieldValues != null) {
                docs.dtoValues.put(methodParameter.getParameterType().getSimpleName(), fieldValues);
            }
        }
    }

    private Map<String, String[]> scanDtoFields(Class<?> type) {
        Map<String, String[]> fieldValues = null;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                AllowedValues allowedValues = field.getAnnotation(AllowedValues.class);
                if (allowedValues != null) {
                    if (fieldValues == null) {
                        fieldValues = new HashMap<>();
                    }
                    fieldValues.put(field.getName(), allowedValues.value());
                }
            }
        }
        return fieldValues;
    }

    private static String refName(String ref) {
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    private static List<String> asList(String[] values) {
        return Arrays.asList(values);
    }

    private static String key(RequestMethod method, String path) {
        return method.name() + " " + path;
    }

    private static String key(PathItem.HttpMethod method, String path) {
        return method.name() + " " + path;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setEnumValues(Schema schema, List<String> values) {
        schema.setEnum(new ArrayList(values));
    }

    private static final class EndpointDocs {
        String[] sortable;
        final Map<String, String[]> paramValues = new HashMap<>();
        final Map<String, Map<String, String[]>> dtoValues = new HashMap<>();
    }
}
