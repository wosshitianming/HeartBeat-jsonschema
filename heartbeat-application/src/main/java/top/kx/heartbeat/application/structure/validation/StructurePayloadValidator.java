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
                              // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                              JsonNode value,
                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                              String path,
                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                              ValidationMode mode,
                              // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                              List<ValidationError> errors) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (value == null || value.isMissingNode()) {
            // 返回已经完成封装的业务结果。
            return;
        }
        // 根据当前业务条件选择对应处理路径。
        if (value.isNull()) {
            // 根据当前业务条件选择对应处理路径。
            if (!model.isNullable()) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                errors.add(error(path, "type", expected(model), "null"));
            }
            // 返回已经完成封装的业务结果。
            return;
        }
        // 比对当前业务状态，决定是否进入该处理分支。
        if (!matches(model.getTypes(), value)) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            errors.add(error(path, "type", expected(model), actual(value)));
            // 返回已经完成封装的业务结果。
            return;
        }

        // 根据当前业务条件选择对应处理路径。
        if (value.isObject() && model.getTypes().contains(StructureType.OBJECT)) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            validateObject(model, value, path, mode, errors);
        } else if (value.isArray()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                && model.getTypes().contains(StructureType.ARRAY)
                // 计算当前分支的中间结果，供后续判断或组装使用。
                && model.getItems() != null) {
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (int index = 0; index < value.size(); index++) {
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                validateNode(model.getItems(), value.get(index), path + "[" + index + "]", mode, errors);
            }
        }
    }

    private void validateObject(StructureNode model,
                                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                                JsonNode value,
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                String path,
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                ValidationMode mode,
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                List<ValidationError> errors) {
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map.Entry<String, StructureNode> property : model.getProperties().entrySet()) {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String propertyPath = propertyPath(path, property.getKey());
            // 根据当前业务条件选择对应处理路径。
            if (!value.has(property.getKey())) {
                // 根据当前业务条件选择对应处理路径。
                if (property.getValue().isRequired()) {
                    // 加入当前处理结果，供后续批量返回或继续组装。
                    errors.add(new ValidationError(
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            propertyPath,
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            "required",
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            "present",
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            "missing",
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            propertyPath + " 为必填字段"
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    ));
                }
                // 跳过当前不需要展示的节点，继续处理下一条数据。
                continue;
            }
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            validateNode(property.getValue(), value.get(property.getKey()), propertyPath, mode, errors);
        }

        // 根据当前业务条件选择对应处理路径。
        if (mode == ValidationMode.STRICT) {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            Iterator<String> names = value.fieldNames();
            // 持续读取可用数据，直到当前数据源处理完成。
            while (names.hasNext()) {
                // 计算当前步骤所需的中间值，供后续业务判断使用。
                String name = names.next();
                // 根据当前业务条件选择对应处理路径。
                if (!model.getProperties().containsKey(name)) {
                    // 计算当前步骤所需的中间值，供后续业务判断使用。
                    String propertyPath = propertyPath(path, name);
                    // 加入当前处理结果，供后续批量返回或继续组装。
                    errors.add(new ValidationError(
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            propertyPath,
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            "additionalProperties",
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            "defined property",
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            "unknown property",
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                            propertyPath + " 未在结构定义中声明"
                            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    ));
                }
            }
        }
    }

    private boolean matches(Set<StructureType> types, JsonNode value) {
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureType type : types) {
            // 按业务类型分派处理逻辑，保持状态判断集中。
            switch (type) {
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                case OBJECT:
                    // 根据当前业务条件选择对应处理路径。
                    if (value.isObject()) return true;
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    break;
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                case ARRAY:
                    // 根据当前业务条件选择对应处理路径。
                    if (value.isArray()) return true;
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    break;
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                case STRING:
                    // 根据当前业务条件选择对应处理路径。
                    if (value.isTextual()) return true;
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    break;
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                case INTEGER:
                    // 根据当前业务条件选择对应处理路径。
                    if (value.isIntegralNumber()) return true;
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    break;
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                case NUMBER:
                    // 根据当前业务条件选择对应处理路径。
                    if (value.isNumber()) return true;
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    break;
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                case BOOLEAN:
                    // 根据当前业务条件选择对应处理路径。
                    if (value.isBoolean()) return true;
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    break;
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                case NULL:
                    // 根据当前业务条件选择对应处理路径。
                    if (value.isNull()) return true;
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    break;
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                default:
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    break;
            }
        }
        // 返回已经完成封装的业务结果。
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
        // 根据当前业务条件选择对应处理路径。
        if (value.isObject()) return "object";
        // 根据当前业务条件选择对应处理路径。
        if (value.isArray()) return "array";
        // 根据当前业务条件选择对应处理路径。
        if (value.isTextual()) return "string";
        // 根据当前业务条件选择对应处理路径。
        if (value.isIntegralNumber()) return "integer";
        // 根据当前业务条件选择对应处理路径。
        if (value.isNumber()) return "number";
        // 根据当前业务条件选择对应处理路径。
        if (value.isBoolean()) return "boolean";
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (value.isNull()) return "null";
        // 返回已经完成封装的业务结果。
        return "unknown";
    }

    private String propertyPath(String parent, String property) {
        return "$".equals(parent) ? "$." + property : parent + "." + property;
    }
}
