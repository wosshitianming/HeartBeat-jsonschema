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
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (CollectionUtils.isEmpty(samples)) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("至少需要一份 JSON 样例");
        }

        // 创建结果集合，承接后续逐项组装的数据。
        List<StructureNode> roots = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (JsonNode sample : samples) {
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (sample == null) {
                // 对非法业务状态立即失败，避免错误继续扩散。
                throw new IllegalArgumentException("JSON 样例不能为 null");
            }
            // 加入当前处理结果，供后续批量返回或继续组装。
            roots.add(observe(sample, "$", "$"));
        }

        // 创建结果集合，承接后续逐项组装的数据。
        List<InferenceWarning> warnings = new ArrayList<>();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureNode root = merge(roots, samples.size(), "$", "$", warnings);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        root.setRequired(true);
        // 返回已经完成封装的业务结果。
        return new InferenceResult(root, warnings);
    }

    private StructureNode observe(JsonNode value, String path, String name) {
        // 创建当前流程需要的临时对象，承载后续处理数据。
        StructureNode node = new StructureNode(path, name);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        node.setOccurrenceCount(1);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        node.setTotalSampleCount(1);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        node.setRequired(true);

        // 根据当前业务条件选择对应处理路径。
        if (value.isNull()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.addType(StructureType.NULL);
            // 返回已经完成封装的业务结果。
            return node;
        }
        // 根据当前业务条件选择对应处理路径。
        if (value.isObject()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.addType(StructureType.OBJECT);
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            // 持续读取可用数据，直到当前数据源处理完成。
            while (fields.hasNext()) {
                // 计算当前步骤所需的中间值，供后续业务判断使用。
                Map.Entry<String, JsonNode> field = fields.next();
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                node.putProperty(field.getKey(), observe(
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        field.getValue(),
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        propertyPath(path, field.getKey()),
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        field.getKey()
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                ));
            }
            // 返回已经完成封装的业务结果。
            return node;
        }
        // 根据当前业务条件选择对应处理路径。
        if (value.isArray()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.addType(StructureType.ARRAY);
            // 创建结果集合，承接后续逐项组装的数据。
            List<StructureNode> elements = new ArrayList<>();
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (JsonNode element : value) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                elements.add(observe(element, path + "[]", "items"));
            }
            // 根据当前业务条件选择对应处理路径。
            if (CollectionUtils.isNotEmpty(elements)) {
                // 创建结果集合，承接后续逐项组装的数据。
                node.setItems(merge(elements, elements.size(), path + "[]", "items", new ArrayList<InferenceWarning>()));
            }
            // 返回已经完成封装的业务结果。
            return node;
        }
        // 根据当前业务条件选择对应处理路径。
        if (value.isIntegralNumber()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.addType(StructureType.INTEGER);
        } else if (value.isNumber()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.addType(StructureType.NUMBER);
        } else if (value.isBoolean()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.addType(StructureType.BOOLEAN);
        } else {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.addType(StructureType.STRING);
        }
        // 返回已经完成封装的业务结果。
        return node;
    }

    private StructureNode merge(List<StructureNode> observed,
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                int totalSampleCount,
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                String path,
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                String name,
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                List<InferenceWarning> warnings) {
        // 创建当前流程需要的临时对象，承载后续处理数据。
        StructureNode merged = new StructureNode(path, name);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        merged.setOccurrenceCount(observed.size());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        merged.setTotalSampleCount(totalSampleCount);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        merged.setRequired(observed.size() == totalSampleCount);

        // 创建去重集合，避免重复标识影响后续查询。
        Set<StructureType> types = new LinkedHashSet<>();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        boolean nullable = false;
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureNode node : observed) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            types.addAll(node.getTypes());
            // 计算当前分支的中间结果，供后续判断或组装使用。
            nullable = nullable || node.getTypes().contains(StructureType.NULL) || node.isNullable();
        }
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        types.remove(StructureType.NULL);
        // 根据当前业务条件选择对应处理路径。
        if (types.contains(StructureType.INTEGER) && types.contains(StructureType.NUMBER)) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            types.remove(StructureType.INTEGER);
        }
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureType type : types) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            merged.addType(type);
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        merged.setNullable(nullable);

        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        mergeObjectProperties(observed, merged, warnings);
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        mergeArrayItems(observed, merged, warnings);

        // 根据当前业务条件选择对应处理路径。
        if (types.size() > 1) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            warnings.add(new InferenceWarning(
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    "TYPE_CONFLICT",
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    path,
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    "同一路径观察到多个不兼容类型: " + types
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            ));
        }
        // 返回已经完成封装的业务结果。
        return merged;
    }

    private void mergeObjectProperties(List<StructureNode> observed,
                                       // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                       StructureNode merged,
                                       // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                       List<InferenceWarning> warnings) {
        // 创建结果集合，承接后续逐项组装的数据。
        List<StructureNode> objects = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureNode node : observed) {
            // 根据当前业务条件选择对应处理路径。
            if (node.getTypes().contains(StructureType.OBJECT)) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                objects.add(node);
            }
        }
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (CollectionUtils.isEmpty(objects)) {
            // 返回已经完成封装的业务结果。
            return;
        }

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, List<StructureNode>> properties = new LinkedHashMap<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureNode object : objects) {
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (Map.Entry<String, StructureNode> property : object.getProperties().entrySet()) {
                // 创建结果集合，承接后续逐项组装的数据。
                properties.computeIfAbsent(property.getKey(), key -> new ArrayList<>()).add(property.getValue());
            }
        }
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map.Entry<String, List<StructureNode>> property : properties.entrySet()) {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String childPath = propertyPath(merged.getPath(), property.getKey());
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            merged.putProperty(property.getKey(), merge(
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    property.getValue(),
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    objects.size(),
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    childPath,
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    property.getKey(),
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    warnings
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            ));
        }
    }

    private void mergeArrayItems(List<StructureNode> observed,
                                 // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                 StructureNode merged,
                                 // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                 List<InferenceWarning> warnings) {
        // 创建结果集合，承接后续逐项组装的数据。
        List<StructureNode> itemNodes = new ArrayList<>();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        boolean sawArray = false;
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureNode node : observed) {
            // 根据当前业务条件选择对应处理路径。
            if (node.getTypes().contains(StructureType.ARRAY)) {
                // 计算当前分支的中间结果，供后续判断或组装使用。
                sawArray = true;
                // 先处理空值或缺省场景，避免后续业务流程出现空指针。
                if (node.getItems() != null) {
                    // 加入当前处理结果，供后续批量返回或继续组装。
                    itemNodes.add(node.getItems());
                }
            }
        }
        // 根据当前业务条件选择对应处理路径。
        if (!sawArray) {
            // 返回已经完成封装的业务结果。
            return;
        }
        // 校验关键文本参数，防止无效输入继续向后流转。
        if (CollectionUtils.isEmpty(itemNodes)) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            warnings.add(new InferenceWarning(
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    "EMPTY_ARRAY_ITEM_UNKNOWN",
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    merged.getPath(),
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    "所有样例中的数组均为空，无法推断元素类型"
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            ));
            // 返回已经完成封装的业务结果。
            return;
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        merged.setItems(merge(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                itemNodes,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                itemNodes.size(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                merged.getPath() + "[]",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "items",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                warnings
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        ));
    }

    private String propertyPath(String parent, String property) {
        return "$".equals(parent) ? "$." + property : parent + "." + property;
    }
}
