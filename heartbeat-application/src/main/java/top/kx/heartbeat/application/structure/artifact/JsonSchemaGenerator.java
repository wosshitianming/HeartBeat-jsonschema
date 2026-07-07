package top.kx.heartbeat.application.structure.artifact;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.structure.dto.GenerationOptions;
import top.kx.heartbeat.application.structure.dto.ValidationMode;
import top.kx.heartbeat.domain.structure.model.StructureNode;
import top.kx.heartbeat.domain.structure.model.StructureType;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JsonSchemaGenerator implements ArtifactGenerator {

    public static final String TYPE = "JSON_SCHEMA";
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String artifactType() {
        return TYPE;
    }

    @Override
    public JsonNode generate(StructureNode model, GenerationOptions options) {
        ObjectNode root = schemaFor(model, options);
        root.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        return root;
    }

    private ObjectNode schemaFor(StructureNode node, GenerationOptions options) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ObjectNode schema = objectMapper.createObjectNode();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        writeTypes(schema, node);

        // 根据当前业务条件选择对应处理路径。
        if (node.getTypes().contains(StructureType.OBJECT)) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            ObjectNode properties = schema.putObject("properties");
            // 计算当前分支的中间结果，供后续判断或组装使用。
            ArrayNode required = objectMapper.createArrayNode();
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (Map.Entry<String, StructureNode> property : node.getProperties().entrySet()) {
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                properties.set(property.getKey(), schemaFor(property.getValue(), options));
                // 根据当前业务条件选择对应处理路径。
                if (property.getValue().isRequired()) {
                    // 加入当前处理结果，供后续批量返回或继续组装。
                    required.add(property.getKey());
                }
            }
            // 根据当前业务条件选择对应处理路径。
            if (required.size() > 0) {
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                schema.set("required", required);
            }
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            schema.put("additionalProperties", options.getValidationMode() == ValidationMode.LENIENT);
        }

        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (node.getTypes().contains(StructureType.ARRAY) && node.getItems() != null) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            schema.set("items", schemaFor(node.getItems(), options));
        }
        // 返回已经完成封装的业务结果。
        return schema;
    }

    private void writeTypes(ObjectNode schema, StructureNode node) {
        // 创建结果集合，承接后续逐项组装的数据。
        List<String> types = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureType type : node.getTypes()) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            types.add(jsonType(type));
        }
        // 根据当前业务条件选择对应处理路径。
        if (node.isNullable()) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            types.add("null");
        }
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (CollectionUtils.isEmpty(types)) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            types.add("null");
        }
        // 根据当前业务条件选择对应处理路径。
        if (types.size() == 1) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            schema.put("type", types.get(0));
        } else {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            ArrayNode typeArray = schema.putArray("type");
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (String type : types) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                typeArray.add(type);
            }
        }
    }

    private String jsonType(StructureType type) {
        return type.name().toLowerCase();
    }
}
