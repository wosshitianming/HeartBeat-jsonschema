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

/**
 * 实现公众号管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class MpAutoReplyRepositoryImpl implements MpAutoReplyRepository {

    @Resource
    private MpAutoReplyDOMapper autoReplyDOMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listAutoReplies(String accountId) {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        MpAutoReplyDOExample example = new MpAutoReplyDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId()).andAccountIdEqualTo(longValue(accountId, 0L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("sort_no ASC, id ASC");
        // 返回已经完成封装的业务结果。
        return autoReplyDOMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::toAutoReplyRecord)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord saveAutoReply(MpAutoReplyRequest request) {
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        MpAutoReplyDO record = findAutoReplyById(longValue(request.getId(), -1L));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (record == null) {
            // 创建数据库记录对象，承载即将写入的业务字段。
            record = new MpAutoReplyDO();
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            record.setTenantId(tenantId());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            record.setCreateTime(now);
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            applyAutoReply(record, request);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            record.setUpdateTime(now);
            // 将当前业务变更写入持久化层，保持数据状态同步。
            autoReplyDOMapper.insertSelective(record);
            // 返回已经完成封装的业务结果。
            return toAutoReplyRecord(record);
        }
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        applyAutoReply(record, request);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        autoReplyDOMapper.updateByPrimaryKeySelective(record);
        // 返回已经完成封装的业务结果。
        return toAutoReplyRecord(record);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param record 应用层业务记录。
     * @param request 公众号管理请求参数。
     */
    private void applyAutoReply(MpAutoReplyDO record, MpAutoReplyRequest request) {
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setAccountId(longValue(defaultText(request.getAccountId(), stringValue(record.getAccountId())), 0L));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setKeyword(defaultText(request.getKeyword(), defaultText(record.getKeyword(), "")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setMatchType(defaultText(request.getMatchType(), defaultText(record.getMatchType(), "EXACT")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setReplyType(defaultText(request.getReplyType(), defaultText(record.getReplyType(), "TEXT")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setSortNo(request.getSortNo() == null ? intValue(record.getSortNo(), 0) : request.getSortNo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setStatus(defaultText(request.getStatus(), defaultText(record.getStatus(), "ACTIVE")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setReplyContent(textValue(request.getReplyContent(), record.getReplyContent()));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private MpAutoReplyDO findAutoReplyById(long id) {
        if (id <= 0) {
            return null;
        }
        MpAutoReplyDO record = autoReplyDOMapper.selectByPrimaryKey(id);
        return record != null && tenantId().equals(record.getTenantId()) ? record : null;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param entity 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord toAutoReplyRecord(MpAutoReplyDO entity) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> row = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("id", stringValue(entity.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("tenantId", stringValue(entity.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("accountId", stringValue(entity.getAccountId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("keyword", entity.getKeyword());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("matchType", entity.getMatchType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("replyType", entity.getReplyType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("sortNo", entity.getSortNo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("status", entity.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("replyContent", entity.getReplyContent());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("createTime", stringValue(entity.getCreateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        row.put("updateTime", stringValue(entity.getUpdateTime()));
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String textValue(Object value, String defaultValue) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (value == null) {
            // 返回已经完成封装的业务结果。
            return defaultValue == null ? "" : defaultValue;
        }
        // 根据当前业务条件选择对应处理路径。
        if (value instanceof String) {
            // 返回已经完成封装的业务结果。
            return (String) value;
        }
        // 返回已经完成封装的业务结果。
        return String.valueOf(value);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param raw 业务处理所需参数。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private int intValue(Object raw, int defaultValue) {
        // 根据当前业务条件选择对应处理路径。
        if (raw instanceof Number) {
            // 返回已经完成封装的业务结果。
            return ((Number) raw).intValue();
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return raw == null ? defaultValue : Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
            return defaultValue;
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param raw 业务处理所需参数。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private long longValue(Object raw, long defaultValue) {
        // 根据当前业务条件选择对应处理路径。
        if (raw instanceof Number) {
            // 返回已经完成封装的业务结果。
            return ((Number) raw).longValue();
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return raw == null ? defaultValue : Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
            return defaultValue;
        }
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
