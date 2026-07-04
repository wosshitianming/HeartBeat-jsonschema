package top.kx.heartbeat.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.security.PermissionAction;
import top.kx.heartbeat.domain.security.PermissionResource;

@Component("permissionGuard")
public class PermissionGuard {

    public boolean hasResource(String resource, String action) {
        // 通过枚举解析资源，避免散落的权限前缀字符串。
        PermissionResource permissionResource = PermissionResource.fromResource(resource);
        if (permissionResource == null) {
            return false;
        }
        // 通过枚举解析动作，统一 create/update/delete/list 的权限后缀。
        PermissionAction permissionAction = PermissionAction.fromResourceAction(action);
        // 拼出最终权限编码后复用统一权限判断。
        return has(permissionResource.permissionOf(permissionAction));
    }

    public boolean has(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if ("1".equals(String.valueOf(authentication.getPrincipal()))) {
            return true;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (permission.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
