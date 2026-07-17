package top.kx.heartbeat.infrastructure.tenant;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.kx.heartbeat.domain.auth.TokenIssuer;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 租户上下文清理过滤器。
 *
 * <p>确保每个请求结束后清理 ThreadLocal，避免租户信息在线程复用时泄漏。</p>
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT_PARAMETER = "tenantId";
    private static final long DEFAULT_TENANT_ID = 1L;

    @Resource
    private TokenIssuer tokenIssuer;

    /**
     * 执行租户上下文请求过滤。
     *
     * @param request HTTP 请求。
     * @param response HTTP 响应。
     * @param filterChain 过滤器链。
     * @throws ServletException Servlet 异常。
     * @throws IOException IO 异常。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                    HttpServletResponse response,
                                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                    FilterChain filterChain) throws ServletException, IOException {
        long tenantId;
        try {
            tenantId = resolveTenantId(request);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant id");
            return;
        }
        // 始终执行后续过滤器链。
        try {
            // 放行请求，交给后续过滤器和控制器处理。
            TenantContext.setTenantId(tenantId);
            request.setAttribute("heartbeatTenantId", String.valueOf(tenantId));
            filterChain.doFilter(request, response);
        } finally {
            // 请求完成后清理当前线程租户上下文。
            TenantContext.clear();
        }
    }

    private long resolveTenantId(HttpServletRequest request) {
        Long stateTenantId = socialCallbackTenantId(request);
        String value = request.getHeader(TENANT_HEADER);
        if (StringUtils.isBlank(value)) {
            value = request.getParameter(TENANT_PARAMETER);
        }
        if (StringUtils.isBlank(value)) {
            return stateTenantId == null ? DEFAULT_TENANT_ID : stateTenantId;
        }
        try {
            long tenantId = Long.parseLong(value.trim());
            if (tenantId <= 0) {
                throw new IllegalArgumentException("tenant id must be positive");
            }
            if (stateTenantId != null && stateTenantId != tenantId) {
                throw new IllegalArgumentException("tenant id does not match OAuth state");
            }
            return tenantId;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("tenant id must be a long value", ex);
        }
    }

    private Long socialCallbackTenantId(HttpServletRequest request) {
        if (!isSocialCallback(request)) {
            return null;
        }
        String state = request.getParameter("state");
        if (StringUtils.isBlank(state)) {
            return null;
        }
        try {
            return tokenIssuer.parseSocialStateTenantId(state);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("OAuth state is invalid or expired", ex);
        }
    }

    private boolean isSocialCallback(HttpServletRequest request) {
        String path = request.getServletPath();
        return path != null
                && path.startsWith("/api/v1/auth/social/")
                && path.endsWith("/callback");
    }
}
