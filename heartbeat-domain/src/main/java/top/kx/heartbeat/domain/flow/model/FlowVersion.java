package top.kx.heartbeat.domain.flow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 流程版本领域模型。
 *
 * <p>用于记录某次发布后的流程 DSL 快照与发布信息。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowVersion {

    /**
     * 流程版本标识。
     */
    private String id;

    /**
     * 流程定义标识。
     */
    private String flowId;

    /**
     * 流程版本号。
     */
    private int versionNo;

    /**
     * 流程 DSL 快照。
     */
    private FlowDefinition flowDsl;

    /**
     * 编译报告摘要。
     */
    private String compileReport;

    /**
     * 生产态运行时引擎。
     */
    private String runtimeEngine;

    /**
     * 生产态 BPMN XML。
     */
    private String bpmnXml;

    /**
     * BPMN XML 内容摘要。
     */
    private String bpmnSha256;

    /**
     * Flowable 部署标识。
     */
    private String deploymentId;

    /**
     * Flowable 流程定义标识。
     */
    private String processDefinitionId;

    /**
     * Flowable 流程定义键。
     */
    private String processDefinitionKey;

    /**
     * 编译状态。
     */
    private String compileStatus;

    /**
     * 编译错误信息。
     */
    private String compileError;

    /**
     * 部署完成时间。
     */
    private Instant deployedAt;

    /**
     * 流程版本状态。
     */
    private String status;

    /**
     * 发布人。
     */
    private String publishedBy;

    /**
     * 发布时间。
     */
    private Instant publishedAt;
}
