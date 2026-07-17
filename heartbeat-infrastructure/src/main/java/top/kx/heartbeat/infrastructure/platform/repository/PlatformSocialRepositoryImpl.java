package top.kx.heartbeat.infrastructure.platform.repository;

import org.apache.commons.lang3.StringUtils;
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
import top.kx.heartbeat.infrastructure.security.SecretCryptoService;
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
    @Resource
    private SecretCryptoService secretCryptoService;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listSocialProviders() {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
        // 返回已经完成封装的业务结果。
        return socialProviderMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::recordProviderForManagement)
                // 使用流式转换批量映射数据，减少中间状态暴露。
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
        return recordProviderForManagement(row);
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
        if (key == null) {
            throw new IllegalArgumentException("Invalid social provider id: " + id);
        }
        AuthSocialProviderDO row = socialProviderRow(request);
        row.setId(key);
        touch(row, false);
        AuthSocialProviderDO persisted = null;
        if (key != null) {
            AuthSocialProviderDOExample example = providerById(key);
            if (socialProviderMapper.updateByExampleSelective(row, example) == 0) {
                throw new IllegalArgumentException("Social provider does not exist: " + id);
            }
            persisted = first(socialProviderMapper.selectByExample(example)).orElse(null);
        }
        return recordProviderForManagement(persisted == null ? row : persisted);
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
            socialProviderMapper.deleteByExample(providerById(key));
        }
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listActiveSocialProviders() {
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andStatusEqualTo("ENABLED")
                .andEnabledEqualTo(Boolean.TRUE)
                .andDeleteMarkerEqualTo(0L);
        // 返回已经完成封装的业务结果。
        return socialProviderMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::recordProviderForPublic)
                // 使用流式转换批量映射数据，减少中间状态暴露。
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
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andProviderCodeEqualTo(provider)
                .andStatusEqualTo("ENABLED")
                .andEnabledEqualTo(Boolean.TRUE)
                .andDeleteMarkerEqualTo(0L);
        return first(socialProviderMapper.selectByExample(example)).map(this::recordProviderForRuntime);
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
        // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
        Optional<DomainRecord> providerRecord = findSocialProvider(provider);
        // 使用流式转换批量映射数据，减少中间状态暴露。
        Long providerId = providerRecord.map(record -> longValue(record.get("id"))).orElse(null);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (providerId == null) {
            // 返回已经完成封装的业务结果。
            return Optional.empty();
        }
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        AuthSocialBindingDOExample example = new AuthSocialBindingDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andProviderIdEqualTo(providerId)
                .andExternalUserIdEqualTo(openId);
        // 返回已经完成封装的业务结果。
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
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (request == null) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Social bind request must not be null");
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String provider = stringValue(request.getProvider());
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long providerId = findSocialProvider(provider)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(record -> longValue(record.get("id")))
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 创建数据库记录对象，承载即将写入的业务字段。
        AuthSocialBindingDO row = new AuthSocialBindingDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTenantId(tenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setProviderId(providerId);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUserId(longValue(request.getUserId()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setExternalUserId(stringValue(request.getOpenId()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setExternalUnionId(stringValue(request.getUnionId()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setExternalUsername(stringValue(request.getNickname()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setExternalAvatar(stringValue(request.getAvatar()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setBindingStatus("BOUND");
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setBoundAt(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setLastLoginAt(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setCreateTime(now);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(now);
        // 将当前业务变更写入持久化层，保持数据状态同步。
        socialBindingMapper.insertSelective(row);
        // 返回已经完成封装的业务结果。
        return recordBinding(row);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param request 平台管理请求参数。
     * @return 处理后的业务结果。
     */
    private AuthSocialProviderDO socialProviderRow(PlatformSocialProviderRequest request) {
        // 兜底空请求对象，保证后续字段读取不需要反复判空。
        PlatformSocialProviderRequest safeRequest =
                // 创建下游写入请求对象，集中承载本次业务处理结果。
                request == null ? new PlatformSocialProviderRequest() : request;
        // 创建数据库记录对象，承载即将写入的业务字段。
        AuthSocialProviderDO row = new AuthSocialProviderDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setProviderCode(safeRequest.getProviderCode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setProviderName(safeRequest.getProviderName());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setProviderType(safeRequest.getProviderType());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setClientId(safeRequest.getClientId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setAppKey(safeRequest.getAppKey());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        String appSecret = StringUtils.trimToNull(safeRequest.getAppSecretCipher());
        row.setAppSecretCipher(appSecret == null ? null : secretCryptoService.encryptIfPlain(appSecret));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setAuthorizeUrl(safeRequest.getAuthorizeUrl());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setTokenUrl(safeRequest.getTokenUrl());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUserInfoUrl(safeRequest.getUserInfoUrl());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setScopes(safeRequest.getScopes());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setEnabled(safeRequest.getEnabled());
        row.setAutoRegister(safeRequest.getAutoRegister());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setStatus(safeRequest.getStatus());
        // 返回已经完成封装的业务结果。
        return row;
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @param creating 是否为新增写入。
     */
    private void touch(AuthSocialProviderDO row, boolean creating) {
        // 统一生成当前时间，保证本次写入使用同一审计时间。
        Date now = new Date();
        // 根据当前业务条件选择对应处理路径。
        if (creating) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setTenantId(tenantId());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setCreateTime(now);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setVersion(0);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            row.setDeleteMarker(0L);
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (row.getStatus() == null) {
                // 设置持久化字段，保证数据库记录具备完整业务属性。
                row.setStatus("ENABLED");
            }
            if (row.getAutoRegister() == null) {
                row.setAutoRegister(Boolean.FALSE);
            }
        }
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        row.setUpdateTime(now);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord recordProviderForManagement(AuthSocialProviderDO row) {
        Map<String, Object> values = providerValues(row);
        values.put("appSecretCipher", row == null ? "" : secretCryptoService.redact(row.getAppSecretCipher()));
        return DomainRecord.of(values);
    }

    private DomainRecord recordProviderForPublic(AuthSocialProviderDO row) {
        return DomainRecord.of(providerValues(row));
    }

    private DomainRecord recordProviderForRuntime(AuthSocialProviderDO row) {
        Map<String, Object> values = providerValues(row);
        values.put("appSecret", row == null ? "" : secretCryptoService.decryptIfCipher(row.getAppSecretCipher()));
        return DomainRecord.of(values);
    }

    private Map<String, Object> providerValues(AuthSocialProviderDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (row == null) {
            // 返回已经完成封装的业务结果。
            return values;
        }
        migratePlainSecret(row);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("providerCode", row.getProviderCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("provider", row.getProviderCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("providerName", row.getProviderName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("name", row.getProviderName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("providerType", row.getProviderType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("clientId", row.getClientId());
        values.put("appId", row.getClientId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("appKey", row.getAppKey());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("authorizeUrl", row.getAuthorizeUrl());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tokenUrl", row.getTokenUrl());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("userInfoUrl", row.getUserInfoUrl());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("scopes", row.getScopes());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("enabled", row.getEnabled());
        values.put("autoRegister", row.getAutoRegister());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", row.getUpdateTime());
        return values;
    }

    private void migratePlainSecret(AuthSocialProviderDO row) {
        if (row == null || StringUtils.isBlank(row.getAppSecretCipher())
                || secretCryptoService.isEncrypted(row.getAppSecretCipher())) {
            return;
        }
        String encrypted = secretCryptoService.encryptIfPlain(row.getAppSecretCipher());
        AuthSocialProviderDO patch = new AuthSocialProviderDO();
        patch.setId(row.getId());
        patch.setAppSecretCipher(encrypted);
        socialProviderMapper.updateByPrimaryKeySelective(patch);
        row.setAppSecretCipher(encrypted);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord recordBinding(AuthSocialBindingDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (row == null) {
            // 返回已经完成封装的业务结果。
            return DomainRecord.of(values);
        }
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("providerId", row.getProviderId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("userId", row.getUserId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("externalUserId", row.getExternalUserId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("externalUnionId", row.getExternalUnionId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("externalUsername", row.getExternalUsername());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("externalAvatar", row.getExternalAvatar());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("bindingStatus", row.getBindingStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("boundAt", row.getBoundAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("lastLoginAt", row.getLastLoginAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("updateTime", row.getUpdateTime());
        // 返回已经完成封装的业务结果。
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
        return TenantContext.getRequiredTenantId();
    }

    private AuthSocialProviderDOExample providerById(Long id) {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andIdEqualTo(id)
                .andDeleteMarkerEqualTo(0L);
        return example;
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，通过 Mapper 完成平台管理数据访问。
     *
     * @param value 待转换的原始值。
     * @return 处理后的业务结果。
     */
    private Long longValue(Object value) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            // 返回已经完成封装的业务结果。
            return null;
        }
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
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
