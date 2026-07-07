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
 * 提供权限菜单 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/iam")
public class IamMenuController {

    @Resource
    private PlatformAdministrationService adminPlatformService;

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托权限菜单应用服务完成处理。
     *
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托权限菜单应用服务完成处理。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托权限菜单应用服务完成处理。
     *
     * @param userId 业务记录标识。
     * @return 处理后的业务结果。
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
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托权限菜单应用服务完成处理。
     *
     * @return 处理后的业务结果。
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
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托权限菜单应用服务完成处理。
     *
     * @param request 权限菜单请求参数。
     * @return 处理后的业务结果。
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
     * 更新业务记录，只处理调用方传入的可变字段，并统一委托权限菜单应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 权限菜单请求参数。
     * @return 处理后的业务结果。
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
     * 删除业务记录，并向上层屏蔽底层存储细节，并统一委托权限菜单应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
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
