package top.kx.heartbeat.infrastructure.pay.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.pay.port.PayChannelRepository;
import top.kx.heartbeat.application.pay.request.PayChannelRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayChannelDO;
import top.kx.heartbeat.infrastructure.persistence.entity.pay.PayChannelDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.pay.PayChannelDOMapper;
import top.kx.heartbeat.infrastructure.security.SecretCryptoService;
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
public class PayChannelRepositoryImpl implements PayChannelRepository {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private PayChannelDOMapper channelMapper;

    @Resource
    private SecretCryptoService secretCryptoService;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成公众号管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listChannels() {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        PayChannelDOExample example = new PayChannelDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("sort_no ASC, id DESC");
        // 返回已经完成封装的业务结果。
        return channelMapper.selectByExampleWithBLOBs(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::record)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord getChannel(String id) {
        return record(requireChannel(id));
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createChannel(PayChannelRequest request) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        PayChannelDO row = channelRow(request);
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        channelMapper.insertSelective(row);
        // 返回已经完成封装的业务结果。
        return record(row);
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord updateChannel(String id, PayChannelRequest request) {
        PayChannelDO row = requireChannel(id);
        merge(row, request);
        row.setUpdateTime(new Date());
        channelMapper.updateByPrimaryKeySelective(row);
        return record(requireChannel(String.valueOf(row.getId())));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    private PayChannelDO channelRow(PayChannelRequest request) {
        PayChannelDO row = new PayChannelDO();
        merge(row, request);
        return row;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param request 公众号管理请求参数。
     */
    private void merge(PayChannelDO row, PayChannelRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PayChannelRequest safeRequest = request == null ? new PayChannelRequest() : request;
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setName(value(safeRequest.getName(), value(row.getName(), "支付渠道")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setProvider(value(safeRequest.getProvider(), value(row.getProvider(), "MOCK")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setAppId(value(safeRequest.getAppId(), value(row.getAppId(), "")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setAppSecret(secretCryptoService.encryptIfPlain(
                value(safeRequest.getAppSecret(), value(row.getAppSecret(), ""))));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(value(safeRequest.getStatus(), value(row.getStatus(), "ACTIVE")));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setSortNo(safeRequest.getSortNo() == null ? intValue(row.getSortNo(), 0) : safeRequest.getSortNo());
        // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
        Object config = safeRequest.getConfig() == null ? row.getConfigJson() : safeRequest.getConfig();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setConfigJson(jsonValue(config));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private PayChannelDO requireChannel(String id) {
        PayChannelDO row = findChannelById(longValue(id, -1L));
        if (row == null) {
            throw new IllegalArgumentException("Pay channel does not exist: " + id);
        }
        return row;
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成公众号管理数据访问。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    private PayChannelDO findChannelById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        PayChannelDO row = channelMapper.selectByPrimaryKey(id);
        return row != null && tenantId().equals(row.getTenantId()) ? row : null;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成公众号管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(PayChannelDO row) {
        migratePlainSecret(row);
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", stringValue(row.getId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", stringValue(row.getTenantId()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("name", row.getName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("provider", row.getProvider());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("appId", row.getAppId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("appSecret", secretCryptoService.decryptIfCipher(row.getAppSecret()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("sortNo", row.getSortNo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("config", readJson(row.getConfigJson()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", stringValue(row.getCreateTime()));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", stringValue(row.getUpdateTime()));
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    private void migratePlainSecret(PayChannelDO row) {
        if (row == null || StringUtils.isBlank(row.getAppSecret())
                || secretCryptoService.isEncrypted(row.getAppSecret())) {
            return;
        }
        String encrypted = secretCryptoService.encryptIfPlain(row.getAppSecret());
        PayChannelDO patch = new PayChannelDO();
        patch.setId(row.getId());
        patch.setAppSecret(encrypted);
        channelMapper.updateByPrimaryKeySelective(patch);
        row.setAppSecret(encrypted);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param json 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private JsonNode readJson(String json) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readTree(StringUtils.isBlank(json) ? "{}" : json);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("JSON parse failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String jsonValue(Object value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (value == null) {
                // 返回已经完成封装的业务结果。
                return "{}";
            }
            // 根据当前业务条件选择对应处理路径。
            if (value instanceof String) {
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                String text = ((String) value).trim();
                // 返回已经完成封装的业务结果。
                return StringUtils.isBlank(text) ? "{}" : text;
            }
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成公众号管理数据访问。
     *
     * @param raw 业务处理所需参数。
     * @param defaultValue 空值时使用的默认值。
     * @return 处理后的业务结果。
     */
    private String value(Object raw, String defaultValue) {
        String text = stringValue(raw);
        return StringUtils.isBlank(text) ? defaultValue : text;
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
