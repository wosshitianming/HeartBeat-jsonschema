package top.kx.heartbeat.application.flow.runtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.domain.flow.model.*;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.LinkedHashMap;

/**
 * 流程运行时门面。
 *
 * <p>用于让应用服务按运行时策略选择本地调试执行器或生产态运行时端口。</p>
 */
@Service
public class FlowRuntimeFacade {

    /**
     * 生产态运行时引擎配置。
     */
    @Value("${heartbeat.flow.runtime.engine:FLOWABLE}")
    private String runtimeEngine;

    /**
     * 调试运行时引擎配置。
     */
    @Value("${heartbeat.flow.runtime.debug-engine:LOCAL_DEBUG}")
    private String debugEngine;

    /**
     * 本地调试执行器。
     */
    @Resource
    private FlowExecutor flowExecutor;

    /**
     * 生产态运行时端口。
     */
    @Resource
    private FlowProductionRuntimePort productionRuntimePort;

    /**
     * 部署已发布流程版本。
     *
     * @param version 流程版本
     * @param compileResult BPMN 编译结果
     * @return 补齐部署信息后的流程版本
     */
    public FlowVersion deployPublishedVersion(FlowVersion version, FlowBpmnCompileResult compileResult) {
        // 解析生产态运行时引擎。
        FlowRuntimeEngine engine = FlowRuntimeEngine.fromCode(runtimeEngine);
        // 判断是否使用 Flowable 引擎。
        if (!FlowRuntimeEngine.FLOWABLE.equals(engine)) {
            // 本地运行时无需部署 BPMN，但仍需留下可激活的运行时元数据。
            version.setRuntimeEngine(engine.getCode());
            version.setProcessDefinitionKey(compileResult.getProcessDefinitionKey());
            version.setDeployedAt(Instant.now());
            return version;
        }
        // 获取生产态运行时端口。
        FlowProductionRuntimePort runtimePort = requiredProductionRuntimePort();
        // 部署 BPMN 到生产态运行时。
        FlowDeploymentResult deployment = runtimePort.deploy(version, compileResult);
        // 写入运行时引擎。
        version.setRuntimeEngine(deployment.getRuntimeEngine());
        // 写入部署标识。
        version.setDeploymentId(deployment.getDeploymentId());
        // 写入流程定义标识。
        version.setProcessDefinitionId(deployment.getProcessDefinitionId());
        // 写入流程定义键。
        version.setProcessDefinitionKey(deployment.getProcessDefinitionKey());
        // 写入部署时间。
        version.setDeployedAt(deployment.getDeployedAt());
        // 返回更新后的流程版本。
        return version;
    }

    /**
     * 启动生产态流程运行。
     *
     * @param command 流程启动命令
     * @return 流程运行记录
     */
    public FlowRun start(FlowStartCommand command) {
        // 解析生产态运行时引擎。
        FlowRuntimeEngine engine = FlowRuntimeEngine.fromCode(runtimeEngine);
        // 判断是否使用本地调试引擎。
        if (FlowRuntimeEngine.LOCAL_DEBUG.equals(engine)) {
            // 使用本地调试执行器启动流程。
            return startWithLocalDebug(command);
        }
        // 使用生产态运行时端口启动流程。
        return requiredProductionRuntimePort().start(command);
    }

    public FlowRuntimeEngine productionEngine() {
        return FlowRuntimeEngine.fromCode(runtimeEngine);
    }

    /**
     * 恢复等待中的流程运行。
     *
     * @param command 流程恢复命令
     */
    public void resume(FlowResumeCommand command) {
        // 委托生产态运行时端口恢复流程。
        requiredProductionRuntimePort().resume(command);
    }

    /**
     * 取消流程运行。
     *
     * @param run 流程运行记录
     * @param reason 取消原因
     */
    public void cancel(FlowRun run, String reason) {
        // 委托生产态运行时端口取消流程。
        requiredProductionRuntimePort().cancel(run.getId(), run.getEngineInstanceId(), reason);
    }

    /**
     * 读取调试运行时引擎。
     *
     * @return 调试运行时引擎
     */
    public FlowRuntimeEngine debugEngine() {
        // 返回调试运行时引擎枚举。
        return FlowRuntimeEngine.fromCode(debugEngine);
    }

    /**
     * 使用本地调试执行器启动流程。
     *
     * @param command 流程启动命令
     * @return 流程运行记录
     */
    private FlowRun startWithLocalDebug(FlowStartCommand command) {
        // 判断流程定义快照是否存在。
        if (command.getFlowDefinition() == null) {
            // 抛出流程定义缺失异常。
            throw new IllegalArgumentException("本地调试运行需要流程定义快照");
        }
        // 执行本地调试。
        FlowDebugResult debugResult = flowExecutor.debug(command.getFlowDefinition(), command.getPayload());
        // 构建运行记录。
        FlowRun run = new FlowRun();
        // 写入运行标识。
        run.setId(debugResult.getRunId());
        // 写入流程定义标识。
        run.setFlowId(command.getFlowId());
        // 写入版本号。
        run.setVersionNo(command.getVersionNo() == null ? 0 : command.getVersionNo());
        // 写入运行时引擎。
        run.setEngine(FlowRuntimeEngine.LOCAL_DEBUG.getCode());
        // 写入触发类型。
        run.setTriggerType(command.getTriggerType().getCode());
        // 写入幂等键。
        run.setIdempotencyKey(command.getIdempotencyKey());
        // 写入幂等范围。
        run.setIdempotencyScope(FlowIdempotencyScope.START.getCode());
        // 写入业务键。
        run.setBusinessKey(command.getBusinessKey());
        // 写入关联键。
        run.setCorrelationKey(command.getCorrelationKey());
        // 写入运行状态。
        run.setStatus(FlowRunStatus.SUCCESS.getCode());
        // 写入输入摘要。
        run.setInputSummary(command.getPayload() == null ? new LinkedHashMap<>() : command.getPayload());
        // 写入输出摘要。
        run.setOutputSummary(debugResult.getOutput());
        // 写入结束时间。
        run.setFinishedAt(Instant.now());
        // 返回运行记录。
        return run;
    }

    /**
     * 获取必需的生产态运行时端口。
     *
     * @return 生产态运行时端口
     */
    private FlowProductionRuntimePort requiredProductionRuntimePort() {
        // 判断生产态运行时端口是否存在。
        if (productionRuntimePort == null) {
            // 抛出生产运行时缺失异常。
            throw new IllegalStateException("未找到生产态流程运行时适配器");
        }
        // 返回生产态运行时端口。
        return productionRuntimePort;
    }
}
