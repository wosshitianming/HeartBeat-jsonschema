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
import top.kx.heartbeat.infrastructure.persistence.entity.sys.SysUserDOExample;
import top.kx.heartbeat.infrastructure.persistence.mapper.sys.SysUserDOMapper;
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

    private static final long SESSION_TOUCH_INTERVAL_MINUTES = 5L;

    @Resource
    private JwtTokenService jwtTokenService;
    @Resource
    private PlatformPermissionRepository platformPermissionRepository;
    @Resource
    private AuthSessionRepository authSessionRepository;
    @Resource
    private SysUserDOMapper userMapper;

    @Value("${heartbeat.security.dev-auto-login:false}")
    private boolean devAutoLogin;

    @Value("${heartbeat.security.dev-header-enabled:false}")
    private boolean devHeaderEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                    HttpServletResponse response,
                                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                    FilterChain filterChain) throws ServletException, IOException {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        AuthenticatedSession session = resolveSession(request);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (session != null) {
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            TenantContext.setTenantId(session.tenantId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            request.setAttribute("heartbeatUserId", session.userId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            request.setAttribute("heartbeatTenantId", String.valueOf(session.tenantId));
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            request.setAttribute("heartbeatSessionId", session.sessionId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            SecurityContextHolder.getContext().setAuthentication(authenticationFor(session.userId));
        }

        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            filterChain.doFilter(request, response);
        } finally {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            SecurityContextHolder.clearContext();
        }
    }

    private UsernamePasswordAuthenticationToken authenticationFor(String userId) {
        // 创建结果集合，承接后续逐项组装的数据。
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 加入当前处理结果，供后续批量返回或继续组装。
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (String permission : platformPermissionRepository.listPermissionsByUserId(userId)) {
            // 根据当前业务条件选择对应处理路径。
            if (StringUtils.isNotBlank(permission)) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        // 返回已经完成封装的业务结果。
        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }

    private AuthenticatedSession resolveSession(HttpServletRequest request) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String authorization = request.getHeader("Authorization");
        // 根据当前业务条件选择对应处理路径。
        if (StringUtils.isNotBlank(authorization) && authorization.startsWith("Bearer ")) {
            // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
            String token = authorization.substring(7).trim();
            // 根据当前业务条件选择对应处理路径。
            if (StringUtils.isNotEmpty(token) && !token.startsWith("dev-")) {
                // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
                try {
                    // 计算当前步骤所需的中间值，供后续业务判断使用。
                    String userId = jwtTokenService.parseUserId(token);
                    // 提取第三方登录返回字段，后续用于绑定或创建本地用户。
                    long tenantId = jwtTokenService.parseTenantId(token);
                    // 计算当前步骤所需的中间值，供后续业务判断使用。
                    String sessionId = jwtTokenService.parseSessionId(token);
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    requireActiveSession(tenantId, userId, sessionId);
                    requireActiveUser(tenantId, userId);
                    // 返回已经完成封装的业务结果。
                    return new AuthenticatedSession(userId, tenantId, sessionId);
                } catch (RuntimeException ignored) {
                    // 返回已经完成封装的业务结果。
                    return null;
                }
            }
        }

        // 根据当前业务条件选择对应处理路径。
        if (devHeaderEnabled) {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String legacyUserId = request.getHeader("X-User-Id");
            // 根据当前业务条件选择对应处理路径。
            if (StringUtils.isNotBlank(legacyUserId)) {
                // 返回已经完成封装的业务结果。
                return new AuthenticatedSession(legacyUserId.trim(), 1L, "dev-header");
            }
        }
        // 根据当前业务条件选择对应处理路径。
        if (devAutoLogin) {
            // 返回已经完成封装的业务结果。
            return new AuthenticatedSession("1", 1L, "dev-auto-login");
        }
        // 返回已经完成封装的业务结果。
        return null;
    }

    private void requireActiveSession(long tenantId, String userId, String sessionId) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        LocalDateTime nowLdt = LocalDateTime.now();
        // 从仓储或 Mapper 读取业务数据，为后续处理准备上下文。
        AuthSession session = authSessionRepository.findActive(tenantId, sessionId)
                // 用 Optional 表达可缺省结果，让调用方显式处理不存在场景。
                .orElseThrow(() -> new IllegalArgumentException("Auth session is not active"));
        // 根据当前业务条件选择对应处理路径。
        if (session.getUserId() != Long.parseLong(userId) || !session.isActiveAt(nowLdt)) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalArgumentException("Auth session is not active");
        }
        // 补齐审计字段和默认值，保证新增与更新写入口径一致。
        LocalDateTime lastAccessAt = session.getLastAccessAt();
        if (lastAccessAt == null
                || !lastAccessAt.plusMinutes(SESSION_TOUCH_INTERVAL_MINUTES).isAfter(nowLdt)) {
            authSessionRepository.touch(tenantId, sessionId, nowLdt);
        }
    }

    private void requireActiveUser(long tenantId, String userId) {
        long id = Long.parseLong(userId);
        SysUserDOExample example = new SysUserDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andIdEqualTo(id)
                .andStatusEqualTo("ENABLED")
                .andDeleteMarkerEqualTo(0L);
        if (userMapper.countByExample(example) != 1L) {
            throw new IllegalArgumentException("User is not active");
        }
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
