package top.kx.heartbeat.application.structure.validation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.structure.dto.ValidationError;
import top.kx.heartbeat.application.structure.dto.ValidationMode;
import top.kx.heartbeat.domain.structure.model.StructureNode;
import top.kx.heartbeat.domain.structure.model.StructureType;

import java.util.*;

@Component
public class StructurePayloadValidator {

    public List<ValidationError> validate(StructureNode model, JsonNode payload, ValidationMode mode) {
        List<ValidationError> errors = new ArrayList<>();
        validateNode(model, payload, "$", mode == null ? ValidationMode.LENIENT : mode, errors);
        return errors;
    }

    private void validateNode(StructureNode model,
                              JsonNode value,
                              String path,
                              ValidationMode mode,
                              List<ValidationError> errors) {
        if (value == null || value.isMissingNode()) {
            return;
        }
        if (value.isNull()) {
            if (!model.isNullable()) {
                errors.add(error(path, "type", expected(model), "null"));
            }
            return;
        }
        if (!matches(model.getTypes(), value)) {
            errors.add(error(path, "type", expected(model), actual(value)));
            return;
        }

        if (value.isObject() && model.getTypes().contains(StructureType.OBJECT)) {
            validateObject(model, value, path, mode, errors);
        } else if (value.isArray()
                && model.getTypes().contains(StructureType.ARRAY)
                && model.getItems() != null) {
            for (int index = 0; index < value.size(); index++) {
                validateNode(model.getItems(), value.get(index), path + "[" + index + "]", mode, errors);
            }
        }
    }

    private void validateObject(StructureNode model,
                                JsonNode value,
                                String path,
                                ValidationMode mode,
                                List<ValidationError> errors) {
        for (Map.Entry<String, StructureNode> property : model.getProperties().entrySet()) {
            String propertyPath = propertyPath(path, property.getKey());
            if (!value.has(property.getKey())) {
                if (property.getValue().isRequired()) {
                    errors.add(new ValidationError(
                            propertyPath,
                            "required",
                            "present",
                            "missing",
                            propertyPath + " 为必填字段"
                    ));
                }
                continue;
            }
            validateNode(property.getValue(), value.get(property.getKey()), propertyPath, mode, errors);
        }

        if (mode == ValidationMode.STRICT) {
            Iterator<String> names = value.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!model.getProperties().containsKey(name)) {
                    String propertyPath = propertyPath(path, name);
                    errors.add(new ValidationError(
                            propertyPath,
                            "additionalProperties",
                            "defined property",
                            "unknown property",
                            propertyPath + " 未在结构定义中声明"
                    ));
                }
            }
        }
    }

    private boolean matches(Set<StructureType> types, JsonNode value) {
        for (StructureType type : types) {
            switch (type) {
                case OBJECT:
                    if (value.isObject()) return true;
                    break;
                case ARRAY:
                    if (value.isArray()) return true;
                    break;
                case STRING:
                    if (value.isTextual()) return true;
                    break;
                case INTEGER:
                    if (value.isIntegralNumber()) return true;
                    break;
                case NUMBER:
                    if (value.isNumber()) return true;
                    break;
                case BOOLEAN:
                    if (value.isBoolean()) return true;
                    break;
                case NULL:
                    if (value.isNull()) return true;
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    private ValidationError error(String path, String keyword, String expected, String actual) {
        return new ValidationError(
                path,
                keyword,
                expected,
                actual,
                path + "：期望 " + expected + "，实际 " + actual
        );
    }

    private String expected(StructureNode model) {
        return model.getTypes().toString() + (model.isNullable() ? " or null" : "");
    }

    private String actual(JsonNode value) {
        if (value.isObject()) return "object";
        if (value.isArray()) return "array";
        if (value.isTextual()) return "string";
        if (value.isIntegralNumber()) return "integer";
        if (value.isNumber()) return "number";
        if (value.isBoolean()) return "boolean";
        if (value.isNull()) return "null";
        return "unknown";
    }

    private String propertyPath(String parent, String property) {
        return "$".equals(parent) ? "$." + property : parent + "." + property;
    }
}
