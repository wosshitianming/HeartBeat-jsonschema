// 注释：声明当前文件所属的包路径。
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

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@RestController
// 注释：声明当前元素使用的注解配置。
@RequestMapping("/api/v1/iam")
public class IamMenuController {

    /**
     * 平台管理应用服务。
     */
    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformAdministrationService adminPlatformService;

    /**
     * 查询后台菜单列表。
     *
     * @return 后台菜单列表响应
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/menus")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('system:menu:list')")
    public Result<List<DynamicRecordResponse>> menus() {
        // 查询后台菜单动态记录列表。
        // 注释：设置或计算当前变量值。
        List<RecordResponse> menus = adminPlatformService.listMenus();
        // 将动态记录列表转换为统一响应对象列表。
        // 注释：设置或计算当前变量值。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(menus);
        // 返回后台菜单列表。
        // 注释：返回当前处理结果。
        return Result.success(response);
        // 注释：结束当前代码块。
    }

    /**
     * 查询当前用户菜单树。
     *
     * @param userId 当前用户标识
     * @return 当前用户菜单树响应
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/menus/tree")
    public Result<List<DynamicRecordResponse>> menuTree(
            // 注释：声明当前元素使用的注解配置。
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        // 查询当前用户可访问路由动态记录列表。
        // 注释：设置或计算当前变量值。
        List<RecordResponse> routes = adminPlatformService.routes();
        // 将动态记录列表转换为统一响应对象列表。
        // 注释：设置或计算当前变量值。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(routes);
        // 返回当前用户菜单树。
        // 注释：返回当前处理结果。
        return Result.success(response);
        // 注释：结束当前代码块。
    }

    /**
     * 查询当前用户前端路由。
     *
     * @param userId 当前用户标识
     * @return 当前用户前端路由响应
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/routes")
    public Result<List<DynamicRecordResponse>> routes(
            // 注释：声明当前元素使用的注解配置。
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        // 查询当前用户可访问路由动态记录列表。
        // 注释：设置或计算当前变量值。
        List<RecordResponse> routes = adminPlatformService.routes();
        // 将动态记录列表转换为统一响应对象列表。
        // 注释：设置或计算当前变量值。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(routes);
        // 返回当前用户前端路由。
        // 注释：返回当前处理结果。
        return Result.success(response);
        // 注释：结束当前代码块。
    }

    /**
     * 查询菜单树选择器数据。
     *
     * @return 菜单树选择器响应
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/menus/tree-select")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('system:menu:list')")
    public Result<List<DynamicRecordResponse>> treeSelect() {
        // 查询菜单树选择器动态记录列表。
        // 注释：设置或计算当前变量值。
        List<RecordResponse> tree = adminPlatformService.menuTreeSelect();
        // 将动态记录列表转换为统一响应对象列表。
        // 注释：设置或计算当前变量值。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(tree);
        // 返回菜单树选择器数据。
        // 注释：返回当前处理结果。
        return Result.success(response);
        // 注释：结束当前代码块。
    }

    /**
     * 新增后台菜单。
     *
     * @param request 菜单新增请求对象
     * @return 后台菜单新增结果
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/menus")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('system:menu:add')")
    public Result<DynamicRecordResponse> create(@RequestBody PlatformMenuRequest request) {
        // 创建后台菜单动态记录。
        // 注释：设置或计算当前变量值。
        RecordResponse menu = adminPlatformService.createMenu(request);
        // 将动态记录转换为统一响应对象。
        // 注释：设置或计算当前变量值。
        DynamicRecordResponse response = DynamicRecordResponse.from(menu);
        // 返回后台菜单新增结果。
        // 注释：返回当前处理结果。
        return Result.success(response);
        // 注释：结束当前代码块。
    }

    /**
     * 修改后台菜单。
     *
     * @param id 菜单标识
     * @param request 菜单修改请求对象
     * @return 后台菜单修改结果
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/menus/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('system:menu:edit')")
    public Result<DynamicRecordResponse> update(@PathVariable String id,
                                                // 注释：声明当前元素使用的注解配置。
                                                @RequestBody PlatformMenuRequest request) {
        // 修改后台菜单动态记录。
        // 注释：设置或计算当前变量值。
        RecordResponse menu = adminPlatformService.updateMenu(id, request);
        // 将动态记录转换为统一响应对象。
        // 注释：设置或计算当前变量值。
        DynamicRecordResponse response = DynamicRecordResponse.from(menu);
        // 返回后台菜单修改结果。
        // 注释：返回当前处理结果。
        return Result.success(response);
        // 注释：结束当前代码块。
    }

    /**
     * 删除后台菜单。
     *
     * @param id 菜单标识
     * @return 删除结果
     */
    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @DeleteMapping("/menus/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('system:menu:remove')")
    public Result<Void> delete(@PathVariable String id) {
        // 删除后台菜单。
        // 注释：执行当前代码行。
        adminPlatformService.deleteMenu(id);
        // 返回删除成功响应。
        // 注释：返回当前处理结果。
        return Result.success();
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
