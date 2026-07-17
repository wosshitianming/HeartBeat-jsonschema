package top.kx.heartbeat.application.flow.runtime.node;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.application.flow.runtime.CodeNode;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionContext;
import top.kx.heartbeat.application.flow.runtime.NodeExecutionResult;
import top.kx.heartbeat.application.flow.runtime.NodeManifestBuilder;
import top.kx.heartbeat.domain.flow.model.FlowNodeRunStatus;
import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 将 payload 指定字段转换为大写文本的示例代码节点。
 */
@Component
public class UppercaseCodeNode implements CodeNode {

    private static final String EXECUTOR_ID = "code:transform.text.uppercase";

    @Override
    public String executorId() {
        return EXECUTOR_ID;
    }

    @Override
    public NodeComponentManifest manifest() {
        return NodeManifestBuilder.codeNode(this, "transform.text.uppercase", "文本转大写")
                .category("转换")
                .description("将 payload 中指定字段的文本转换为大写")
                .icon("case-upper")
                .requiredInput("in", "输入", "object")
                .output("out", "输出", "object")
                .output("error", "错误", "object")
                .stringProperty("field", "字段名称", "text", true)
                .capability("transform")
                .build();
    }

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        Map<String, Object> input = context == null || context.getInput() == null
                ? Collections.emptyMap() : context.getInput();
        Map<String, Object> config = context == null || context.getNode() == null
                || context.getNode().getConfig() == null
                ? Collections.emptyMap() : context.getNode().getConfig();
        Map<String, Object> output = new LinkedHashMap<>(input);
        Object configuredField = config.get("field");
        String field = configuredField == null
                ? "text" : StringUtils.trimToNull(String.valueOf(configuredField));
        if (field == null) {
            return failed(output, "字段名称不能为空");
        }
        Object value = output.get(field);
        if (!(value instanceof CharSequence)) {
            return failed(output, "字段 " + field + " 不存在或不是文本");
        }
        output.put(field, value.toString().toUpperCase(Locale.ROOT));
        return new NodeExecutionResult(
                FlowNodeRunStatus.SUCCESS.getCode(), output, Collections.singletonList("out"), null);
    }

    private NodeExecutionResult failed(Map<String, Object> output, String message) {
        return new NodeExecutionResult(
                FlowNodeRunStatus.FAILED.getCode(), output, Collections.singletonList("error"), message);
    }
}
