package top.kx.heartbeat.infrastructure.flow.flowable;

import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.flow.runtime.FlowBpmnCompileResult;
import top.kx.heartbeat.application.flow.runtime.FlowDeploymentResult;
import top.kx.heartbeat.application.flow.runtime.FlowProductionRuntimePort;
import top.kx.heartbeat.application.flow.runtime.FlowResumeCommand;
import top.kx.heartbeat.application.flow.runtime.FlowStartCommand;
import top.kx.heartbeat.domain.flow.model.FlowRun;
import top.kx.heartbeat.domain.flow.model.FlowVersion;

import javax.annotation.Resource;

/**
 * Flowable 运行时适配器。
 *
 * <p>用于把应用层生产运行时端口适配到 Flowable 部署、启动、恢复和取消能力。</p>
 */
@Service
public class FlowableRuntimeAdapter implements FlowProductionRuntimePort {

    /**
     * Flowable 部署服务。
     */
    @Resource
    private FlowableDeploymentService deploymentService;

    /**
     * Flowable 运行服务。
     */
    @Resource
    private FlowableRuntimeService runtimeService;

    /**
     * 部署已编译流程版本。
     *
     * @param version 流程版本
     * @param compileResult BPMN 编译结果
     * @return Flowable 部署结果
     */
    @Override
    public FlowDeploymentResult deploy(FlowVersion version, FlowBpmnCompileResult compileResult) {
        // 委托 Flowable 部署服务执行部署。
        return deploymentService.deploy(version, compileResult);
    }

    /**
     * 启动 Flowable 流程实例。
     *
     * @param command 流程启动命令
     * @return 流程运行记录
     */
    @Override
    public FlowRun start(FlowStartCommand command) {
        // 委托 Flowable 运行服务启动流程。
        return runtimeService.start(command);
    }

    /**
     * 恢复 Flowable 等待实例。
     *
     * @param command 流程恢复命令
     */
    @Override
    public void resume(FlowResumeCommand command) {
        // 委托 Flowable 运行服务恢复流程。
        runtimeService.resume(command);
    }

    /**
     * 取消 Flowable 流程实例。
     *
     * @param runId 流程运行标识
     * @param engineInstanceId 引擎流程实例标识
     * @param reason 取消原因
     */
    @Override
    public void cancel(String runId, String engineInstanceId, String reason) {
        // 委托 Flowable 运行服务取消流程。
        runtimeService.cancel(runId, engineInstanceId, reason);
    }
}
