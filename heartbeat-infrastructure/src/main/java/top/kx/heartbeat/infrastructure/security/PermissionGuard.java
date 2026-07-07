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
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (authentication == null || !authentication.isAuthenticated()) {
            // 返回已经完成封装的业务结果。
            return false;
        }
        // 比对当前业务状态，决定是否进入该处理分支。
        if ("1".equals(String.valueOf(authentication.getPrincipal()))) {
            // 返回已经完成封装的业务结果。
            return true;
        }
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            // 比对当前业务状态，决定是否进入该处理分支。
            if (permission.equals(authority.getAuthority())) {
                // 返回已经完成封装的业务结果。
                return true;
            }
        }
        // 返回已经完成封装的业务结果。
        return false;
    }
}
