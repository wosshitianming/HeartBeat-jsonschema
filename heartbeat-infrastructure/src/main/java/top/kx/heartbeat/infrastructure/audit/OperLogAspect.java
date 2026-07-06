package top.kx.heartbeat.infrastructure.audit;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysOperLogDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysOperLogDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class OperLogAspect {

    private static final long DEFAULT_TENANT_ID = 1L;

    @Resource
    private SysOperLogDOMapper operLogMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            writeLog(operLog, "SUCCESS", null, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            writeLog(operLog, "FAIL", ex, System.currentTimeMillis() - start);
            throw ex;
        }
    }

    private void writeLog(OperLog operLog, String status, Throwable error, long durationMs) {
        try {
            HttpServletRequest request = currentRequest();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String operator = authentication == null ? "" : String.valueOf(authentication.getPrincipal());
            SysOperLogDOWithBLOBs entity = new SysOperLogDOWithBLOBs();
            entity.setTenantId(resolveTenantId());
            entity.setTraceId(resolveTraceId(request));
            entity.setModuleCode(operLog.module());
            entity.setOperationType(operLog.action());
            entity.setOperationName(operLog.action());
            entity.setOperatorId(parseLong(operator));
            entity.setOperatorName(operator);
            entity.setRequestMethod(request == null ? null : request.getMethod());
            entity.setRequestPath(request == null ? null : request.getRequestURI());
            entity.setRequestIp(request == null ? null : request.getRemoteAddr());
            entity.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
            entity.setResultStatus(status);
            entity.setErrorCode(error == null ? null : error.getClass().getSimpleName());
            entity.setErrorMessage(error == null ? null : truncate(error.getMessage(), 1024));
            entity.setDurationMs(durationMs);
            entity.setOperatedAt(new Date());
            TenantContext.runAsPlatform(() -> {
                operLogMapper.insertSelective(entity);
                return null;
            });
        } catch (RuntimeException ex) {
            log.warn("写入操作日志失败: {}", ex.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private String resolveTraceId(HttpServletRequest request) {
        if (request != null) {
            String traceId = request.getHeader("X-Trace-Id");
            if (StringUtils.isNotBlank(traceId)) {
                return truncate(traceId.trim(), 96);
            }
        }
        return UUID.randomUUID().toString();
    }

    private Long parseLong(String value) {
        try {
            return StringUtils.isBlank(value) ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
