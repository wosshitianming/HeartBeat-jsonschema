package top.kx.heartbeat.application.admin;


import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.admin.dto.AdminMetricDTO;
import top.kx.heartbeat.application.admin.dto.AdminModuleDTO;
import top.kx.heartbeat.application.common.response.RecordResponse;
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
        flattenMenus(recordsToMaps(adminPlatformService.routes()), modules);
        return modules;
    }

    public AdminModuleDTO getModule(String key) {
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (AdminModuleDTO module : listModules()) {
            // 比对当前业务状态，决定是否进入该处理分支。
            if (module.getKey().equals(key)) {
                // 返回已经完成封装的业务结果。
                return module;
            }
        }
        // 对非法业务状态立即失败，避免错误继续扩散。
        throw new IllegalArgumentException("系统管理模块不存在: " + key);
    }

    @SuppressWarnings("unchecked")
    private void flattenMenus(List<Map<String, Object>> menus, List<AdminModuleDTO> target) {
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map<String, Object> menu : menus) {
            // 菜单类型由枚举统一判断，避免裸字符串散落。
            if (PlatformMenuType.MENU.matches(stringValue(menu.get("type")))) {
                // 计算当前步骤所需的中间值，供后续业务判断使用。
                String id = stringValue(menu.get("id"));
                // 计算当前步骤所需的中间值，供后续业务判断使用。
                String name = stringValue(menu.get("name"));
                // 计算当前步骤所需的中间值，供后续业务判断使用。
                String permission = stringValue(menu.get("permission"));
                // 计算当前步骤所需的中间值，供后续业务判断使用。
                String status = stringValue(menu.get("status"));
                // 加入当前处理结果，供后续批量返回或继续组装。
                target.add(new AdminModuleDTO(
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        id,
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        name,
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        categoryOf(permission),
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        name + "由数据库菜单与权限配置动态生成",
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        permission,
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        status,
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        Collections.emptyList(),
                        // 创建当前流程需要的临时对象，承载后续处理数据。
                        Collections.singletonList(new AdminMetricDTO(
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                "权限标识",
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                permission,
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                "来源：sys_menu"
                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        )),
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        Arrays.asList("名称", "权限标识", "状态"),
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        Collections.emptyList()
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                ));
            }
            // 计算当前分支的中间结果，供后续判断或组装使用。
            Object children = menu.get("children");
            // 根据当前业务条件选择对应处理路径。
            if (children instanceof List) {
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
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

    private List<Map<String, Object>> recordsToMaps(List<RecordResponse> records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RecordResponse record : records) {
            result.add(record.toMap());
        }
        return result;
    }
}
