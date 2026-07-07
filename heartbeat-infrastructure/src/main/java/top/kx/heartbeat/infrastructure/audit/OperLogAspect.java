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
        // 计算当前分支的中间结果，供后续判断或组装使用。
        long start = System.currentTimeMillis();
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            Object result = joinPoint.proceed();
            // 记录操作审计信息，便于后续追踪用户行为。
            writeLog(operLog, "SUCCESS", null, System.currentTimeMillis() - start);
            // 返回已经完成封装的业务结果。
            return result;
        } catch (Throwable ex) {
            // 记录操作审计信息，便于后续追踪用户行为。
            writeLog(operLog, "FAIL", ex, System.currentTimeMillis() - start);
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw ex;
        }
    }

    private void writeLog(OperLog operLog, String status, Throwable error, long durationMs) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 读取当前 HTTP 请求上下文，用于补充审计和调用来源信息。
            HttpServletRequest request = currentRequest();
            // 计算当前分支的中间结果，供后续判断或组装使用。
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
            String operator = authentication == null ? "" : String.valueOf(authentication.getPrincipal());
            // 创建当前流程需要的临时对象，承载后续处理数据。
            SysOperLogDOWithBLOBs entity = new SysOperLogDOWithBLOBs();
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setTenantId(resolveTenantId());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setTraceId(resolveTraceId(request));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setModuleCode(operLog.module());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setOperationType(operLog.action());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setOperationName(operLog.action());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setOperatorId(parseLong(operator));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setOperatorName(operator);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setRequestMethod(request == null ? null : request.getMethod());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setRequestPath(request == null ? null : request.getRequestURI());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setRequestIp(request == null ? null : request.getRemoteAddr());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setResultStatus(status);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setErrorCode(error == null ? null : error.getClass().getSimpleName());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setErrorMessage(error == null ? null : truncate(error.getMessage(), 1024));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setDurationMs(durationMs);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setOperatedAt(new Date());
            // 读取当前租户上下文，确保数据访问始终受租户隔离约束。
            TenantContext.runAsPlatform(() -> {
                // 将当前业务变更写入持久化层，保持数据状态同步。
                operLogMapper.insertSelective(entity);
                // 返回已经完成封装的业务结果。
                return null;
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            });
        } catch (RuntimeException ex) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (request != null) {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String traceId = request.getHeader("X-Trace-Id");
            // 根据当前业务条件选择对应处理路径。
            if (StringUtils.isNotBlank(traceId)) {
                // 返回已经完成封装的业务结果。
                return truncate(traceId.trim(), 96);
            }
        }
        // 返回已经完成封装的业务结果。
        return UUID.randomUUID().toString();
    }

    private Long parseLong(String value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return StringUtils.isBlank(value) ? null : Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
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
