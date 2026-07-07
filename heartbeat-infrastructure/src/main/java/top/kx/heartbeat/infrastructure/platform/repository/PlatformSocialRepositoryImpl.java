package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformSocialRepository;
import top.kx.heartbeat.application.platform.request.PlatformSocialBindRequest;
import top.kx.heartbeat.application.platform.request.PlatformSocialProviderRequest;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialBindingDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialBindingDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialProviderDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSocialProviderDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSocialBindingDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSocialProviderDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PlatformSocialRepositoryImpl implements PlatformSocialRepository {

    @Resource
    private AuthSocialProviderDOMapper socialProviderMapper;
    @Resource
    private AuthSocialBindingDOMapper socialBindingMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listSocialProviders() {
        return socialProviderMapper.selectByExample(new AuthSocialProviderDOExample())
                .stream()
                .map(this::recordProvider)
                .collect(Collectors.toList());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord createSocialProvider(PlatformSocialProviderRequest request) {
        AuthSocialProviderDO row = socialProviderRow(request);
        touch(row, true);
        socialProviderMapper.insertSelective(row);
        return recordProvider(row);
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord updateSocialProvider(String id, PlatformSocialProviderRequest request) {
        Long key = longValue(id);
        AuthSocialProviderDO row = socialProviderRow(request);
        row.setId(key);
        touch(row, false);
        socialProviderMapper.updateByPrimaryKeySelective(row);
        AuthSocialProviderDO persisted = key == null ? null : socialProviderMapper.selectByPrimaryKey(key);
        return recordProvider(persisted == null ? row : persisted);
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param id 业务记录标识。
     */
    @Override
    public void deleteSocialProvider(String id) {
        Long key = longValue(id);
        if (key != null) {
            socialProviderMapper.deleteByPrimaryKey(key);
        }
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listActiveSocialProviders() {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria().andStatusEqualTo("ENABLED");
        return socialProviderMapper.selectByExample(example)
                .stream()
                .map(this::recordProvider)
                .collect(Collectors.toList());
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成平台管理数据访问。
     *
     * @param provider 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @Override
    public Optional<DomainRecord> findSocialProvider(String provider) {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria().andProviderCodeEqualTo(provider);
        return first(socialProviderMapper.selectByExample(example)).map(this::recordProvider);
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，通过 Mapper 完成平台管理数据访问。
     *
     * @param provider 业务处理所需参数。
     * @param openId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @Override
    public Optional<DomainRecord> findSocialBind(String provider, String openId) {
        Optional<DomainRecord> providerRecord = findSocialProvider(provider);
        Long providerId = providerRecord.map(record -> longValue(record.get("id"))).orElse(null);
        if (providerId == null) {
            return Optional.empty();
        }
        AuthSocialBindingDOExample example = new AuthSocialBindingDOExample();
        example.createCriteria().andProviderIdEqualTo(providerId).andExternalUserIdEqualTo(openId);
        return first(socialBindingMapper.selectByExample(example)).map(this::recordBinding);
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    @Override
    public DomainRecord saveSocialBind(PlatformSocialBindRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Social bind request must not be null");
        }
        String provider = stringValue(request.getProvider());
        Long providerId = findSocialProvider(provider)
                .map(record -> longValue(record.get("id")))
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        Date now = new Date();
        AuthSocialBindingDO row = new AuthSocialBindingDO();
        row.setTenantId(tenantId());
        row.setProviderId(providerId);
        row.setUserId(longValue(request.getUserId()));
        row.setExternalUserId(stringValue(request.getOpenId()));
        row.setExternalUnionId(stringValue(request.getUnionId()));
        row.setExternalUsername(stringValue(request.getNickname()));
        row.setExternalAvatar(stringValue(request.getAvatar()));
        row.setBindingStatus("BOUND");
        row.setBoundAt(now);
        row.setLastLoginAt(now);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        socialBindingMapper.insertSelective(row);
        return recordBinding(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private AuthSocialProviderDO socialProviderRow(PlatformSocialProviderRequest request) {
        PlatformSocialProviderRequest safeRequest =
                request == null ? new PlatformSocialProviderRequest() : request;
        AuthSocialProviderDO row = new AuthSocialProviderDO();
        row.setProviderCode(safeRequest.getProviderCode());
        row.setProviderName(safeRequest.getProviderName());
        row.setProviderType(safeRequest.getProviderType());
        row.setClientId(safeRequest.getClientId());
        row.setAppKey(safeRequest.getAppKey());
        row.setAppSecretCipher(safeRequest.getAppSecretCipher());
        row.setAuthorizeUrl(safeRequest.getAuthorizeUrl());
        row.setTokenUrl(safeRequest.getTokenUrl());
        row.setUserInfoUrl(safeRequest.getUserInfoUrl());
        row.setScopes(safeRequest.getScopes());
        row.setEnabled(safeRequest.getEnabled());
        row.setStatus(safeRequest.getStatus());
        return row;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param creating 是否为新增写入。
     */
    private void touch(AuthSocialProviderDO row, boolean creating) {
        Date now = new Date();
        if (creating) {
            row.setTenantId(tenantId());
            row.setCreateTime(now);
            row.setVersion(0);
            row.setDeleteMarker(0L);
            if (row.getStatus() == null) {
                row.setStatus("ENABLED");
            }
        }
        row.setUpdateTime(now);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord recordProvider(AuthSocialProviderDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("providerCode", row.getProviderCode());
        values.put("provider", row.getProviderCode());
        values.put("providerName", row.getProviderName());
        values.put("name", row.getProviderName());
        values.put("providerType", row.getProviderType());
        values.put("clientId", row.getClientId());
        values.put("appKey", row.getAppKey());
        values.put("appSecretCipher", row.getAppSecretCipher());
        values.put("authorizeUrl", row.getAuthorizeUrl());
        values.put("tokenUrl", row.getTokenUrl());
        values.put("userInfoUrl", row.getUserInfoUrl());
        values.put("scopes", row.getScopes());
        values.put("enabled", row.getEnabled());
        values.put("status", row.getStatus());
        values.put("createTime", row.getCreateTime());
        values.put("updateTime", row.getUpdateTime());
        return DomainRecord.of(values);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord recordBinding(AuthSocialBindingDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return DomainRecord.of(values);
        }
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("providerId", row.getProviderId());
        values.put("userId", row.getUserId());
        values.put("externalUserId", row.getExternalUserId());
        values.put("externalUnionId", row.getExternalUnionId());
        values.put("externalUsername", row.getExternalUsername());
        values.put("externalAvatar", row.getExternalAvatar());
        values.put("bindingStatus", row.getBindingStatus());
        values.put("boundAt", row.getBoundAt());
        values.put("lastLoginAt", row.getLastLoginAt());
        values.put("createTime", row.getCreateTime());
        values.put("updateTime", row.getUpdateTime());
        return DomainRecord.of(values);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param rows 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    private <T> Optional<T> first(List<T> rows) {
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
    }

    /**
     * 读取当前租户上下文，保证数据写入归属正确，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 统一处理字符串兜底，避免空值在业务流程中扩散，通过 Mapper 完成平台管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
