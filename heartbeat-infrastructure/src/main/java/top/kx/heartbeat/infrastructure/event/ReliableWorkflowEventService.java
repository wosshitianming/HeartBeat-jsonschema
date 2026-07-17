package top.kx.heartbeat.infrastructure.event;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.domain.event.FlowWaitStatus;
import top.kx.heartbeat.domain.event.ReliableEventStatus;
import top.kx.heartbeat.infrastructure.persistence.entity.event.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.event.FlowWaitStateDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.event.SysInboxEventDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.event.SysOutboxEventDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
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
        // 计算当前分支的中间结果，供后续判断或组装使用。
        long tenantId = tenantId();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        FlowWaitStateDO existing = selectWait(tenantId, correlationKey);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (existing != null) {
            // 返回已经完成封装的业务结果。
            return existing.getCorrelationKey();
        }
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 创建数据库记录对象，承载即将写入的业务字段。
        FlowWaitStateDO wait = new FlowWaitStateDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        wait.setTenantId(tenantId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        wait.setRunId(runId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        wait.setNodeId(nodeId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        wait.setCorrelationKey(correlationKey);
        // 新等待记录统一进入等待状态。
        wait.setStatus(FlowWaitStatus.WAITING.getCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        wait.setPayloadJson(payloadJson);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        wait.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        wait.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        waitStateMapper.insertSelective(wait);
        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
        createOutbox("FLOW_APPROVAL_REQUESTED", "FLOW_RUN", String.valueOf(runId), payloadJson);
        // 返回已经完成封装的业务结果。
        return correlationKey;
    }

    @Transactional
    public boolean consumeOnce(String consumerCode, String eventId, String correlationKey, String resumePayloadJson) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        long tenantId = tenantId();
        // 组装工作流事件状态，保证可靠事件可以被后续消费。
        SysInboxEventDO existing = selectInbox(consumerCode, eventId);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (existing != null) {
            // 返回已经完成封装的业务结果。
            return false;
        }
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 创建数据库记录对象，承载即将写入的业务字段。
        SysInboxEventDO inbox = new SysInboxEventDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        inbox.setTenantId(tenantId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        inbox.setConsumerCode(consumerCode);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        inbox.setEventId(eventId);
        // Inbox 写入即代表当前消费者已完成幂等处理。
        inbox.setStatus(ReliableEventStatus.PROCESSED.getCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        inbox.setProcessedAt(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        inboxEventMapper.insertSelective(inbox);

        // 计算当前分支的中间结果，供后续判断或组装使用。
        FlowWaitStateDO wait = selectWait(tenantId, correlationKey);
        // 只有等待中的记录允许被外部事件恢复。
        if (wait != null && FlowWaitStatus.WAITING.matches(wait.getStatus())) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            wait.setStatus(FlowWaitStatus.RESUMED.getCode());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            wait.setPayloadJson(resumePayloadJson);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            wait.setUpdateTime(now);
            // 将当前业务变更写入持久化层，保持数据状态同步。
            waitStateMapper.updateByPrimaryKeySelective(wait);
        }
        // 返回已经完成封装的业务结果。
        return true;
    }

    @Transactional
    public String createOutbox(String eventType, String aggregateType, String aggregateId, String payloadJson) {
        // 创建数据库记录对象，承载即将写入的业务字段。
        SysOutboxEventDO event = new SysOutboxEventDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        event.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        event.setEventId(UUID.randomUUID().toString());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        event.setEventType(eventType);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        event.setAggregateType(aggregateType);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        event.setAggregateId(aggregateId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        event.setPayloadJson(payloadJson == null ? "{}" : payloadJson);
        // Outbox 新事件统一进入待投递状态。
        event.setStatus(ReliableEventStatus.NEW.getCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        event.setCreateTime(new Date());
        // 将当前业务变更写入持久化层，保持数据状态同步。
        outboxEventMapper.insertSelective(event);
        // 返回已经完成封装的业务结果。
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
        return first(waitStateMapper.selectByExampleWithBLOBs(example));
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
