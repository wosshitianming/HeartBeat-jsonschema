package top.kx.heartbeat.interfaces.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.kx.heartbeat.application.admin.SystemAdminService;
import top.kx.heartbeat.application.admin.dto.AdminModuleDTO;
import top.kx.heartbeat.interfaces.common.Result;

import javax.annotation.Resource;
import java.util.List;

/**
 * 系统管理首页接口控制器。
 *
 * <p>负责系统管理模块导航和模块摘要数据的 HTTP 协议适配。</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class SystemAdminController {

    /**
     * 系统管理应用服务。
     */
    @Resource
    private SystemAdminService systemAdminService;

    /**
     * 查询系统管理模块列表。
     *
     * @return 系统管理模块列表响应
     */
    @GetMapping("/modules")
    @PreAuthorize("@permissionGuard.has('dashboard:view')")
    public Result<List<AdminModuleDTO>> modules() {
        // 查询系统管理模块列表。
        List<AdminModuleDTO> modules = systemAdminService.listModules();
        // 返回系统管理模块列表。
        return Result.success(modules);
    }

    /**
     * 查询系统管理模块详情。
     *
     * @param key 系统管理模块标识
     * @return 系统管理模块详情响应
     */
    @GetMapping("/modules/{key}")
    @PreAuthorize("@permissionGuard.has('dashboard:view')")
    public Result<AdminModuleDTO> module(@PathVariable String key) {
        // 查询系统管理模块详情。
        AdminModuleDTO module = systemAdminService.getModule(key);
        // 返回系统管理模块详情。
        return Result.success(module);
    }
}
