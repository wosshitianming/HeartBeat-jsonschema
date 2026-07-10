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

    private static final int MAX_JSON_UNWRAP_DEPTH = 8;

    @Resource
    private ObjectMapper objectMapper;

    public String writeModel(StructureNode node) {
        if (node == null) {
            throw new IllegalArgumentException("结构模型不能为空");
        }
        return write(nodeToJson(node));
    }

    public StructureNode readModel(String json) {
        JsonNode root = unwrapSerializedJson(read(json));
        if (root.isNull() || !root.isObject()) {
            throw new IllegalStateException("结构模型 JSON 必须是对象");
        }
        return jsonToNode(root);
    }

    public String writeArtifacts(Map<String, String> artifacts) {
        ObjectNode root = objectMapper.createObjectNode();
        if (artifacts == null) {
            return write(root);
        }
        for (Map.Entry<String, String> artifact : artifacts.entrySet()) {
            root.set(artifact.getKey(), unwrapSerializedJson(read(artifact.getValue())));
        }
        return write(root);
    }

    public Map<String, String> readArtifacts(String json) {
        Map<String, String> artifacts = new LinkedHashMap<>();
        JsonNode root = unwrapSerializedJson(read(json));
        if (root.isNull() || !root.isObject()) {
            throw new IllegalStateException("结构产物 JSON 必须是对象");
        }
        root.fields().forEachRemaining(entry ->
                artifacts.put(entry.getKey(), normalizeArtifact(entry.getValue())));
        return artifacts;
    }

    public String writeWarnings(List<InferenceWarning> warnings) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ArrayNode array = objectMapper.createArrayNode();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (InferenceWarning warning : warnings == null ? Collections.<InferenceWarning>emptyList() : warnings) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            ObjectNode item = array.addObject();
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("code", warning.getCode());
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("path", warning.getPath());
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("message", warning.getMessage());
        }
        // 返回已经完成封装的业务结果。
        return write(array);
    }

    public List<InferenceWarning> readWarnings(String json) {
        List<InferenceWarning> warnings = new ArrayList<>();
        JsonNode root = unwrapSerializedJson(read(json));
        if (root.isNull() || !root.isArray()) {
            throw new IllegalStateException("结构告警 JSON 必须是数组");
        }
        for (JsonNode item : root) {
            if (!item.isObject()
                    || !item.path("code").isTextual()
                    || !item.path("path").isTextual()
                    || !item.path("message").isTextual()) {
                throw new IllegalStateException("结构告警 JSON 缺少必要字段");
            }
            warnings.add(new InferenceWarning(
                    item.get("code").asText(),
                    item.get("path").asText(),
                    item.get("message").asText()
            ));
        }
        return warnings;
    }

    public String normalizeJson(String json) {
        if (json == null) {
            return "null";
        }
        return write(unwrapSerializedJson(read(json)));
    }

    private ObjectNode nodeToJson(StructureNode node) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ObjectNode json = objectMapper.createObjectNode();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        json.put("path", node.getPath());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        json.put("name", node.getName());
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ArrayNode types = json.putArray("types");
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureType type : node.getTypes()) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            types.add(type.name());
        }
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        json.put("required", node.isRequired());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        json.put("nullable", node.isNullable());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        json.put("occurrenceCount", node.getOccurrenceCount());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        json.put("totalSampleCount", node.getTotalSampleCount());
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ObjectNode properties = json.putObject("properties");
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map.Entry<String, StructureNode> property : node.getProperties().entrySet()) {
            // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
            properties.set(property.getKey(), nodeToJson(property.getValue()));
        }
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (node.getItems() != null) {
            // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
            json.set("items", nodeToJson(node.getItems()));
        }
        // 返回已经完成封装的业务结果。
        return json;
    }

    private StructureNode jsonToNode(JsonNode json) {
        if (!json.isObject()
                || !json.path("path").isTextual()
                || !json.path("name").isTextual()
                || !json.path("types").isArray()
                || !json.path("properties").isObject()) {
            throw new IllegalStateException("结构模型 JSON 缺少必要字段");
        }
        // 创建当前流程需要的临时对象，承载后续处理数据。
        StructureNode node = new StructureNode(json.get("path").asText(), json.get("name").asText());
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (JsonNode type : json.path("types")) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.addType(StructureType.valueOf(type.asText()));
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        node.setRequired(json.path("required").asBoolean());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        node.setNullable(json.path("nullable").asBoolean());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        node.setOccurrenceCount(json.path("occurrenceCount").asInt());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        node.setTotalSampleCount(json.path("totalSampleCount").asInt());
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Iterator<Map.Entry<String, JsonNode>> properties = json.path("properties").fields();
        // 持续读取可用数据，直到当前数据源处理完成。
        while (properties.hasNext()) {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            Map.Entry<String, JsonNode> property = properties.next();
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.putProperty(property.getKey(), jsonToNode(property.getValue()));
        }
        // 根据当前业务条件选择对应处理路径。
        if (json.hasNonNull("items")) {
            if (!json.get("items").isObject()) {
                throw new IllegalStateException("结构模型 items 必须是对象");
            }
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            node.setItems(jsonToNode(json.get("items")));
        }
        // 返回已经完成封装的业务结果。
        return node;
    }

    private String write(JsonNode value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("结构 JSON 序列化失败", ex);
        }
    }

    private String normalizeArtifact(JsonNode value) {
        return write(unwrapSerializedJson(value));
    }

    private JsonNode unwrapSerializedJson(JsonNode value) {
        JsonNode current = value;
        for (int depth = 0; depth < MAX_JSON_UNWRAP_DEPTH && current.isTextual(); depth++) {
            JsonNode nested = tryRead(current.textValue());
            if (nested == null) {
                break;
            }
            current = nested;
        }
        return current;
    }

    private JsonNode tryRead(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private JsonNode read(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("结构 JSON 不能为空");
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("结构 JSON 反序列化失败", ex);
        }
    }
}
