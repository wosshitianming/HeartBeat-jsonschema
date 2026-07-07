package top.kx.heartbeat.infrastructure.tenant;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
        // 始终执行后续过滤器链。
        try {
            // 放行请求，交给后续过滤器和控制器处理。
            filterChain.doFilter(request, response);
        } finally {
            // 请求完成后清理当前线程租户上下文。
            TenantContext.clear();
        }
    }
}
