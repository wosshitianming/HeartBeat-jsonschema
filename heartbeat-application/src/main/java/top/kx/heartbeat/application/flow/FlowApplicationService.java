package top.kx.heartbeat.application.flow;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.flow.runtime.*;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.domain.flow.model.*;
import top.kx.heartbeat.domain.flow.repository.FlowRepository;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;
import top.kx.heartbeat.domain.flow.validation.FlowDslValidator;
import top.kx.heartbeat.domain.flow.validation.FlowValidationIssue;
import top.kx.heartbeat.domain.flow.validation.FlowValidationResult;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 流程应用服务。
 *
 * <p>负责流程定义草稿、编译、发布、调试和运行记录查询的用例编排。</p>
 */
@Service
public class FlowApplicationService {

    /**
     * 流程定义仓储。
     */
    @Resource
    private FlowRepository flowRepository;

    @Resource
    private CurrentUserProvider currentUserProvider;

    /**
     * 节点组件目录。
     */
    @Resource
    private NodeComponentRegistryService componentRegistryService;

    /**
     * 流程调试执行器。
     */
    @Resource
    private FlowExecutor flowExecutor;

    /**
     * Flow DSL BPMN 编译器。
     */
    @Resource
    private FlowBpmnCompiler flowBpmnCompiler;

    /**
     * 流程运行时门面。
     */
    @Resource
    private FlowRuntimeFacade flowRuntimeFacade;

    /**
     * 流程运行记录仓储。
     */
    @Resource
    private FlowRunRepository flowRunRepository;

    @Resource
    private FlowRunIdGenerator flowRunIdGenerator;

    @Resource
    private FlowRunLaunchService flowRunLaunchService;

    /**
     * 流程 DSL 校验领域服务。
     */
    @Resource
    private FlowDslValidator validator;

    /**
     * 查询流程定义列表。
     *
     * @return 流程定义列表
     */
    public List<FlowDefinition> list() {
        // 查询全部流程定义。
        return flowRepository.findAll();
    }

    /**
     * 查询流程定义详情。
     *
     * @param id 流程定义标识
     * @return 流程定义详情
     */
    public FlowDefinition get(String id) {
        // 按标识查询流程定义。
        return flowRepository.findById(id)
                // 流程不存在时抛出业务异常。
                .orElseThrow(() -> new IllegalArgumentException("流程不存在: " + id));
    }

    /**
     * 保存流程定义草稿。
     *
     * @param draft 流程定义草稿
     * @return 保存后的流程定义
     */
    @Transactional
    public FlowDefinition saveDraft(FlowDefinition draft) {
        if (draft == null) throw new IllegalArgumentException("流程草稿不能为空");
        // 获取当前时间。
        Instant now = Instant.now();
        FlowDefinition existing = StringUtils.isBlank(draft.getId())
                ? null : flowRepository.findById(draft.getId()).orElse(null);
        // 判断是否需要初始化流程标识。
        if (existing == null) {
            // 写入流程创建时间。
            draft.setCreateTime(now);
        } else {
            draft.setCreateTime(existing.getCreateTime());
            draft.setActiveVersionNo(existing.getActiveVersionNo());
            draft.setRuntimeEngine(existing.getRuntimeEngine());
            draft.setActiveProcessDefinitionId(existing.getActiveProcessDefinitionId());
            draft.setActiveDeploymentId(existing.getActiveDeploymentId());
        }
        // 判断是否需要初始化流程编码。
        if (StringUtils.isBlank(draft.getCode())) {
            // 生成默认流程编码。
            draft.setCode("flow_" + UUID.randomUUID().toString().replace("-", ""));
        }
        // 根据是否已有激活版本设置流程定义状态。
        if (existing != null && (FlowDefinitionStatus.ONLINE.getCode().equals(existing.getStatus())
                || FlowDefinitionStatus.OFFLINE.getCode().equals(existing.getStatus()))) {
            draft.setStatus(existing.getStatus());
        } else {
            draft.setStatus(resolveDraftStatus(draft).getCode());
        }
        // 写入流程更新时间。
        draft.setUpdateTime(now);
        // 保存流程定义草稿。
        return flowRepository.saveDraft(draft);
    }

    /**
     * 编译流程定义。
     *
     * @param flow 流程定义草稿
     * @return 流程编译报告
     */
    public RecordResponse compile(FlowDefinition flow) {
        // 查询全部启用节点组件。
        List<NodeComponentManifest> manifests = componentRegistryService.listActive();
        // 编译流程定义。
        FlowBpmnCompileResult result = flowBpmnCompiler.compile(flow, manifests);
        // 兜底空流程对象，避免统计节点数量时空指针。
        FlowDefinition safeFlow = flow == null ? new FlowDefinition() : flow;
        // 创建编译报告。
        Map<String, Object> report = new LinkedHashMap<>();
        // 写入是否校验通过。
        report.put("valid", result.isValid());
        // 写入校验问题列表。
        report.put("issues", result.getIssues());
        // 写入流程定义键。
        report.put("processDefinitionKey", result.getProcessDefinitionKey());
        // 写入 BPMN 摘要。
        report.put("bpmnSha256", result.getBpmnSha256());
        // 写入 BPMN XML。
        report.put("bpmnXml", result.getBpmnXml());
        // 写入节点数量。
        report.put("nodeCount", safeFlow.getNodes() == null ? 0 : safeFlow.getNodes().size());
        // 写入连线数量。
        report.put("edgeCount", safeFlow.getEdges() == null ? 0 : safeFlow.getEdges().size());
        // 返回流程编译报告。
        return RecordResponse.from(report);
    }

    /**
     * 发布流程定义版本。
     *
     * @param flowId 流程定义标识
     * @return 发布后的流程版本
     */
    @Transactional
    public FlowVersion publish(String flowId) {
        // 查询流程定义。
        FlowDefinition flow = get(flowId);
        // 查询全部启用节点组件。
        List<NodeComponentManifest> manifests = componentRegistryService.listActive();
        // 编译 Flow DSL 到 BPMN。
        FlowBpmnCompileResult compileResult = flowBpmnCompiler.compile(flow, manifests);
        // 判断流程是否通过编译。
        if (!compileResult.isValid()) {
            // 读取第一条编译问题。
            FlowValidationIssue issue = compileResult.getIssues().get(0);
            // 抛出流程编译失败异常。
            throw new IllegalArgumentException("流程编译失败: " + issue.getMessage());
        }
        // 计算下一个流程版本号。
        int nextVersionNo = flowRepository.findVersions(flowId).stream()
                // 提取流程版本号。
                .mapToInt(FlowVersion::getVersionNo)
                // 查询当前最大版本号。
                .max()
                // 无版本时从零开始。
                .orElse(0) + 1;
        // 创建流程版本对象。
        FlowVersion version = new FlowVersion();
        // 写入流程定义标识。
        version.setFlowId(flowId);
        // 写入流程版本号。
        version.setVersionNo(nextVersionNo);
        // 写入流程 DSL 快照。
        version.setFlowDsl(flow);
        // 写入编译报告摘要。
        version.setCompileReport("{\"valid\":true,\"issueCount\":0}");
        // 写入流程版本状态。
        version.setStatus(FlowVersionStatus.PUBLISHED.getCode());
        // 写入运行时引擎。
        version.setRuntimeEngine(flowRuntimeFacade.productionEngine().getCode());
        // 写入 BPMN XML。
        version.setBpmnXml(compileResult.getBpmnXml());
        // 写入 BPMN 摘要。
        version.setBpmnSha256(compileResult.getBpmnSha256());
        // 写入流程定义键。
        version.setProcessDefinitionKey(compileResult.getProcessDefinitionKey());
        // 写入编译状态。
        version.setCompileStatus("COMPILED");
        // 写入发布人。
        version.setPublishedBy(currentUserProvider.currentUserId());
        // 写入发布时间。
        version.setPublishedAt(Instant.now());
        // 保存流程版本。
        FlowVersion saved = flowRepository.saveVersion(version);
        // 补齐保存后版本的运行时字段。
        copyRuntimeFields(version, saved);
        // 部署已发布版本。
        FlowVersion deployed = flowRuntimeFacade.deployPublishedVersion(saved, compileResult);
        // 标记部署成功。
        deployed.setCompileStatus("DEPLOYED");
        // 更新流程版本运行时部署信息。
        flowRepository.updateVersionRuntime(deployed);
        // 激活新发布版本。
        flowRepository.activateVersion(flowId, nextVersionNo);
        // 更新流程定义激活部署信息。
        flowRepository.updateActiveRuntimeDeployment(flowId, deployed.getRuntimeEngine(), deployed.getDeploymentId(), deployed.getProcessDefinitionId());
        // 返回部署后的流程版本。
        return deployed;
    }

    /**
     * 查询流程版本列表。
     *
     * @param flowId 流程定义标识
     * @return 流程版本列表
     */
    public List<FlowVersion> versions(String flowId) {
        // 查询流程版本列表。
        return flowRepository.findVersions(flowId);
    }

    /**
     * 激活流程指定版本。
     *
     * @param flowId 流程定义标识
     * @param versionNo 流程版本号
     * @return 激活后的流程定义
     */
    @Transactional
    public FlowDefinition activate(String flowId, int versionNo) {
        // 校验指定版本是否存在。
        FlowVersion version = flowRepository.findVersion(flowId, versionNo)
                // 版本不存在时抛出业务异常。
                .orElseThrow(() -> new IllegalArgumentException("流程版本不存在: v" + versionNo));
        if (!"DEPLOYED".equals(version.getCompileStatus())
                || (requiresFlowableDeployment(version)
                && (StringUtils.isBlank(version.getDeploymentId())
                || StringUtils.isBlank(version.getProcessDefinitionId())))) {
            throw new IllegalStateException("流程版本尚未完成生产部署: v" + versionNo);
        }
        // 激活指定流程版本。
        flowRepository.activateVersion(flowId, versionNo);
        flowRepository.updateActiveRuntimeDeployment(flowId, version.getRuntimeEngine(),
                version.getDeploymentId(), version.getProcessDefinitionId());
        // 返回最新流程定义详情。
        return get(flowId);
    }

    @Transactional
    public FlowDefinition deactivate(String flowId) {
        get(flowId);
        flowRepository.deactivate(flowId);
        return get(flowId);
    }

    /**
     * 调试流程定义。
     *
     * @param flowId 流程定义标识
     * @param input 调试输入参数
     * @return 流程调试结果
     */
    public FlowDebugResult debug(String flowId, Map<String, Object> input) {
        // 查询流程定义。
        FlowDefinition flow = get(flowId);
        // 执行流程调试。
        return flowExecutor.debug(flow, input);
    }

    /**
     * 启动生产态流程运行。
     *
     * @param flowId 流程定义标识
     * @param input 运行输入参数
     * @return 流程运行记录
     */
    public FlowRun run(String flowId, Map<String, Object> input) {
        // 查询流程定义。
        FlowDefinition flow = get(flowId);
        if (!FlowDefinitionStatus.ONLINE.getCode().equals(flow.getStatus())) {
            throw new IllegalStateException("流程未启用，不能执行生产运行");
        }
        if (flow.getActiveVersionNo() == null) throw new IllegalStateException("流程尚未发布生产版本");
        FlowVersion version = flowRepository.findVersion(flowId, flow.getActiveVersionNo())
                .orElseThrow(() -> new IllegalStateException("当前激活流程版本不存在"));
        if (!"DEPLOYED".equals(version.getCompileStatus())
                || (requiresFlowableDeployment(version) && StringUtils.isBlank(version.getProcessDefinitionId()))) {
            throw new IllegalStateException("当前激活流程版本尚未完成生产部署");
        }
        // 构建流程启动命令。
        FlowStartCommand command = new FlowStartCommand();
        // 写入租户标识。
        command.setTenantId(currentUserProvider.currentTenantId());
        // 写入流程定义标识。
        command.setFlowId(flowId);
        // 写入 HeartBeat 运行标识。
        command.setRunId(flowRunIdGenerator.nextId());
        // 写入流程版本号。
        command.setVersionNo(version.getVersionNo());
        command.setFlowVersionId(version.getId());
        // 写入流程定义快照。
        command.setFlowDefinition(version.getFlowDsl() == null ? flow : version.getFlowDsl());
        // 写入运行时流程定义标识。
        command.setProcessDefinitionId(version.getProcessDefinitionId());
        // 写入运行时流程定义键。
        command.setProcessDefinitionKey(StringUtils.defaultIfBlank(version.getProcessDefinitionKey(), flow.getCode()));
        // 写入触发类型。
        command.setTriggerType(FlowTriggerType.MANUAL);
        // 写入幂等键。
        command.setIdempotencyKey(UUID.randomUUID().toString());
        // 写入业务键。
        command.setBusinessKey("flow:" + flowId + ":" + command.getIdempotencyKey());
        // 写入运行输入载荷。
        command.setPayload(input == null ? new LinkedHashMap<>() : input);
        return flowRunLaunchService.start(command, flowRunLaunchService.createPending(command));
    }

    /**
     * 查询流程运行详情。
     *
     * @param runId 流程运行标识
     * @return 流程运行详情
     */
    public FlowRun runDetail(String runId) {
        // 查询流程运行详情。
        return flowRunRepository.findRun(runId)
                // 运行不存在时抛出业务异常。
                .orElseThrow(() -> new IllegalArgumentException("流程运行不存在: " + runId));
    }

    private boolean requiresFlowableDeployment(FlowVersion version) {
        String engine = StringUtils.defaultIfBlank(
                version == null ? null : version.getRuntimeEngine(),
                flowRuntimeFacade.productionEngine().getCode());
        return FlowRuntimeEngine.FLOWABLE.getCode().equals(engine);
    }

    /**
     * 查询流程运行回放事件。
     *
     * @param runId 流程运行标识
     * @return 流程运行回放事件
     */
    public List<FlowRunEvent> replay(String runId) {
        // 当前回放先复用事件序列查询。
        return flowRunRepository.findEvents(runId);
    }

    /**
     * 取消流程运行。
     *
     * @param runId 流程运行标识
     * @param reason 取消原因
     */
    public void cancel(String runId, String reason) {
        // 查询流程运行详情。
        FlowRun run = runDetail(runId);
        if (!FlowRunStatus.isCancelable(run.getStatus())) {
            throw new IllegalStateException("当前流程运行状态不允许取消: " + run.getStatus());
        }
        // 取消生产态流程。
        flowRuntimeFacade.cancel(run, reason);
        FlowRun projected = flowRunRepository.findRun(runId).orElse(run);
        if (FlowRunStatus.isCancelable(projected.getStatus())) {
            Instant finishedAt = Instant.now();
            projected.setStatus(FlowRunStatus.CANCELED.getCode());
            projected.setErrorMessage(reason);
            projected.setFinishedAt(finishedAt);
            if (projected.getStartedAt() != null) {
                projected.setElapsedMs(Duration.between(projected.getStartedAt(), finishedAt).toMillis());
            }
            flowRunRepository.saveRun(projected);
        }
    }

    /**
     * 查询流程运行记录列表。
     *
     * @param flowId 流程定义标识
     * @return 流程运行记录列表
     */
    public List<FlowRun> runs(String flowId) {
        // 查询流程运行记录列表。
        return flowRunRepository.findRunsByFlowId(flowId);
    }

    /**
     * 查询流程运行事件列表。
     *
     * @param runId 流程运行标识
     * @return 流程运行事件列表
     */
    public List<FlowRunEvent> runEvents(String runId) {
        // 查询流程运行事件列表。
        return flowRunRepository.findEvents(runId);
    }

    /**
     * 校验流程定义。
     *
     * @param flow 流程定义
     * @return 流程校验结果
     */
    private FlowValidationResult validate(FlowDefinition flow) {
        // 查询全部启用节点组件。
        List<NodeComponentManifest> manifests = componentRegistryService.listActive();
        // 使用流程 DSL 校验领域服务执行校验。
        return validator.validate(flow, manifests);
    }

    /**
     * 复制运行时字段到保存后的流程版本。
     *
     * @param source 源流程版本
     * @param target 目标流程版本
     */
    private void copyRuntimeFields(FlowVersion source, FlowVersion target) {
        // 写入运行时引擎。
        target.setRuntimeEngine(source.getRuntimeEngine());
        // 写入 BPMN XML。
        target.setBpmnXml(source.getBpmnXml());
        // 写入 BPMN 摘要。
        target.setBpmnSha256(source.getBpmnSha256());
        // 写入流程定义键。
        target.setProcessDefinitionKey(source.getProcessDefinitionKey());
        // 写入编译状态。
        target.setCompileStatus(source.getCompileStatus());
    }

    /**
     * 解析草稿保存后的流程定义状态。
     *
     * @param draft 流程定义草稿
     * @return 流程定义状态
     */
    private FlowDefinitionStatus resolveDraftStatus(FlowDefinition draft) {
        // 根据激活版本判断是否为纯草稿。
        return draft.getActiveVersionNo() == null ? FlowDefinitionStatus.DRAFT : FlowDefinitionStatus.SAVED;
    }
}
