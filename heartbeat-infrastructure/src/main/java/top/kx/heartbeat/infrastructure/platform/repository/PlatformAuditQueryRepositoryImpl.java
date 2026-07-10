package top.kx.heartbeat.infrastructure.platform.repository;

import org.springframework.stereotype.Repository;
import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.platform.port.PlatformAuditQueryRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthOauthClientDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthOauthClientDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSessionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.auth.AuthSessionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysLoginLogDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysLoginLogDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysOperLogDO;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysOperLogDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthOauthClientDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.auth.AuthSessionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysLoginLogDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysOperLogDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实现平台管理持久化端口，通过 Mapper 完成数据读写与对象转换。
 */
@Repository
public class PlatformAuditQueryRepositoryImpl implements PlatformAuditQueryRepository {

    @Resource
    private SysLoginLogDOMapper loginLogMapper;
    @Resource
    private SysOperLogDOMapper operLogMapper;
    @Resource
    private AuthSessionDOMapper sessionMapper;
    @Resource
    private AuthOauthClientDOMapper oauthClientMapper;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listLoginLogs() {
        SysLoginLogDOExample example = new SysLoginLogDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return loginLogMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listOperationLogs() {
        SysOperLogDOExample example = new SysOperLogDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        example.setOrderByClause("create_time DESC, id DESC");
        return operLogMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listOnlineSessions() {
        AuthSessionDOExample example = new AuthSessionDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId());
        // 返回已经完成封装的业务结果。
        return sessionMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::record)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listOauthClients() {
        AuthOauthClientDOExample example = new AuthOauthClientDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId())
                .andDeleteMarkerEqualTo(0L);
        // 返回已经完成封装的业务结果。
        return oauthClientMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::record)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .collect(Collectors.toList());
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysLoginLogDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("traceId", row.getTraceId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("userId", row.getUserId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("username", row.getUsername());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("loginType", row.getLoginType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("loginIp", row.getLoginIp());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("userAgent", row.getUserAgent());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("resultStatus", row.getResultStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("failureReason", row.getFailureReason());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("loggedAt", row.getLoggedAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysOperLogDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("traceId", row.getTraceId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("moduleCode", row.getModuleCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("operationType", row.getOperationType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("operationName", row.getOperationName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("operatorId", row.getOperatorId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("operatorName", row.getOperatorName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("requestMethod", row.getRequestMethod());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("requestPath", row.getRequestPath());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("requestIp", row.getRequestIp());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("userAgent", row.getUserAgent());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("resultStatus", row.getResultStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("errorCode", row.getErrorCode());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("errorMessage", row.getErrorMessage());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("durationMs", row.getDurationMs());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("operatedAt", row.getOperatedAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(AuthSessionDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("sessionId", row.getSessionId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("userId", row.getUserId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("deviceType", row.getDeviceType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("deviceName", row.getDeviceName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("loginIp", row.getLoginIp());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("userAgent", row.getUserAgent());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("issuedAt", row.getIssuedAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("expireAt", row.getExpireAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("lastAccessAt", row.getLastAccessAt());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(AuthOauthClientDO row) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> values = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("id", row.getId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("tenantId", row.getTenantId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("clientId", row.getClientId());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("clientName", row.getClientName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("clientType", row.getClientType());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("accessTokenTtl", row.getAccessTokenTtl());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("refreshTokenTtl", row.getRefreshTokenTtl());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("scopes", row.getScopes());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("autoApprove", row.getAutoApprove());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("status", row.getStatus());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        values.put("createTime", row.getCreateTime());
        // 返回已经完成封装的业务结果。
        return DomainRecord.of(values);
    }

    private Long tenantId() {
        return TenantContext.getRequiredTenantId();
    }
}
