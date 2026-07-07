// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.interfaces.admin;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.platform.PlatformAdministrationService;
import top.kx.heartbeat.application.platform.request.*;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@RestController
// 注释：声明当前元素使用的注解配置。
@RequestMapping("/api/v1/admin/resources")
public class AdminResourceController {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private PlatformAdministrationService platformAdministrationService;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/users")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('users', 'list')")
    public Result<List<DynamicRecordResponse>> listUsers() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listUsers());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/users")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('users', 'create')")
    public Result<DynamicRecordResponse> createUser(@RequestBody PlatformUserRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.createUser(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/users/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('users', 'update')")
    public Result<DynamicRecordResponse> updateUser(@PathVariable String id,
                                                    // 注释：声明当前元素使用的注解配置。
                                                    @RequestBody PlatformUserRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.updateUser(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @DeleteMapping("/users/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('users', 'delete')")
    public Result<Void> deleteUser(@PathVariable String id) {
        // 注释：执行当前代码行。
        platformAdministrationService.deleteUser(id);
        // 注释：返回当前处理结果。
        return Result.success();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/depts")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('depts', 'list')")
    public Result<List<DynamicRecordResponse>> listDepartments() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listDepartments());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/depts")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('depts', 'create')")
    public Result<DynamicRecordResponse> createDepartment(@RequestBody PlatformDepartmentRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.createDepartment(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/depts/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('depts', 'update')")
    public Result<DynamicRecordResponse> updateDepartment(@PathVariable String id,
                                                          // 注释：声明当前元素使用的注解配置。
                                                          @RequestBody PlatformDepartmentRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.updateDepartment(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @DeleteMapping("/depts/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('depts', 'delete')")
    public Result<Void> deleteDepartment(@PathVariable String id) {
        // 注释：执行当前代码行。
        platformAdministrationService.deleteDepartment(id);
        // 注释：返回当前处理结果。
        return Result.success();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/roles")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('roles', 'list')")
    public Result<List<DynamicRecordResponse>> listRoles() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listRoles());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/roles")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('roles', 'create')")
    public Result<DynamicRecordResponse> createRole(@RequestBody PlatformRoleRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.createRole(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/roles/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('roles', 'update')")
    public Result<DynamicRecordResponse> updateRole(@PathVariable String id,
                                                    // 注释：声明当前元素使用的注解配置。
                                                    @RequestBody PlatformRoleRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.updateRole(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @DeleteMapping("/roles/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('roles', 'delete')")
    public Result<Void> deleteRole(@PathVariable String id) {
        // 注释：执行当前代码行。
        platformAdministrationService.deleteRole(id);
        // 注释：返回当前处理结果。
        return Result.success();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/configs")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('configs', 'list')")
    public Result<List<DynamicRecordResponse>> listConfigurations() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listConfigurations());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/configs")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('configs', 'create')")
    public Result<DynamicRecordResponse> createConfiguration(@RequestBody PlatformConfigurationRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.createConfiguration(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/configs/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('configs', 'update')")
    public Result<DynamicRecordResponse> updateConfiguration(@PathVariable String id,
                                                             // 注释：声明当前元素使用的注解配置。
                                                             @RequestBody PlatformConfigurationRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.updateConfiguration(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @DeleteMapping("/configs/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('configs', 'delete')")
    public Result<Void> deleteConfiguration(@PathVariable String id) {
        // 注释：执行当前代码行。
        platformAdministrationService.deleteConfiguration(id);
        // 注释：返回当前处理结果。
        return Result.success();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/social-providers")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'list')")
    public Result<List<DynamicRecordResponse>> listSocialProviders() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listSocialProviders());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/social-providers")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'create')")
    public Result<DynamicRecordResponse> createSocialProvider(@RequestBody PlatformSocialProviderRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.createSocialProvider(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PutMapping("/social-providers/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'update')")
    public Result<DynamicRecordResponse> updateSocialProvider(@PathVariable String id,
                                                              // 注释：声明当前元素使用的注解配置。
                                                              @RequestBody PlatformSocialProviderRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(platformAdministrationService.updateSocialProvider(id, request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @DeleteMapping("/social-providers/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'delete')")
    public Result<Void> deleteSocialProvider(@PathVariable String id) {
        // 注释：执行当前代码行。
        platformAdministrationService.deleteSocialProvider(id);
        // 注释：返回当前处理结果。
        return Result.success();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/login-logs")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('login-logs', 'list')")
    public Result<List<DynamicRecordResponse>> listLoginLogs() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listLoginLogs());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/tenants")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('tenants', 'list')")
    public Result<List<DynamicRecordResponse>> listTenants() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listTenants());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/posts")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('posts', 'list')")
    public Result<List<DynamicRecordResponse>> listPosts() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listPosts());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/dict-types")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('dict-types', 'list')")
    public Result<List<DynamicRecordResponse>> listDictTypes() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listDictTypes());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/dict-data")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('dict-data', 'list')")
    public Result<List<DynamicRecordResponse>> listDictData() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listDictData());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/notices")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('notices', 'list')")
    public Result<List<DynamicRecordResponse>> listNotices() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listNotices());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/oper-logs")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('oper-logs', 'list')")
    public Result<List<DynamicRecordResponse>> listOperationLogs() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listOperationLogs());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/online-sessions")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('online-sessions', 'list')")
    public Result<List<DynamicRecordResponse>> listOnlineSessions() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listOnlineSessions());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/oauth-clients")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.hasResource('oauth-clients', 'list')")
    public Result<List<DynamicRecordResponse>> listOauthClients() {
        // 注释：返回当前处理结果。
        return listResponse(platformAdministrationService.listOauthClients());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.fromRecordList(records));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.from(record));
        // 注释：结束当前代码块。
    }

// 注释：结束当前代码块。
}
