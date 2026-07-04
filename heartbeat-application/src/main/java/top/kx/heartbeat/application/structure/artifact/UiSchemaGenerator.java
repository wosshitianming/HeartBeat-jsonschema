package top.kx.heartbeat.application.structure.artifact;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.structure.dto.GenerationOptions;
import top.kx.heartbeat.domain.structure.model.StructureNode;
import top.kx.heartbeat.domain.structure.model.StructureType;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class UiSchemaGenerator implements ArtifactGenerator {

    public static final String TYPE = "UI_SCHEMA";
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String artifactType() {
        return TYPE;
    }

    @Override
    public JsonNode generate(StructureNode model, GenerationOptions options) {
        return uiFor(model, options);
    }

    private ObjectNode uiFor(StructureNode node, GenerationOptions options) {
        ObjectNode ui = objectMapper.createObjectNode();
        ui.put("title", node.getName());
        ui.put("widget", defaultWidget(node));

        if (MapUtils.isNotEmpty(node.getProperties())) {
            ArrayNode order = ui.putArray("order");
            ObjectNode fields = ui.putObject("fields");
            for (Map.Entry<String, StructureNode> property : node.getProperties().entrySet()) {
                order.add(property.getKey());
                fields.set(property.getKey(), uiFor(property.getValue(), options));
            }
        }
        if (node.getItems() != null) {
            ui.set("items", uiFor(node.getItems(), options));
        }

        JsonNode override = options.getUiOverrides().path(node.getPath());
        if (override.isObject()) {
            override.fields().forEachRemaining(entry -> ui.set(entry.getKey(), entry.getValue()));
        }
        return ui;
    }

    private String defaultWidget(StructureNode node) {
        if (node.getTypes().contains(StructureType.OBJECT)) {
            return "fieldset";
        }
        if (node.getTypes().contains(StructureType.ARRAY)) {
            return "list";
        }
        if (node.getTypes().contains(StructureType.BOOLEAN)) {
            return "switch";
        }
        if (node.getTypes().contains(StructureType.INTEGER)
                || node.getTypes().contains(StructureType.NUMBER)) {
            return "number";
        }
        return "text";
    }
}
