package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.kx.heartbeat.domain.flow.validation.FlowValidationIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * Flow DSL 到 BPMN XML 的编译结果。
 *
 * <p>用于承接 BPMN XML、流程定义键、内容摘要和画布映射信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowBpmnCompileResult {

    /**
     * 是否编译成功。
     */
    private boolean valid;

    /**
     * 流程定义键。
     */
    private String processDefinitionKey;

    /**
     * 流程定义名称。
     */
    private String processName;

    /**
     * BPMN 资源名称。
     */
    private String resourceName;

    /**
     * BPMN XML 内容。
     */
    private String bpmnXml;

    /**
     * BPMN XML SHA-256 摘要。
     */
    private String bpmnSha256;

    /**
     * BPMN 元素映射列表。
     */
    private List<FlowBpmnElementMapping> mappings = new ArrayList<>();

    /**
     * 编译问题列表。
     */
    private List<FlowValidationIssue> issues = new ArrayList<>();

    /**
     * 创建失败编译结果。
     *
     * @param issues 编译问题列表
     * @return 失败编译结果
     */
    public static FlowBpmnCompileResult invalid(List<FlowValidationIssue> issues) {
        // 创建编译结果。
        FlowBpmnCompileResult result = new FlowBpmnCompileResult();
        // 标记编译失败。
        result.setValid(false);
        // 写入编译问题。
        result.setIssues(issues);
        // 返回失败编译结果。
        return result;
    }
}
