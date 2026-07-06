package top.kx.heartbeat.infrastructure.flow.flowable;

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.common.response.RecordResponse;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flowable 历史查询服务。
 *
 * <p>用于从 Flowable 历史表读取引擎事实，支撑后续投影补偿任务。</p>
 */
@Service
public class FlowableHistoryQueryService {

    /**
     * Flowable 历史服务。
     */
    @Resource
    private HistoryService historyService;

    /**
     * 查询历史流程实例摘要。
     *
     * @param processInstanceId 流程实例标识
     * @return 历史流程实例摘要
     */
    public RecordResponse findProcessSummary(String processInstanceId) {
        // 查询历史流程实例。
        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        // 创建历史摘要。
        Map<String, Object> summary = new LinkedHashMap<>();
        // 判断历史实例是否存在。
        if (instance == null) {
            // 返回空摘要。
            return RecordResponse.from(summary);
        }
        // 写入流程实例标识。
        summary.put("processInstanceId", instance.getId());
        // 写入流程定义标识。
        summary.put("processDefinitionId", instance.getProcessDefinitionId());
        // 写入业务键。
        summary.put("businessKey", instance.getBusinessKey());
        // 写入开始时间。
        summary.put("startTime", instance.getStartTime());
        // 写入结束时间。
        summary.put("endTime", instance.getEndTime());
        // 写入删除原因。
        summary.put("deleteReason", instance.getDeleteReason());
        // 返回历史摘要。
        return RecordResponse.from(summary);
    }
}
