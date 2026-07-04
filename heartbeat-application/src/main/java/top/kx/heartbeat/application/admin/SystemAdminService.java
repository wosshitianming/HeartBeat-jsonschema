package top.kx.heartbeat.application.admin;


import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.admin.dto.AdminMetricDTO;
import top.kx.heartbeat.application.admin.dto.AdminModuleDTO;
import top.kx.heartbeat.application.platform.PlatformAdministrationService;
import top.kx.heartbeat.domain.platform.PlatformMenuType;
import top.kx.heartbeat.domain.security.PermissionCategory;

import javax.annotation.Resource;
import java.util.*;

/**
 * Builds the legacy module view from the authenticated user's persisted menus.
 */
@Service
public class SystemAdminService {

    @Resource
    private PlatformAdministrationService adminPlatformService;

    public List<AdminModuleDTO> listModules() {
        List<AdminModuleDTO> modules = new ArrayList<>();
        flattenMenus(adminPlatformService.routes(), modules);
        return modules;
    }

    public AdminModuleDTO getModule(String key) {
        for (AdminModuleDTO module : listModules()) {
            if (module.getKey().equals(key)) {
                return module;
            }
        }
        throw new IllegalArgumentException("系统管理模块不存在: " + key);
    }

    @SuppressWarnings("unchecked")
    private void flattenMenus(List<Map<String, Object>> menus, List<AdminModuleDTO> target) {
        for (Map<String, Object> menu : menus) {
            // 菜单类型由枚举统一判断，避免裸字符串散落。
            if (PlatformMenuType.MENU.matches(stringValue(menu.get("type")))) {
                String id = stringValue(menu.get("id"));
                String name = stringValue(menu.get("name"));
                String permission = stringValue(menu.get("permission"));
                String status = stringValue(menu.get("status"));
                target.add(new AdminModuleDTO(
                        id,
                        name,
                        categoryOf(permission),
                        name + "由数据库菜单与权限配置动态生成",
                        permission,
                        status,
                        Collections.emptyList(),
                        Collections.singletonList(new AdminMetricDTO(
                                "权限标识",
                                permission,
                                "来源：sys_menu"
                        )),
                        Arrays.asList("名称", "权限标识", "状态"),
                        Collections.emptyList()
                ));
            }
            Object children = menu.get("children");
            if (children instanceof List) {
                flattenMenus((List<Map<String, Object>>) children, target);
            }
        }
    }

    private String categoryOf(String permission) {
        // 使用枚举集中维护权限前缀与菜单分类的映射关系。
        return PermissionCategory.resolve(permission).getLabel();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
