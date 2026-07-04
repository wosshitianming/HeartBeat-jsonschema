package top.kx.heartbeat.infrastructure.structure.converter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.structure.model.InferenceWarning;
import top.kx.heartbeat.domain.structure.model.StructureNode;
import top.kx.heartbeat.domain.structure.model.StructureType;

import javax.annotation.Resource;
import java.util.*;

@Component
public class StructureModelJsonCodec {

    @Resource
    private ObjectMapper objectMapper;

    public String writeModel(StructureNode node) {
        return write(nodeToJson(node));
    }

    public StructureNode readModel(String json) {
        return jsonToNode(read(json));
    }

    public String writeArtifacts(Map<String, String> artifacts) {
        ObjectNode root = objectMapper.createObjectNode();
        for (Map.Entry<String, String> artifact : artifacts.entrySet()) {
            root.set(artifact.getKey(), read(artifact.getValue()));
        }
        return write(root);
    }

    public Map<String, String> readArtifacts(String json) {
        Map<String, String> artifacts = new LinkedHashMap<>();
        read(json).fields().forEachRemaining(entry ->
                artifacts.put(entry.getKey(), write(entry.getValue())));
        return artifacts;
    }

    public String writeWarnings(List<InferenceWarning> warnings) {
        ArrayNode array = objectMapper.createArrayNode();
        for (InferenceWarning warning : warnings) {
            ObjectNode item = array.addObject();
            item.put("code", warning.getCode());
            item.put("path", warning.getPath());
            item.put("message", warning.getMessage());
        }
        return write(array);
    }

    public List<InferenceWarning> readWarnings(String json) {
        List<InferenceWarning> warnings = new ArrayList<>();
        for (JsonNode item : read(json)) {
            warnings.add(new InferenceWarning(
                    item.path("code").asText(),
                    item.path("path").asText(),
                    item.path("message").asText()
            ));
        }
        return warnings;
    }

    private ObjectNode nodeToJson(StructureNode node) {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("path", node.getPath());
        json.put("name", node.getName());
        ArrayNode types = json.putArray("types");
        for (StructureType type : node.getTypes()) {
            types.add(type.name());
        }
        json.put("required", node.isRequired());
        json.put("nullable", node.isNullable());
        json.put("occurrenceCount", node.getOccurrenceCount());
        json.put("totalSampleCount", node.getTotalSampleCount());
        ObjectNode properties = json.putObject("properties");
        for (Map.Entry<String, StructureNode> property : node.getProperties().entrySet()) {
            properties.set(property.getKey(), nodeToJson(property.getValue()));
        }
        if (node.getItems() != null) {
            json.set("items", nodeToJson(node.getItems()));
        }
        return json;
    }

    private StructureNode jsonToNode(JsonNode json) {
        StructureNode node = new StructureNode(json.path("path").asText(), json.path("name").asText());
        for (JsonNode type : json.path("types")) {
            node.addType(StructureType.valueOf(type.asText()));
        }
        node.setRequired(json.path("required").asBoolean());
        node.setNullable(json.path("nullable").asBoolean());
        node.setOccurrenceCount(json.path("occurrenceCount").asInt());
        node.setTotalSampleCount(json.path("totalSampleCount").asInt());
        Iterator<Map.Entry<String, JsonNode>> properties = json.path("properties").fields();
        while (properties.hasNext()) {
            Map.Entry<String, JsonNode> property = properties.next();
            node.putProperty(property.getKey(), jsonToNode(property.getValue()));
        }
        if (json.has("items")) {
            node.setItems(jsonToNode(json.get("items")));
        }
        return node;
    }

    private String write(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("结构 JSON 序列化失败", ex);
        }
    }

    private JsonNode read(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("结构 JSON 反序列化失败", ex);
        }
    }
}
