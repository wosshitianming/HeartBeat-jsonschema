package top.kx.heartbeat.infrastructure.mp.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.port.MpAutoReplyRepository;
import top.kx.heartbeat.application.mp.request.MpAutoReplyRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpAutoReplyDO;
import top.kx.heartbeat.infrastructure.persistence.entity.mp.MpAutoReplyDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.mp.MpAutoReplyDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class MpAutoReplyRepositoryImpl implements MpAutoReplyRepository {

    @Resource
    private MpAutoReplyDOMapper autoReplyDOMapper;

    @Override
    public List<DomainRecord> listAutoReplies(String accountId) {
        MpAutoReplyDOExample example = new MpAutoReplyDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        example.setOrderByClause("sort_no ASC, id ASC");
        return autoReplyDOMapper.selectByExampleWithBLOBs(example)
                .stream()
                .map(this::toAutoReplyRecord)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord saveAutoReply(MpAutoReplyRequest request) {
        Date now = new Date();
        MpAutoReplyDO record = findAutoReplyById(longValue(request.getId(), -1L));
        if (record == null) {
            record = new MpAutoReplyDO();
            record.setTenantId(tenantId());
            record.setCreateTime(now);
            applyAutoReply(record, request);
            record.setUpdateTime(now);
            autoReplyDOMapper.insertSelective(record);
            return toAutoReplyRecord(record);
        }
        applyAutoReply(record, request);
        record.setUpdateTime(now);
        autoReplyDOMapper.updateByPrimaryKeySelective(record);
        return toAutoReplyRecord(record);
    }

    private void applyAutoReply(MpAutoReplyDO record, MpAutoReplyRequest request) {
        record.setAccountId(longValue(defaultText(request.getAccountId(), stringValue(record.getAccountId())), 0L));
        record.setKeyword(defaultText(request.getKeyword(), defaultText(record.getKeyword(), "")));
        record.setMatchType(defaultText(request.getMatchType(), defaultText(record.getMatchType(), "EXACT")));
        record.setReplyType(defaultText(request.getReplyType(), defaultText(record.getReplyType(), "TEXT")));
        record.setSortNo(request.getSortNo() == null ? intValue(record.getSortNo(), 0) : request.getSortNo());
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        record.setReplyContent(textValue(request.getReplyContent(), record.getReplyContent()));
    }

    private MpAutoReplyDO findAutoReplyById(long id) {
        if (id <= 0) {
            return null;
        }
        MpAutoReplyDO record = autoReplyDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    private DomainRecord toAutoReplyRecord(MpAutoReplyDO entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", stringValue(entity.getId()));
        row.put("tenantId", stringValue(entity.getTenantId()));
        row.put("accountId", stringValue(entity.getAccountId()));
        row.put("keyword", entity.getKeyword());
        row.put("matchType", entity.getMatchType());
        row.put("replyType", entity.getReplyType());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        row.put("replyContent", entity.getReplyContent());
        row.put("createTime", stringValue(entity.getCreateTime()));
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        return DomainRecord.of(row);
    }

    private String textValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue == null ? "" : defaultValue;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return String.valueOf(value);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    private int intValue(Object raw, int defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return raw == null ? defaultValue : Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private long longValue(Object raw, long defaultValue) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
