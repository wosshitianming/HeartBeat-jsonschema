package top.kx.heartbeat.application.flow;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.common.vo.PageResultVO;
import top.kx.heartbeat.application.flow.param.FlowRunQueryParam;
import top.kx.heartbeat.application.flow.param.FlowRunSummaryParam;
import top.kx.heartbeat.application.flow.request.FlowRetryRequest;
import top.kx.heartbeat.application.flow.runtime.FlowPayloadSummaryService;
import top.kx.heartbeat.application.flow.runtime.FlowRunIdGenerator;
import top.kx.heartbeat.application.flow.runtime.FlowRunLaunchService;
import top.kx.heartbeat.application.flow.runtime.FlowStartCommand;
import top.kx.heartbeat.application.flow.vo.FlowOperationsSummaryVO;
import top.kx.heartbeat.application.flow.vo.FlowRunListItemVO;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.domain.flow.model.*;
import top.kx.heartbeat.domain.flow.repository.FlowRepository;
import top.kx.heartbeat.domain.flow.repository.FlowRunRepository;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlowRunOperationsService {
    private static final Set<String> RUN_STATUSES = Arrays.stream(FlowRunStatus.values())
            .map(FlowRunStatus::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
    private static final Set<String> TRIGGER_TYPES = Arrays.stream(FlowTriggerType.values())
            .map(FlowTriggerType::getCode).collect(Collectors.toCollection(LinkedHashSet::new));

    @Resource
    private FlowRunRepository flowRunRepository;
    @Resource
    private FlowRepository flowRepository;
    @Resource
    private FlowRunLaunchService flowRunLaunchService;
    @Resource
    private FlowRunIdGenerator runIdGenerator;
    @Resource
    private CurrentUserProvider currentUserProvider;

    public PageResultVO<FlowRunListItemVO> page(FlowRunQueryParam param) {
        FlowRunQuery query = normalizeQuery(param);
        FlowRunRepository.Page<FlowRun> page = flowRunRepository.pageByQuery(query);
        Map<String, String> names = flowRepository.findAll().stream()
                .collect(Collectors.toMap(FlowDefinition::getId, FlowDefinition::getName, (a, b) -> a));
        List<FlowRunListItemVO> records = page.getRecords().stream()
                .map(run -> toListItem(run, names.get(run.getFlowId())))
                .collect(Collectors.toList());
        return PageResultVO.of(page.getPageNum(), page.getPageSize(), page.getTotal(), records);
    }

    public FlowOperationsSummaryVO summary(FlowRunSummaryParam param) {
        FlowRunSummaryParam safe = param == null ? new FlowRunSummaryParam() : param;
        Instant before = safe.getStartedBefore() == null ? Instant.now() : safe.getStartedBefore();
        Instant after = safe.getStartedAfter() == null ? before.minus(Duration.ofHours(24)) : safe.getStartedAfter();
        if (after.isAfter(before)) throw new IllegalArgumentException("运行统计开始时间不能晚于结束时间");
        List<FlowDefinition> definitions = StringUtils.isBlank(safe.getFlowId())
                ? flowRepository.findAll()
                : Collections.singletonList(flowRepository.findById(safe.getFlowId())
                .orElseThrow(() -> new IllegalArgumentException("流程不存在: " + safe.getFlowId())));
        FlowRunStatistics stats = flowRunRepository.summarize(safe.getFlowId(), after, before);
        FlowOperationsSummaryVO result = new FlowOperationsSummaryVO();
        result.setTotalFlows(definitions.size());
        result.setActiveFlows(definitions.stream().filter(v -> FlowDefinitionStatus.ONLINE.getCode().equals(v.getStatus())).count());
        result.setPublishedFlows(definitions.stream().filter(v -> v.getActiveVersionNo() != null).count());
        result.setDraftFlows(definitions.stream().filter(v -> v.getActiveVersionNo() == null).count());
        result.setTotalRuns(stats.getTotalRuns());
        result.setRunningRuns(stats.getRunningRuns());
        result.setWaitingRuns(stats.getWaitingRuns());
        result.setSuccessRuns(stats.getSuccessRuns());
        result.setFailedRuns(stats.getFailedRuns());
        result.setCanceledRuns(stats.getCanceledRuns());
        result.setAverageDurationMs(stats.getAverageDurationMs());
        long completed = stats.getSuccessRuns() + stats.getFailedRuns();
        result.setSuccessRate(completed == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(stats.getSuccessRuns() * 100.0D / completed).setScale(2, RoundingMode.HALF_UP));
        result.setStartedAfter(after);
        result.setStartedBefore(before);
        return result;
    }

    public FlowRun retry(String runId, FlowRetryRequest request) {
        FlowRun original = flowRunRepository.findRun(runId)
                .orElseThrow(() -> new IllegalArgumentException("流程运行不存在: " + runId));
        if (!FlowRunStatus.isRetryable(original.getStatus())) {
            throw new IllegalStateException("只有失败或超时的流程运行可以重试");
        }
        FlowDefinition flow = flowRepository.findById(original.getFlowId())
                .orElseThrow(() -> new IllegalArgumentException("流程不存在: " + original.getFlowId()));
        FlowVersion version = flowRepository.findVersion(original.getFlowId(), original.getVersionNo())
                .orElseThrow(() -> new IllegalArgumentException("流程版本不存在: v" + original.getVersionNo()));
        Map<String, Object> variables = retryPayload(original, request);
        String retryId = runIdGenerator.nextId();
        String idempotencyKey = UUID.randomUUID().toString();
        FlowStartCommand command = new FlowStartCommand();
        command.setTenantId(currentUserProvider.currentTenantId());
        command.setFlowId(flow.getId());
        command.setRunId(retryId);
        command.setVersionNo(version.getVersionNo());
        command.setFlowVersionId(version.getId());
        command.setFlowDefinition(version.getFlowDsl() == null ? flow : version.getFlowDsl());
        command.setProcessDefinitionId(version.getProcessDefinitionId());
        command.setProcessDefinitionKey(StringUtils.defaultIfBlank(version.getProcessDefinitionKey(), flow.getCode()));
        command.setTriggerType(FlowTriggerType.RETRY);
        command.setIdempotencyKey(idempotencyKey);
        command.setBusinessKey("flow:" + flow.getId() + ":retry:" + runId + ":" + idempotencyKey);
        command.setPayload(variables);
        FlowRun pending = flowRunLaunchService.createPending(command);
        pending.setIdempotencyScope(FlowIdempotencyScope.USER_RETRY.getCode());
        pending.setRetryFromRunId(original.getId());
        pending.setParentRunId(original.getId());
        pending.setRootRunId(StringUtils.defaultIfBlank(original.getRootRunId(), original.getId()));
        pending.setRetryNo((original.getRetryNo() == null ? 0 : original.getRetryNo()) + 1);
        pending.setRetryReason(request == null || StringUtils.isBlank(request.getReason())
                ? "运维中心重试失败执行" : request.getReason().trim());
        return flowRunLaunchService.start(command, pending);
    }

    private Map<String, Object> retryPayload(FlowRun original, FlowRetryRequest request) {
        if (request != null && request.getVariables() != null) {
            return new LinkedHashMap<>(request.getVariables());
        }
        Map<String, Object> summary = original.getInputSummary() == null
                ? Collections.emptyMap() : original.getInputSummary();
        boolean summarized = FlowPayloadSummaryService.SUMMARY_FORMAT.equals(summary.get("payloadSummary"))
                || "HB_FLOW_PAYLOAD_AES_GCM_V1".equals(summary.get("payloadStorage"));
        if (summarized) {
            throw new IllegalArgumentException("原运行输入仅保留了大 payload 摘要，重试时必须显式提供 variables");
        }
        return new LinkedHashMap<>(summary);
    }

    private FlowRunQuery normalizeQuery(FlowRunQueryParam param) {
        FlowRunQueryParam safe = param == null ? new FlowRunQueryParam() : param;
        FlowRunQuery query = new FlowRunQuery();
        query.setPageNum(Math.max(1, safe.getPageNum() == null ? 1 : safe.getPageNum()));
        query.setPageSize(Math.max(1, Math.min(100, safe.getPageSize() == null ? 20 : safe.getPageSize())));
        query.setFlowId(StringUtils.trimToNull(safe.getFlowId()));
        query.setStatuses(normalizeCodes(safe.getStatuses(), RUN_STATUSES, "运行状态"));
        query.setTriggerTypes(normalizeCodes(safe.getTriggerTypes(), TRIGGER_TYPES, "触发类型"));
        query.setStartedAfter(safe.getStartedAfter());
        query.setStartedBefore(safe.getStartedBefore());
        if (query.getStartedAfter() != null && query.getStartedBefore() != null
                && query.getStartedAfter().isAfter(query.getStartedBefore())) {
            throw new IllegalArgumentException("运行记录开始时间不能晚于结束时间");
        }
        query.setOrderByColumn(safe.getOrderByColumn());
        query.setOrderByDirection(safe.getOrderByDirection());
        return query;
    }

    private List<String> normalizeCodes(List<String> values, Set<String> supported, String label) {
        if (values == null || values.isEmpty()) return new ArrayList<>();
        List<String> result = values.stream().filter(StringUtils::isNotBlank)
                .map(v -> v.trim().toUpperCase()).distinct().collect(Collectors.toList());
        List<String> invalid = result.stream().filter(v -> !supported.contains(v)).collect(Collectors.toList());
        if (!invalid.isEmpty()) throw new IllegalArgumentException(label + "不支持: " + String.join(", ", invalid));
        return result;
    }

    private FlowRunListItemVO toListItem(FlowRun run, String flowName) {
        FlowRunListItemVO item = new FlowRunListItemVO();
        item.setId(run.getId());
        item.setFlowId(run.getFlowId());
        item.setFlowName(flowName);
        item.setVersionNo(run.getVersionNo());
        item.setRunNo(run.getRunNo());
        item.setEngine(run.getEngine());
        item.setTriggerType(run.getTriggerType());
        item.setStatus(run.getStatus());
        item.setRetryFromRunId(run.getRetryFromRunId());
        item.setRetryNo(run.getRetryNo());
        item.setStartedAt(run.getStartedAt());
        item.setFinishedAt(run.getFinishedAt());
        item.setElapsedMs(run.getElapsedMs());
        item.setRetryable(FlowRunStatus.isRetryable(run.getStatus()));
        return item;
    }
}
