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
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ObjectNode ui = objectMapper.createObjectNode();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        ui.put("title", node.getName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        ui.put("widget", defaultWidget(node));

        // 根据当前业务条件选择对应处理路径。
        if (MapUtils.isNotEmpty(node.getProperties())) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            ArrayNode order = ui.putArray("order");
            // 计算当前分支的中间结果，供后续判断或组装使用。
            ObjectNode fields = ui.putObject("fields");
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (Map.Entry<String, StructureNode> property : node.getProperties().entrySet()) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                order.add(property.getKey());
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                fields.set(property.getKey(), uiFor(property.getValue(), options));
            }
        }
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (node.getItems() != null) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            ui.set("items", uiFor(node.getItems(), options));
        }

        // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
        JsonNode override = options.getUiOverrides().path(node.getPath());
        // 根据当前业务条件选择对应处理路径。
        if (override.isObject()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            override.fields().forEachRemaining(entry -> ui.set(entry.getKey(), entry.getValue()));
        }
        // 返回已经完成封装的业务结果。
        return ui;
    }

    private String defaultWidget(StructureNode node) {
        // 根据当前业务条件选择对应处理路径。
        if (node.getTypes().contains(StructureType.OBJECT)) {
            // 返回已经完成封装的业务结果。
            return "fieldset";
        }
        // 根据当前业务条件选择对应处理路径。
        if (node.getTypes().contains(StructureType.ARRAY)) {
            // 返回已经完成封装的业务结果。
            return "list";
        }
        // 根据当前业务条件选择对应处理路径。
        if (node.getTypes().contains(StructureType.BOOLEAN)) {
            // 返回已经完成封装的业务结果。
            return "switch";
        }
        // 根据当前业务条件选择对应处理路径。
        if (node.getTypes().contains(StructureType.INTEGER)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                || node.getTypes().contains(StructureType.NUMBER)) {
            // 返回已经完成封装的业务结果。
            return "number";
        }
        // 返回已经完成封装的业务结果。
        return "text";
    }
}
