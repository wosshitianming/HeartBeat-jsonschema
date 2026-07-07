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
        return sessionMapper.selectByExample(new AuthSessionDOExample())
                .stream()
                .map(this::record)
                .collect(Collectors.toList());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，通过 Mapper 完成平台管理数据访问。
     *
     * @return 处理后的业务结果。
     */
    @Override
    public List<DomainRecord> listOauthClients() {
        return oauthClientMapper.selectByExample(new AuthOauthClientDOExample())
                .stream()
                .map(this::record)
                .collect(Collectors.toList());
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysLoginLogDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("traceId", row.getTraceId());
        values.put("userId", row.getUserId());
        values.put("username", row.getUsername());
        values.put("loginType", row.getLoginType());
        values.put("loginIp", row.getLoginIp());
        values.put("userAgent", row.getUserAgent());
        values.put("resultStatus", row.getResultStatus());
        values.put("failureReason", row.getFailureReason());
        values.put("loggedAt", row.getLoggedAt());
        values.put("createTime", row.getCreateTime());
        return DomainRecord.of(values);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(SysOperLogDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("traceId", row.getTraceId());
        values.put("moduleCode", row.getModuleCode());
        values.put("operationType", row.getOperationType());
        values.put("operationName", row.getOperationName());
        values.put("operatorId", row.getOperatorId());
        values.put("operatorName", row.getOperatorName());
        values.put("requestMethod", row.getRequestMethod());
        values.put("requestPath", row.getRequestPath());
        values.put("requestIp", row.getRequestIp());
        values.put("userAgent", row.getUserAgent());
        values.put("resultStatus", row.getResultStatus());
        values.put("errorCode", row.getErrorCode());
        values.put("errorMessage", row.getErrorMessage());
        values.put("durationMs", row.getDurationMs());
        values.put("operatedAt", row.getOperatedAt());
        values.put("createTime", row.getCreateTime());
        return DomainRecord.of(values);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(AuthSessionDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("sessionId", row.getSessionId());
        values.put("userId", row.getUserId());
        values.put("deviceType", row.getDeviceType());
        values.put("deviceName", row.getDeviceName());
        values.put("loginIp", row.getLoginIp());
        values.put("userAgent", row.getUserAgent());
        values.put("status", row.getStatus());
        values.put("issuedAt", row.getIssuedAt());
        values.put("expireAt", row.getExpireAt());
        values.put("lastAccessAt", row.getLastAccessAt());
        values.put("createTime", row.getCreateTime());
        return DomainRecord.of(values);
    }

    /**
     * 转换数据结构，隔离接口层、应用层与持久化层的对象差异，通过 Mapper 完成平台管理数据访问。
     *
     * @param row 待写入或转换的数据库记录。
     * @return 处理后的业务结果。
     */
    private DomainRecord record(AuthOauthClientDO row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", row.getId());
        values.put("tenantId", row.getTenantId());
        values.put("clientId", row.getClientId());
        values.put("clientName", row.getClientName());
        values.put("clientType", row.getClientType());
        values.put("accessTokenTtl", row.getAccessTokenTtl());
        values.put("refreshTokenTtl", row.getRefreshTokenTtl());
        values.put("scopes", row.getScopes());
        values.put("autoApprove", row.getAutoApprove());
        values.put("status", row.getStatus());
        values.put("createTime", row.getCreateTime());
        return DomainRecord.of(values);
    }
}
