// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@Repository
public class PlatformAuditQueryRepositoryImpl implements PlatformAuditQueryRepository {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysLoginLogDOMapper loginLogMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private SysOperLogDOMapper operLogMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private AuthSessionDOMapper sessionMapper;
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private AuthOauthClientDOMapper oauthClientMapper;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listLoginLogs() {
        // 注释：设置或计算当前变量值。
        SysLoginLogDOExample example = new SysLoginLogDOExample();
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return loginLogMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listOperationLogs() {
        // 注释：设置或计算当前变量值。
        SysOperLogDOExample example = new SysOperLogDOExample();
        // 注释：执行当前代码行。
        example.setOrderByClause("create_time DESC, id DESC");
        // 注释：返回当前处理结果。
        return operLogMapper.selectByExample(example).stream().map(this::record).collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listOnlineSessions() {
        // 注释：返回当前处理结果。
        return sessionMapper.selectByExample(new AuthSessionDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::record)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @Override
    public List<DomainRecord> listOauthClients() {
        // 注释：返回当前处理结果。
        return oauthClientMapper.selectByExample(new AuthOauthClientDOExample())
                // 注释：继续当前链式调用。
                .stream()
                // 注释：继续当前链式调用。
                .map(this::record)
                // 注释：继续当前链式调用。
                .collect(Collectors.toList());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(SysLoginLogDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("traceId", row.getTraceId());
        // 注释：执行当前代码行。
        values.put("userId", row.getUserId());
        // 注释：执行当前代码行。
        values.put("username", row.getUsername());
        // 注释：执行当前代码行。
        values.put("loginType", row.getLoginType());
        // 注释：执行当前代码行。
        values.put("loginIp", row.getLoginIp());
        // 注释：执行当前代码行。
        values.put("userAgent", row.getUserAgent());
        // 注释：执行当前代码行。
        values.put("resultStatus", row.getResultStatus());
        // 注释：执行当前代码行。
        values.put("failureReason", row.getFailureReason());
        // 注释：执行当前代码行。
        values.put("loggedAt", row.getLoggedAt());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(SysOperLogDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("traceId", row.getTraceId());
        // 注释：执行当前代码行。
        values.put("moduleCode", row.getModuleCode());
        // 注释：执行当前代码行。
        values.put("operationType", row.getOperationType());
        // 注释：执行当前代码行。
        values.put("operationName", row.getOperationName());
        // 注释：执行当前代码行。
        values.put("operatorId", row.getOperatorId());
        // 注释：执行当前代码行。
        values.put("operatorName", row.getOperatorName());
        // 注释：执行当前代码行。
        values.put("requestMethod", row.getRequestMethod());
        // 注释：执行当前代码行。
        values.put("requestPath", row.getRequestPath());
        // 注释：执行当前代码行。
        values.put("requestIp", row.getRequestIp());
        // 注释：执行当前代码行。
        values.put("userAgent", row.getUserAgent());
        // 注释：执行当前代码行。
        values.put("resultStatus", row.getResultStatus());
        // 注释：执行当前代码行。
        values.put("errorCode", row.getErrorCode());
        // 注释：执行当前代码行。
        values.put("errorMessage", row.getErrorMessage());
        // 注释：执行当前代码行。
        values.put("durationMs", row.getDurationMs());
        // 注释：执行当前代码行。
        values.put("operatedAt", row.getOperatedAt());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(AuthSessionDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("sessionId", row.getSessionId());
        // 注释：执行当前代码行。
        values.put("userId", row.getUserId());
        // 注释：执行当前代码行。
        values.put("deviceType", row.getDeviceType());
        // 注释：执行当前代码行。
        values.put("deviceName", row.getDeviceName());
        // 注释：执行当前代码行。
        values.put("loginIp", row.getLoginIp());
        // 注释：执行当前代码行。
        values.put("userAgent", row.getUserAgent());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("issuedAt", row.getIssuedAt());
        // 注释：执行当前代码行。
        values.put("expireAt", row.getExpireAt());
        // 注释：执行当前代码行。
        values.put("lastAccessAt", row.getLastAccessAt());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private DomainRecord record(AuthOauthClientDO row) {
        // 注释：设置或计算当前变量值。
        Map<String, Object> values = new LinkedHashMap<>();
        // 注释：执行当前代码行。
        values.put("id", row.getId());
        // 注释：执行当前代码行。
        values.put("tenantId", row.getTenantId());
        // 注释：执行当前代码行。
        values.put("clientId", row.getClientId());
        // 注释：执行当前代码行。
        values.put("clientName", row.getClientName());
        // 注释：执行当前代码行。
        values.put("clientType", row.getClientType());
        // 注释：执行当前代码行。
        values.put("accessTokenTtl", row.getAccessTokenTtl());
        // 注释：执行当前代码行。
        values.put("refreshTokenTtl", row.getRefreshTokenTtl());
        // 注释：执行当前代码行。
        values.put("scopes", row.getScopes());
        // 注释：执行当前代码行。
        values.put("autoApprove", row.getAutoApprove());
        // 注释：执行当前代码行。
        values.put("status", row.getStatus());
        // 注释：执行当前代码行。
        values.put("createTime", row.getCreateTime());
        // 注释：返回当前处理结果。
        return DomainRecord.of(values);
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
