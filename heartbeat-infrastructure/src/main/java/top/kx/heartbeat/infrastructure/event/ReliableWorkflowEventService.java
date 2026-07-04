package top.kx.heartbeat.infrastructure.event;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.domain.event.FlowWaitStatus;
import top.kx.heartbeat.domain.event.ReliableEventStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.event.FlowWaitStateDO;
import top.kx.heartbeat.infrastructure.persistence.entity.event.FlowWaitStateDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.event.SysInboxEventDO;
import top.kx.heartbeat.infrastructure.persistence.entity.event.SysInboxEventDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.event.SysOutboxEventDO;
import top.kx.heartbeat.infrastructure.persistence.mapper.event.FlowWaitStateDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.event.SysInboxEventDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.event.SysOutboxEventDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ReliableWorkflowEventService {

    @Resource
    private FlowWaitStateDOMapper waitStateMapper;
    @Resource
    private SysOutboxEventDOMapper outboxEventMapper;
    @Resource
    private SysInboxEventDOMapper inboxEventMapper;

    @Transactional
    public String createApprovalWait(long runId, String nodeId, String correlationKey, String payloadJson) {
        long tenantId = tenantId();
        FlowWaitStateDO existing = selectWait(tenantId, correlationKey);
        if (existing != null) {
            return existing.getCorrelationKey();
        }
        Date now = new Date();
        FlowWaitStateDO wait = new FlowWaitStateDO();
        wait.setTenantId(tenantId);
        wait.setRunId(runId);
        wait.setNodeId(nodeId);
        wait.setCorrelationKey(correlationKey);
        // 新等待记录统一进入等待状态。
        wait.setStatus(FlowWaitStatus.WAITING.getCode());
        wait.setPayloadJson(payloadJson);
        wait.setCreateTime(now);
        wait.setUpdateTime(now);
        waitStateMapper.insertSelective(wait);
        createOutbox("FLOW_APPROVAL_REQUESTED", "FLOW_RUN", String.valueOf(runId), payloadJson);
        return correlationKey;
    }

    @Transactional
    public boolean consumeOnce(String consumerCode, String eventId, String correlationKey, String resumePayloadJson) {
        long tenantId = tenantId();
        SysInboxEventDO existing = selectInbox(consumerCode, eventId);
        if (existing != null) {
            return false;
        }
        Date now = new Date();
        SysInboxEventDO inbox = new SysInboxEventDO();
        inbox.setTenantId(tenantId);
        inbox.setConsumerCode(consumerCode);
        inbox.setEventId(eventId);
        // Inbox 写入即代表当前消费者已完成幂等处理。
        inbox.setStatus(ReliableEventStatus.PROCESSED.getCode());
        inbox.setProcessedAt(now);
        inboxEventMapper.insertSelective(inbox);

        FlowWaitStateDO wait = selectWait(tenantId, correlationKey);
        // 只有等待中的记录允许被外部事件恢复。
        if (wait != null && FlowWaitStatus.WAITING.matches(wait.getStatus())) {
            wait.setStatus(FlowWaitStatus.RESUMED.getCode());
            wait.setPayloadJson(resumePayloadJson);
            wait.setUpdateTime(now);
            waitStateMapper.updateByPrimaryKeySelective(wait);
        }
        return true;
    }

    @Transactional
    public String createOutbox(String eventType, String aggregateType, String aggregateId, String payloadJson) {
        SysOutboxEventDO event = new SysOutboxEventDO();
        event.setTenantId(tenantId());
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayloadJson(payloadJson == null ? "{}" : payloadJson);
        // Outbox 新事件统一进入待投递状态。
        event.setStatus(ReliableEventStatus.NEW.getCode());
        event.setCreateTime(new Date());
        outboxEventMapper.insertSelective(event);
        return event.getEventId();
    }

    public FlowWaitStateDO findWait(String correlationKey) {
        return selectWait(tenantId(), correlationKey);
    }

    private FlowWaitStateDO selectWait(long tenantId, String correlationKey) {
        FlowWaitStateDOExample example = new FlowWaitStateDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andCorrelationKeyEqualTo(correlationKey);
        return first(waitStateMapper.selectByExample(example));
    }

    private SysInboxEventDO selectInbox(String consumerCode, String eventId) {
        SysInboxEventDOExample example = new SysInboxEventDOExample();
        example.createCriteria()
                .andConsumerCodeEqualTo(consumerCode)
                .andEventIdEqualTo(eventId);
        return first(inboxEventMapper.selectByExample(example));
    }

    private <T> T first(List<T> records) {
        return records.isEmpty() ? null : records.get(0);
    }

    private long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
