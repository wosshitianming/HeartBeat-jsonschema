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
        ObjectNode schema = objectMapper.createObjectNode();
        writeTypes(schema, node);

        if (node.getTypes().contains(StructureType.OBJECT)) {
            ObjectNode properties = schema.putObject("properties");
            ArrayNode required = objectMapper.createArrayNode();
            for (Map.Entry<String, StructureNode> property : node.getProperties().entrySet()) {
                properties.set(property.getKey(), schemaFor(property.getValue(), options));
                if (property.getValue().isRequired()) {
                    required.add(property.getKey());
                }
            }
            if (required.size() > 0) {
                schema.set("required", required);
            }
            schema.put("additionalProperties", options.getValidationMode() == ValidationMode.LENIENT);
        }

        if (node.getTypes().contains(StructureType.ARRAY) && node.getItems() != null) {
            schema.set("items", schemaFor(node.getItems(), options));
        }
        return schema;
    }

    private void writeTypes(ObjectNode schema, StructureNode node) {
        List<String> types = new ArrayList<>();
        for (StructureType type : node.getTypes()) {
            types.add(jsonType(type));
        }
        if (node.isNullable()) {
            types.add("null");
        }
        if (CollectionUtils.isEmpty(types)) {
            types.add("null");
        }
        if (types.size() == 1) {
            schema.put("type", types.get(0));
        } else {
            ArrayNode typeArray = schema.putArray("type");
            for (String type : types) {
                typeArray.add(type);
            }
        }
    }

    private String jsonType(StructureType type) {
        return type.name().toLowerCase();
    }
}
