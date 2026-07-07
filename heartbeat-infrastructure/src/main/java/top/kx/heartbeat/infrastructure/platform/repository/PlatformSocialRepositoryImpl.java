// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformSocialRepositoryImpl implements PlatformSocialRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private AuthSocialProviderDOMapper socialProviderMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private AuthSocialBindingDOMapper socialBindingMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listSocialProviders() {
        // 注释：返回当前处理结果。
        return socialProviderMapper.selectByExample(new AuthSocialProviderDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::recordProvider)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord createSocialProvider(PlatformSocialProviderRequest request) {
        // 注释：设置或计算当前变量值。
        AuthSocialProviderDO row = socialProviderRow(request);
        // 注释：执行当前代码行。
        touch(row, true);
        // 注释：执行当前代码行。
        socialProviderMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return recordProvider(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord updateSocialProvider(String id, PlatformSocialProviderRequest request) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：设置或计算当前变量值。
        AuthSocialProviderDO row = socialProviderRow(request);
        // 注释：执行当前代码行。
        row.setId(key);
        // 注释：执行当前代码行。
        touch(row, false);
        // 注释：执行当前代码行。
        socialProviderMapper.updateByPrimaryKeySelective(row);
        // 注释：设置或计算当前变量值。
        AuthSocialProviderDO persisted = key == null ? null : socialProviderMapper.selectByPrimaryKey(key);
        // 注释：返回当前处理结果。
        return recordProvider(persisted == null ? row : persisted);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public void deleteSocialProvider(String id) {
        // 注释：设置或计算当前变量值。
        Long key = longValue(id);
        // 注释：判断当前业务条件。
        if (key != null) {
            // 注释：执行当前代码行。
            socialProviderMapper.deleteByPrimaryKey(key);
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listActiveSocialProviders() {
        // 注释：设置或计算当前变量值。
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andStatusEqualTo("ENABLED");
        // 注释：返回当前处理结果。
        return socialProviderMapper.selectByExample(example)
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::recordProvider)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public Optional<DomainRecord> findSocialProvider(String provider) {
        // 注释：设置或计算当前变量值。
        AuthSocialProviderDOExample example = new AuthSocialProviderDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andProviderCodeEqualTo(provider);
        // 注释：返回当前处理结果。
        return first(socialProviderMapper.selectByExample(example)).map(this::recordProvider);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public Optional<DomainRecord> findSocialBind(String provider, String openId) {
        // 注释：设置或计算当前变量值。
        Optional<DomainRecord> providerRecord = findSocialProvider(provider);
        // 注释：设置或计算当前变量值。
        Long providerId = providerRecord.map(record -> longValue(record.get("id"))).orElse(null);
        // 注释：判断当前业务条件。
        if (providerId == null) {
            // 注释：返回当前处理结果。
            return Optional.empty();
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        AuthSocialBindingDOExample example = new AuthSocialBindingDOExample();
        // 注释：执行当前代码行。
        example.createCriteria().andProviderIdEqualTo(providerId).andExternalUserIdEqualTo(openId);
        // 注释：返回当前处理结果。
        return first(socialBindingMapper.selectByExample(example)).map(this::recordBinding);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public DomainRecord saveSocialBind(PlatformSocialBindRequest request) {
        // 注释：判断当前业务条件。
        if (request == null) {
            // 注释：抛出当前业务异常。
            throw new IllegalArgumentException("Social bind request must not be null");
            // 注释：结束当前代码块。
        }
        // 注释：设置或计算当前变量值。
        String provider = stringValue(request.getProvider());
        // 注释：设置或计算当前变量值。
        Long providerId = findSocialProvider(provider)
                // 注释：继续当前链式调用。
                .map(record -> longValue(record.get("id")))
                // 注释：继续当前链式调用。
                .orElseThrow(() -> new IllegalArgumentException("Social provider is not enabled: " + provider));
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：设置或计算当前变量值。
        AuthSocialBindingDO row = new AuthSocialBindingDO();
        // 注释：执行当前代码行。
        row.setTenantId(tenantId());
        // 注释：执行当前代码行。
        row.setProviderId(providerId);
        // 注释：执行当前代码行。
        row.setUserId(longValue(request.getUserId()));
        // 注释：执行当前代码行。
        row.setExternalUserId(stringValue(request.getOpenId()));
        // 注释：执行当前代码行。
        row.setExternalUnionId(stringValue(request.getUnionId()));
        // 注释：执行当前代码行。
        row.setExternalUsername(stringValue(request.getNickname()));
        // 注释：执行当前代码行。
        row.setExternalAvatar(stringValue(request.getAvatar()));
        // 注释：执行当前代码行。
        row.setBindingStatus("BOUND");
        // 注释：执行当前代码行。
        row.setBoundAt(now);
        // 注释：执行当前代码行。
        row.setLastLoginAt(now);
        // 注释：执行当前代码行。
        row.setCreateTime(now);
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：执行当前代码行。
        socialBindingMapper.insertSelective(row);
        // 注释：返回当前处理结果。
        return recordBinding(row);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private AuthSocialProviderDO socialProviderRow(PlatformSocialProviderRequest request) {
        // 注释：设置或计算当前变量值。
        PlatformSocialProviderRequest safeRequest =
                // 注释：设置或计算当前变量值。
                request == null ? new PlatformSocialProviderRequest() : request;
        // 注释：设置或计算当前变量值。
        AuthSocialProviderDO row = new AuthSocialProviderDO();
        // 注释：执行当前代码行。
        row.setProviderCode(safeRequest.getProviderCode());
        // 注释：执行当前代码行。
        row.setProviderName(safeRequest.getProviderName());
        // 注释：执行当前代码行。
        row.setProviderType(safeRequest.getProviderType());
        // 注释：执行当前代码行。
        row.setClientId(safeRequest.getClientId());
        // 注释：执行当前代码行。
        row.setAppKey(safeRequest.getAppKey());
        // 注释：执行当前代码行。
        row.setAppSecretCipher(safeRequest.getAppSecretCipher());
        // 注释：执行当前代码行。
        row.setAuthorizeUrl(safeRequest.getAuthorizeUrl());
        // 注释：执行当前代码行。
        row.setTokenUrl(safeRequest.getTokenUrl());
        // 注释：执行当前代码行。
        row.setUserInfoUrl(safeRequest.getUserInfoUrl());
        // 注释：执行当前代码行。
        row.setScopes(safeRequest.getScopes());
        // 注释：执行当前代码行。
        row.setEnabled(safeRequest.getEnabled());
        // 注释：执行当前代码行。
        row.setStatus(safeRequest.getStatus());
        // 注释：返回当前处理结果。
        return row;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private void touch(AuthSocialProviderDO row, boolean creating) {
        // 注释：设置或计算当前变量值。
        Date now = new Date();
        // 注释：判断当前业务条件。
        if (creating) {
            // 注释：执行当前代码行。
            row.setTenantId(tenantId());
            // 注释：执行当前代码行。
            row.setCreateTime(now);
            // 注释：执行当前代码行。
            row.setVersion(0);
            // 注释：执行当前代码行。
            row.setDeleteMarker(0L);
            // 注释：判断当前业务条件。
            if (row.getStatus() == null) {
                // 注释：执行当前代码行。
                row.setStatus("ENABLED");
                // 注释：结束当前代码块。
            }
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        row.setUpdateTime(now);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord recordProvider(AuthSocialProviderDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：判断当前业务条件。
        if (row == null) {
            // 注释：返回当前处理结果。
            return DomainRecord.of(values);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("providerCode", row.getProviderCode());
        // 注释：执行当前代码行。
        values.put("provider", row.getProviderCode());
        // 注释：执行当前代码行。
        values.put("providerName", row.getProviderName());
        // 注释：执行当前代码行。
        values.put("name", row.getProviderName());
        // 注释：执行当前代码行。
        values.put("providerType", row.getProviderType());
        // 注释：执行当前代码行。
        values.put("clientId", row.getClientId());
        // 注释：执行当前代码行。
        values.put("appKey", row.getAppKey());
        // 注释：执行当前代码行。
        values.put("appSecretCipher", row.getAppSecretCipher());
        // 注释：执行当前代码行。
        values.put("authorizeUrl", row.getAuthorizeUrl());
        // 注释：执行当前代码行。
        values.put("tokenUrl", row.getTokenUrl());
        // 注释：执行当前代码行。
        values.put("userInfoUrl", row.getUserInfoUrl());
        // 注释：执行当前代码行。
        values.put("scopes", row.getScopes());
        // 注释：执行当前代码行。
        values.put("enabled", row.getEnabled());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：执行当前代码行。
        values.put("updateTime", row.getUpdateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord recordBinding(AuthSocialBindingDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：判断当前业务条件。
        if (row == null) {
            // 注释：返回当前处理结果。
            return DomainRecord.of(values);
            // 注释：结束当前代码块。
        }
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("providerId", row.getProviderId());
        // 注释：执行当前代码行。
        values.put("userId", row.getUserId());
        // 注释：执行当前代码行。
        values.put("externalUserId", row.getExternalUserId());
        // 注释：执行当前代码行。
        values.put("externalUnionId", row.getExternalUnionId());
        // 注释：执行当前代码行。
        values.put("externalUsername", row.getExternalUsername());
        // 注释：执行当前代码行。
        values.put("externalAvatar", row.getExternalAvatar());
        // 注释：执行当前代码行。
        values.put("bindingStatus", row.getBindingStatus());
        // 注释：执行当前代码行。
        values.put("boundAt", row.getBoundAt());
        // 注释：执行当前代码行。
        values.put("lastLoginAt", row.getLastLoginAt());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：执行当前代码行。
        values.put("updateTime", row.getUpdateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private <T> Optional<T> first(List<T> rows) {
        // 注释：返回当前处理结果。
        return rows == null || rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long tenantId() {
        // 注释：设置或计算当前变量值。
        Long tenantId = TenantContext.getTenantId();
        // 注释：返回当前处理结果。
        return tenantId == null ? 1L : tenantId;
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Long longValue(Object value) {
        // 注释：判断当前业务条件。
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：开始执行可能抛出异常的逻辑。
        try {
            // 注释：返回当前处理结果。
            return Long.parseLong(String.valueOf(value).trim());
            // 注释：捕获并处理当前异常。
        } catch (NumberFormatException ignored) {
            // 注释：返回当前处理结果。
            return null;
            // 注释：结束当前代码块。
        }
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private String stringValue(Object value) {
        // 注释：返回当前处理结果。
        return value == null ? "" : String.valueOf(value).trim();
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
