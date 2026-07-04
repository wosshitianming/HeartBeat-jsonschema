package top.kx.heartbeat.application.flow.runtime;

import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowVersion;

/**
 * 生产态流程运行时端口。
 *
 * <p>用于让应用层只依赖抽象能力，由基础设施层适配 Flowable 等具体引擎。</p>
 */
public interface FlowProductionRuntimePort {

    /**
     * 部署已编译流程版本。
     *
     * @param version 流程版本
     * @param compileResult BPMN 编译结果
     * @return 流程部署结果
     */
    FlowDeploymentResult deploy(FlowVersion version, FlowBpmnCompileResult compileResult);

    /**
     * 启动流程实例。
     *
     * @param command 流程启动命令
     * @return 流程运行记录
     */
    FlowRun start(FlowStartCommand command);

    /**
     * 恢复等待中的流程实例。
     *
     * @param command 流程恢复命令
     */
    void resume(FlowResumeCommand command);

    /**
     * 取消流程实例。
     *
     * @param runId 流程运行标识
     * @param engineInstanceId 引擎流程实例标识
     * @param reason 取消原因
     */
    void cancel(String runId, String engineInstanceId, String reason);
}
