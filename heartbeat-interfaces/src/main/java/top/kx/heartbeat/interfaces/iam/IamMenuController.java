package top.kx.heartbeat.interfaces.iam;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.PlatformAdministrationService;
import top.kx.heartbeat.application.platform.request.PlatformMenuRequest;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;

/**
 * 身份权限菜单接口控制器。
 *
 * <p>负责后台菜单、菜单树、路由和菜单维护入口。</p>
 */
@RestController
@RequestMapping("/api/v1/iam")
public class IamMenuController {

    /**
     * 平台管理应用服务。
     */
    @Resource
    private PlatformAdministrationService adminPlatformService;

    /**
     * 查询后台菜单列表。
     *
     * @return 后台菜单列表响应
     */
    @GetMapping("/menus")
    @PreAuthorize("@permissionGuard.has('system:menu:list')")
    public Result<List<DynamicRecordResponse>> menus() {
        // 查询后台菜单动态记录列表。
        List<RecordResponse> menus = adminPlatformService.listMenus();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(menus);
        // 返回后台菜单列表。
        return Result.success(response);
    }

    /**
     * 查询当前用户菜单树。
     *
     * @param userId 当前用户标识
     * @return 当前用户菜单树响应
     */
    @GetMapping("/menus/tree")
    public Result<List<DynamicRecordResponse>> menuTree(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        // 查询当前用户可访问路由动态记录列表。
        List<RecordResponse> routes = adminPlatformService.routes();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(routes);
        // 返回当前用户菜单树。
        return Result.success(response);
    }

    /**
     * 查询当前用户前端路由。
     *
     * @param userId 当前用户标识
     * @return 当前用户前端路由响应
     */
    @GetMapping("/routes")
    public Result<List<DynamicRecordResponse>> routes(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        // 查询当前用户可访问路由动态记录列表。
        List<RecordResponse> routes = adminPlatformService.routes();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(routes);
        // 返回当前用户前端路由。
        return Result.success(response);
    }

    /**
     * 查询菜单树选择器数据。
     *
     * @return 菜单树选择器响应
     */
    @GetMapping("/menus/tree-select")
    @PreAuthorize("@permissionGuard.has('system:menu:list')")
    public Result<List<DynamicRecordResponse>> treeSelect() {
        // 查询菜单树选择器动态记录列表。
        List<RecordResponse> tree = adminPlatformService.menuTreeSelect();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(tree);
        // 返回菜单树选择器数据。
        return Result.success(response);
    }

    /**
     * 新增后台菜单。
     *
     * @param request 菜单新增请求对象
     * @return 后台菜单新增结果
     */
    @PostMapping("/menus")
    @PreAuthorize("@permissionGuard.has('system:menu:add')")
    public Result<DynamicRecordResponse> create(@RequestBody PlatformMenuRequest request) {
        // 创建后台菜单动态记录。
        RecordResponse menu = adminPlatformService.createMenu(request);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(menu);
        // 返回后台菜单新增结果。
        return Result.success(response);
    }

    /**
     * 修改后台菜单。
     *
     * @param id 菜单标识
     * @param request 菜单修改请求对象
     * @return 后台菜单修改结果
     */
    @PutMapping("/menus/{id}")
    @PreAuthorize("@permissionGuard.has('system:menu:edit')")
    public Result<DynamicRecordResponse> update(@PathVariable String id,
                                                @RequestBody PlatformMenuRequest request) {
        // 修改后台菜单动态记录。
        RecordResponse menu = adminPlatformService.updateMenu(id, request);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(menu);
        // 返回后台菜单修改结果。
        return Result.success(response);
    }

    /**
     * 删除后台菜单。
     *
     * @param id 菜单标识
     * @return 删除结果
     */
    @DeleteMapping("/menus/{id}")
    @PreAuthorize("@permissionGuard.has('system:menu:remove')")
    public Result<Void> delete(@PathVariable String id) {
        // 删除后台菜单。
        adminPlatformService.deleteMenu(id);
        // 返回删除成功响应。
        return Result.success();
    }
}
