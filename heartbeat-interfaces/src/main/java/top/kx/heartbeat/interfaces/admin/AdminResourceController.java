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
 * 提供后台资源管理 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/admin/resources")
public class AdminResourceController {

    @Resource
    private PlatformAdministrationService platformAdministrationService;

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/users")
    @PreAuthorize("@permissionGuard.hasResource('users', 'list')")
    public Result<List<DynamicRecordResponse>> listUsers() {
        return listResponse(platformAdministrationService.listUsers());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托后台资源管理应用服务完成处理。
     *
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/users")
    @PreAuthorize("@permissionGuard.hasResource('users', 'create')")
    public Result<DynamicRecordResponse> createUser(@RequestBody PlatformUserRequest request) {
        return recordResponse(platformAdministrationService.createUser(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("@permissionGuard.hasResource('users', 'update')")
    public Result<DynamicRecordResponse> updateUser(@PathVariable String id,
                                                    @RequestBody PlatformUserRequest request) {
        return recordResponse(platformAdministrationService.updateUser(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("@permissionGuard.hasResource('users', 'delete')")
    public Result<Void> deleteUser(@PathVariable String id) {
        platformAdministrationService.deleteUser(id);
        return Result.success();
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/depts")
    @PreAuthorize("@permissionGuard.hasResource('depts', 'list')")
    public Result<List<DynamicRecordResponse>> listDepartments() {
        return listResponse(platformAdministrationService.listDepartments());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托后台资源管理应用服务完成处理。
     *
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/depts")
    @PreAuthorize("@permissionGuard.hasResource('depts', 'create')")
    public Result<DynamicRecordResponse> createDepartment(@RequestBody PlatformDepartmentRequest request) {
        return recordResponse(platformAdministrationService.createDepartment(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PutMapping("/depts/{id}")
    @PreAuthorize("@permissionGuard.hasResource('depts', 'update')")
    public Result<DynamicRecordResponse> updateDepartment(@PathVariable String id,
                                                          @RequestBody PlatformDepartmentRequest request) {
        return recordResponse(platformAdministrationService.updateDepartment(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @DeleteMapping("/depts/{id}")
    @PreAuthorize("@permissionGuard.hasResource('depts', 'delete')")
    public Result<Void> deleteDepartment(@PathVariable String id) {
        platformAdministrationService.deleteDepartment(id);
        return Result.success();
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/roles")
    @PreAuthorize("@permissionGuard.hasResource('roles', 'list')")
    public Result<List<DynamicRecordResponse>> listRoles() {
        return listResponse(platformAdministrationService.listRoles());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托后台资源管理应用服务完成处理。
     *
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/roles")
    @PreAuthorize("@permissionGuard.hasResource('roles', 'create')")
    public Result<DynamicRecordResponse> createRole(@RequestBody PlatformRoleRequest request) {
        return recordResponse(platformAdministrationService.createRole(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PutMapping("/roles/{id}")
    @PreAuthorize("@permissionGuard.hasResource('roles', 'update')")
    public Result<DynamicRecordResponse> updateRole(@PathVariable String id,
                                                    @RequestBody PlatformRoleRequest request) {
        return recordResponse(platformAdministrationService.updateRole(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @DeleteMapping("/roles/{id}")
    @PreAuthorize("@permissionGuard.hasResource('roles', 'delete')")
    public Result<Void> deleteRole(@PathVariable String id) {
        platformAdministrationService.deleteRole(id);
        return Result.success();
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/configs")
    @PreAuthorize("@permissionGuard.hasResource('configs', 'list')")
    public Result<List<DynamicRecordResponse>> listConfigurations() {
        return listResponse(platformAdministrationService.listConfigurations());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托后台资源管理应用服务完成处理。
     *
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/configs")
    @PreAuthorize("@permissionGuard.hasResource('configs', 'create')")
    public Result<DynamicRecordResponse> createConfiguration(@RequestBody PlatformConfigurationRequest request) {
        return recordResponse(platformAdministrationService.createConfiguration(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PutMapping("/configs/{id}")
    @PreAuthorize("@permissionGuard.hasResource('configs', 'update')")
    public Result<DynamicRecordResponse> updateConfiguration(@PathVariable String id,
                                                             @RequestBody PlatformConfigurationRequest request) {
        return recordResponse(platformAdministrationService.updateConfiguration(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @DeleteMapping("/configs/{id}")
    @PreAuthorize("@permissionGuard.hasResource('configs', 'delete')")
    public Result<Void> deleteConfiguration(@PathVariable String id) {
        platformAdministrationService.deleteConfiguration(id);
        return Result.success();
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/social-providers")
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'list')")
    public Result<List<DynamicRecordResponse>> listSocialProviders() {
        return listResponse(platformAdministrationService.listSocialProviders());
    }

    /**
     * 创建业务记录，并补齐持久化所需的默认数据，并统一委托后台资源管理应用服务完成处理。
     *
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/social-providers")
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'create')")
    public Result<DynamicRecordResponse> createSocialProvider(@RequestBody PlatformSocialProviderRequest request) {
        return recordResponse(platformAdministrationService.createSocialProvider(request));
    }

    /**
     * 更新业务记录，只处理调用方传入的可变字段，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 后台资源管理请求参数。
     * @return 处理后的业务结果。
     */
    @PutMapping("/social-providers/{id}")
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'update')")
    public Result<DynamicRecordResponse> updateSocialProvider(@PathVariable String id,
                                                              @RequestBody PlatformSocialProviderRequest request) {
        return recordResponse(platformAdministrationService.updateSocialProvider(id, request));
    }

    /**
     * 删除业务记录，并向上层屏蔽底层存储细节，并统一委托后台资源管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @DeleteMapping("/social-providers/{id}")
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'delete')")
    public Result<Void> deleteSocialProvider(@PathVariable String id) {
        platformAdministrationService.deleteSocialProvider(id);
        return Result.success();
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/login-logs")
    @PreAuthorize("@permissionGuard.hasResource('login-logs', 'list')")
    public Result<List<DynamicRecordResponse>> listLoginLogs() {
        return listResponse(platformAdministrationService.listLoginLogs());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/tenants")
    @PreAuthorize("@permissionGuard.hasResource('tenants', 'list')")
    public Result<List<DynamicRecordResponse>> listTenants() {
        return listResponse(platformAdministrationService.listTenants());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/posts")
    @PreAuthorize("@permissionGuard.hasResource('posts', 'list')")
    public Result<List<DynamicRecordResponse>> listPosts() {
        return listResponse(platformAdministrationService.listPosts());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/dict-types")
    @PreAuthorize("@permissionGuard.hasResource('dict-types', 'list')")
    public Result<List<DynamicRecordResponse>> listDictTypes() {
        return listResponse(platformAdministrationService.listDictTypes());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/dict-data")
    @PreAuthorize("@permissionGuard.hasResource('dict-data', 'list')")
    public Result<List<DynamicRecordResponse>> listDictData() {
        return listResponse(platformAdministrationService.listDictData());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/notices")
    @PreAuthorize("@permissionGuard.hasResource('notices', 'list')")
    public Result<List<DynamicRecordResponse>> listNotices() {
        return listResponse(platformAdministrationService.listNotices());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/oper-logs")
    @PreAuthorize("@permissionGuard.hasResource('oper-logs', 'list')")
    public Result<List<DynamicRecordResponse>> listOperationLogs() {
        return listResponse(platformAdministrationService.listOperationLogs());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/online-sessions")
    @PreAuthorize("@permissionGuard.hasResource('online-sessions', 'list')")
    public Result<List<DynamicRecordResponse>> listOnlineSessions() {
        return listResponse(platformAdministrationService.listOnlineSessions());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/oauth-clients")
    @PreAuthorize("@permissionGuard.hasResource('oauth-clients', 'list')")
    public Result<List<DynamicRecordResponse>> listOauthClients() {
        return listResponse(platformAdministrationService.listOauthClients());
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托后台资源管理应用服务完成处理。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托后台资源管理应用服务完成处理。
     *
     * @param record 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }

}
