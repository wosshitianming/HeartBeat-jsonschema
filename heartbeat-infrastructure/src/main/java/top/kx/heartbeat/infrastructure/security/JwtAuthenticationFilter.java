package top.kx.heartbeat.infrastructure.security;


import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.kx.heartbeat.application.platform.port.PlatformPermissionRepository;
import top.kx.heartbeat.domain.auth.AuthSession;
import top.kx.heartbeat.domain.auth.AuthSessionRepository;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Restores Spring Security and tenant context from a Bearer JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtTokenService jwtTokenService;
    @Resource
    private PlatformPermissionRepository platformPermissionRepository;
    @Resource
    private AuthSessionRepository authSessionRepository;

    @Value("${heartbeat.security.dev-auto-login:false}")
    private boolean devAutoLogin;

    @Value("${heartbeat.security.dev-header-enabled:false}")
    private boolean devHeaderEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        AuthenticatedSession session = resolveSession(request);
        if (session != null) {
            TenantContext.setTenantId(session.tenantId);
            request.setAttribute("heartbeatUserId", session.userId);
            request.setAttribute("heartbeatTenantId", String.valueOf(session.tenantId));
            request.setAttribute("heartbeatSessionId", session.sessionId);
            SecurityContextHolder.getContext().setAuthentication(authenticationFor(session.userId));
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private UsernamePasswordAuthenticationToken authenticationFor(String userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        for (String permission : platformPermissionRepository.listPermissionsByUserId(userId)) {
            if (StringUtils.isNotBlank(permission)) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    private AuthenticatedSession resolveSession(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(authorization) && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            if (StringUtils.isNotEmpty(token) && !token.startsWith("dev-")) {
                try {
                    String userId = jwtTokenService.parseUserId(token);
                    long tenantId = jwtTokenService.parseTenantId(token);
                    String sessionId = jwtTokenService.parseSessionId(token);
                    requireActiveSession(tenantId, userId, sessionId);
                    return new AuthenticatedSession(userId, tenantId, sessionId);
                } catch (RuntimeException ignored) {
                    return null;
                }
            }
        }

        if (devHeaderEnabled) {
            String legacyUserId = request.getHeader("X-User-Id");
            if (StringUtils.isNotBlank(legacyUserId)) {
                return new AuthenticatedSession(legacyUserId.trim(), 1L, "dev-header");
            }
        }
        if (devAutoLogin) {
            return new AuthenticatedSession("1", 1L, "dev-auto-login");
        }
        return null;
    }

    private void requireActiveSession(long tenantId, String userId, String sessionId) {
        LocalDateTime nowLdt = LocalDateTime.now();
        AuthSession session = authSessionRepository.findActive(tenantId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Auth session is not active"));
        if (session.getUserId() != Long.parseLong(userId) || !session.isActiveAt(nowLdt)) {
            throw new IllegalArgumentException("Auth session is not active");
        }
        authSessionRepository.touch(tenantId, sessionId, nowLdt);
    }

    private static final class AuthenticatedSession {
        private final String userId;
        private final long tenantId;
        private final String sessionId;

        private AuthenticatedSession(String userId, long tenantId, String sessionId) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.sessionId = sessionId;
        }
    }
}
