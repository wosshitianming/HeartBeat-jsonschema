package top.kx.heartbeat.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;

/**
 * Reads the current user id from Spring Security.
 */
@Component
public class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            return "1";
        }
        return String.valueOf(authentication.getPrincipal());
    }
}
