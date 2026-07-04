package top.kx.heartbeat.application.flow.runtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 流程部署结果。
 *
 * <p>用于承接基础设施层部署到生产运行时后的引擎标识。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowDeploymentResult {

    /**
     * 运行时引擎编码。
     */
    private String runtimeEngine;

    /**
     * 部署标识。
     */
    private String deploymentId;

    /**
     * 流程定义标识。
     */
    private String processDefinitionId;

    /**
     * 流程定义键。
     */
    private String processDefinitionKey;

    /**
     * 部署完成时间。
     */
    private Instant deployedAt;
}
