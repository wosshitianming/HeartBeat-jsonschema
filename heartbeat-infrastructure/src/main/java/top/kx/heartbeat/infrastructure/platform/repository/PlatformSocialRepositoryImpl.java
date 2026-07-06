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

@Repository
public class PlatformSocialRepositoryImpl implements PlatformSocialRepository {

    @Resource
    private AuthSocialProviderDOMapper socialProviderMapper;
    @Resource
    private AuthSocialBindingDOMapper socialBindingMapper;

    @Override
    public List<DomainRecord> listSocialProviders() {
        return socialProviderMapper.selectByExample(new AuthSocialProviderDOExample())
                .stream()
                .map(this::recordProvider)
                .collect(Collectors.toList());
    }

    @Override
    public DomainRecord createSocialProvider(PlatformSocialProviderRequest request) {
        AuthSocialProviderDO row = socialProviderRow(request);
        touch(row, true);
        socialProviderMapper.insertSelective(row);
        return recordProvider(row);
    }

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

    @Override
    public void deleteSocialProvider(String id) {
        Long key = longValue(id);
        if (key != null) {
            socialProviderMapper.deleteByPrimaryKey(key);
        }
    }

    @Override
    public List<DomainRecord> listActiveSocialProviders() {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria().andStatusEqualTo("ENABLED");
        return socialProviderMapper.selectByExample(example)
                .stream()
                .map(this::recordProvider)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DomainRecord> findSocialProvider(String provider) {
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        example.createCriteria().andProviderCodeEqualTo(provider);
        return first(socialProviderMapper.selectByExample(example)).map(this::recordProvider);
    }

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

    private <T> Optional<T> first(List<T> rows) {
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
    }

    private Long tenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
