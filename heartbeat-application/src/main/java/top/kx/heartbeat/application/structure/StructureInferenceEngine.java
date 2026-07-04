package top.kx.heartbeat.application.structure;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.structure.dto.InferenceResult;
import top.kx.heartbeat.domain.structure.model.InferenceWarning;
import top.kx.heartbeat.domain.structure.model.StructureNode;
import top.kx.heartbeat.domain.structure.model.StructureType;

import java.util.*;

/**
 * 将多份 JSON 样例合并为格式无关的结构模型。
 */
@Component
public class StructureInferenceEngine {

    public InferenceResult infer(List<JsonNode> samples) {
        if (CollectionUtils.isEmpty(samples)) {
            throw new IllegalArgumentException("至少需要一份 JSON 样例");
        }

        List<StructureNode> roots = new ArrayList<>();
        for (JsonNode sample : samples) {
            if (sample == null) {
                throw new IllegalArgumentException("JSON 样例不能为 null");
            }
            roots.add(observe(sample, "$", "$"));
        }

        List<InferenceWarning> warnings = new ArrayList<>();
        StructureNode root = merge(roots, samples.size(), "$", "$", warnings);
        root.setRequired(true);
        return new InferenceResult(root, warnings);
    }

    private StructureNode observe(JsonNode value, String path, String name) {
        StructureNode node = new StructureNode(path, name);
        node.setOccurrenceCount(1);
        node.setTotalSampleCount(1);
        node.setRequired(true);

        if (value.isNull()) {
            node.addType(StructureType.NULL);
            return node;
        }
        if (value.isObject()) {
            node.addType(StructureType.OBJECT);
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                node.putProperty(field.getKey(), observe(
                        field.getValue(),
                        propertyPath(path, field.getKey()),
                        field.getKey()
                ));
            }
            return node;
        }
        if (value.isArray()) {
            node.addType(StructureType.ARRAY);
            List<StructureNode> elements = new ArrayList<>();
            for (JsonNode element : value) {
                elements.add(observe(element, path + "[]", "items"));
            }
            if (CollectionUtils.isNotEmpty(elements)) {
                node.setItems(merge(elements, elements.size(), path + "[]", "items", new ArrayList<InferenceWarning>()));
            }
            return node;
        }
        if (value.isIntegralNumber()) {
            node.addType(StructureType.INTEGER);
        } else if (value.isNumber()) {
            node.addType(StructureType.NUMBER);
        } else if (value.isBoolean()) {
            node.addType(StructureType.BOOLEAN);
        } else {
            node.addType(StructureType.STRING);
        }
        return node;
    }

    private StructureNode merge(List<StructureNode> observed,
                                int totalSampleCount,
                                String path,
                                String name,
                                List<InferenceWarning> warnings) {
        StructureNode merged = new StructureNode(path, name);
        merged.setOccurrenceCount(observed.size());
        merged.setTotalSampleCount(totalSampleCount);
        merged.setRequired(observed.size() == totalSampleCount);

        Set<StructureType> types = new LinkedHashSet<>();
        boolean nullable = false;
        for (StructureNode node : observed) {
            types.addAll(node.getTypes());
            nullable = nullable || node.getTypes().contains(StructureType.NULL) || node.isNullable();
        }
        types.remove(StructureType.NULL);
        if (types.contains(StructureType.INTEGER) && types.contains(StructureType.NUMBER)) {
            types.remove(StructureType.INTEGER);
        }
        for (StructureType type : types) {
            merged.addType(type);
        }
        merged.setNullable(nullable);

        mergeObjectProperties(observed, merged, warnings);
        mergeArrayItems(observed, merged, warnings);

        if (types.size() > 1) {
            warnings.add(new InferenceWarning(
                    "TYPE_CONFLICT",
                    path,
                    "同一路径观察到多个不兼容类型: " + types
            ));
        }
        return merged;
    }

    private void mergeObjectProperties(List<StructureNode> observed,
                                       StructureNode merged,
                                       List<InferenceWarning> warnings) {
        List<StructureNode> objects = new ArrayList<>();
        for (StructureNode node : observed) {
            if (node.getTypes().contains(StructureType.OBJECT)) {
                objects.add(node);
            }
        }
        if (CollectionUtils.isEmpty(objects)) {
            return;
        }

        Map<String, List<StructureNode>> properties = new LinkedHashMap<>();
        for (StructureNode object : objects) {
            for (Map.Entry<String, StructureNode> property : object.getProperties().entrySet()) {
                properties.computeIfAbsent(property.getKey(), key -> new ArrayList<>()).add(property.getValue());
            }
        }
        for (Map.Entry<String, List<StructureNode>> property : properties.entrySet()) {
            String childPath = propertyPath(merged.getPath(), property.getKey());
            merged.putProperty(property.getKey(), merge(
                    property.getValue(),
                    objects.size(),
                    childPath,
                    property.getKey(),
                    warnings
            ));
        }
    }

    private void mergeArrayItems(List<StructureNode> observed,
                                 StructureNode merged,
                                 List<InferenceWarning> warnings) {
        List<StructureNode> itemNodes = new ArrayList<>();
        boolean sawArray = false;
        for (StructureNode node : observed) {
            if (node.getTypes().contains(StructureType.ARRAY)) {
                sawArray = true;
                if (node.getItems() != null) {
                    itemNodes.add(node.getItems());
                }
            }
        }
        if (!sawArray) {
            return;
        }
        if (CollectionUtils.isEmpty(itemNodes)) {
            warnings.add(new InferenceWarning(
                    "EMPTY_ARRAY_ITEM_UNKNOWN",
                    merged.getPath(),
                    "所有样例中的数组均为空，无法推断元素类型"
            ));
            return;
        }
        merged.setItems(merge(
                itemNodes,
                itemNodes.size(),
                merged.getPath() + "[]",
                "items",
                warnings
        ));
    }

    private String propertyPath(String parent, String property) {
        return "$".equals(parent) ? "$." + property : parent + "." + property;
    }
}
