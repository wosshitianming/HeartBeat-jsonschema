package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.flow.runtime.FlowBpmnCompileResult;
import top.kx.heartbeat.application.flow.runtime.FlowDeploymentResult;
import top.kx.heartbeat.domain.flow.model.FlowRuntimeEngine;
import top.kx.heartbeat.domain.flow.model.FlowVersion;

import javax.annotation.Resource;
import java.time.Instant;

/**
 * Flowable 流程部署服务。
 *
 * <p>负责把应用层编译出的 BPMN XML 部署为 Flowable 流程定义。</p>
 */
@Service
public class FlowableDeploymentService {

    /**
     * Flowable 仓储服务。
     */
    @Resource
    private RepositoryService repositoryService;

    /**
     * Flowable 租户解析器。
     */
    @Resource
    private FlowableTenantResolver tenantResolver;

    /**
     * 部署 BPMN XML。
     *
     * @param version 流程版本
     * @param compileResult BPMN 编译结果
     * @return 部署结果
     */
    public FlowDeploymentResult deploy(FlowVersion version, FlowBpmnCompileResult compileResult) {
        // 判断编译结果是否有效。
        if (compileResult == null || !compileResult.isValid()) {
            // 抛出编译结果无效异常。
            throw new IllegalArgumentException("BPMN 编译结果无效，禁止部署");
        }
        // 解析租户标识。
        String tenantId = tenantResolver.resolveTenantId(version);
        // 部署 BPMN 资源。
        Deployment deployment = repositoryService.createDeployment()
                .tenantId(tenantId)
                .name(compileResult.getProcessName())
                .addString(compileResult.getResourceName(), compileResult.getBpmnXml())
                .deploy();
        // 查询本次部署产生的流程定义。
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        // 创建部署结果。
        FlowDeploymentResult result = new FlowDeploymentResult();
        // 写入运行时引擎。
        result.setRuntimeEngine(FlowRuntimeEngine.FLOWABLE.getCode());
        // 写入部署标识。
        result.setDeploymentId(deployment.getId());
        // 写入流程定义标识。
        result.setProcessDefinitionId(definition == null ? null : definition.getId());
        // 写入流程定义键。
        result.setProcessDefinitionKey(definition == null ? compileResult.getProcessDefinitionKey() : definition.getKey());
        // 写入部署时间。
        result.setDeployedAt(Instant.now());
        // 返回部署结果。
        return result;
    }
}
