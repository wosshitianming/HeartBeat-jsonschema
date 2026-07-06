package top.kx.heartbeat.interfaces.iam;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.PlatformAdministrationService;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;
import top.kx.heartbeat.interfaces.iam.request.AssignRoleMenusRequest;

import javax.annotation.Resource;

/**
 * 身份权限角色接口控制器。
 *
 * <p>负责角色菜单查询和角色菜单授权入口。</p>
 */
@RestController
@RequestMapping("/api/v1/iam/roles")
public class IamRoleController {

    /**
     * 平台管理应用服务。
     */
    @Resource
    private PlatformAdministrationService adminPlatformService;

    /**
     * 查询角色菜单详情。
     *
     * @param roleId 角色标识
     * @return 角色菜单详情响应
     */
    @GetMapping("/{id}/menus")
    @PreAuthorize("@permissionGuard.has('system:role:list')")
    public Result<DynamicRecordResponse> roleMenus(@PathVariable("id") String roleId) {
        // 查询角色菜单动态详情。
        RecordResponse detail = adminPlatformService.roleMenuDetail(roleId);
        // 将动态详情转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(detail);
        // 返回角色菜单详情。
        return Result.success(response);
    }

    /**
     * 分配角色菜单。
     *
     * @param roleId 角色标识
     * @param request 角色菜单分配请求对象
     * @return 分配结果
     */
    @PutMapping("/{id}/menus")
    @PreAuthorize("@permissionGuard.has('system:role:grant')")
    public Result<Void> assignRoleMenus(@PathVariable("id") String roleId,
                                        @RequestBody AssignRoleMenusRequest request) {
        // 分配角色菜单关系。
        adminPlatformService.assignRoleMenus(roleId, request.getMenuIds());
        // 返回分配成功响应。
        return Result.success();
    }
}
