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

@RestController
@RequestMapping("/api/v1/admin/resources")
public class AdminResourceController {

    @Resource
    private PlatformAdministrationService platformAdministrationService;

    @GetMapping("/users")
    @PreAuthorize("@permissionGuard.hasResource('users', 'list')")
    public Result<List<DynamicRecordResponse>> listUsers() {
        return listResponse(platformAdministrationService.listUsers());
    }

    @PostMapping("/users")
    @PreAuthorize("@permissionGuard.hasResource('users', 'create')")
    public Result<DynamicRecordResponse> createUser(@RequestBody PlatformUserRequest request) {
        return recordResponse(platformAdministrationService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("@permissionGuard.hasResource('users', 'update')")
    public Result<DynamicRecordResponse> updateUser(@PathVariable String id,
                                                    @RequestBody PlatformUserRequest request) {
        return recordResponse(platformAdministrationService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("@permissionGuard.hasResource('users', 'delete')")
    public Result<Void> deleteUser(@PathVariable String id) {
        platformAdministrationService.deleteUser(id);
        return Result.success();
    }

    @GetMapping("/depts")
    @PreAuthorize("@permissionGuard.hasResource('depts', 'list')")
    public Result<List<DynamicRecordResponse>> listDepartments() {
        return listResponse(platformAdministrationService.listDepartments());
    }

    @PostMapping("/depts")
    @PreAuthorize("@permissionGuard.hasResource('depts', 'create')")
    public Result<DynamicRecordResponse> createDepartment(@RequestBody PlatformDepartmentRequest request) {
        return recordResponse(platformAdministrationService.createDepartment(request));
    }

    @PutMapping("/depts/{id}")
    @PreAuthorize("@permissionGuard.hasResource('depts', 'update')")
    public Result<DynamicRecordResponse> updateDepartment(@PathVariable String id,
                                                          @RequestBody PlatformDepartmentRequest request) {
        return recordResponse(platformAdministrationService.updateDepartment(id, request));
    }

    @DeleteMapping("/depts/{id}")
    @PreAuthorize("@permissionGuard.hasResource('depts', 'delete')")
    public Result<Void> deleteDepartment(@PathVariable String id) {
        platformAdministrationService.deleteDepartment(id);
        return Result.success();
    }

    @GetMapping("/roles")
    @PreAuthorize("@permissionGuard.hasResource('roles', 'list')")
    public Result<List<DynamicRecordResponse>> listRoles() {
        return listResponse(platformAdministrationService.listRoles());
    }

    @PostMapping("/roles")
    @PreAuthorize("@permissionGuard.hasResource('roles', 'create')")
    public Result<DynamicRecordResponse> createRole(@RequestBody PlatformRoleRequest request) {
        return recordResponse(platformAdministrationService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("@permissionGuard.hasResource('roles', 'update')")
    public Result<DynamicRecordResponse> updateRole(@PathVariable String id,
                                                    @RequestBody PlatformRoleRequest request) {
        return recordResponse(platformAdministrationService.updateRole(id, request));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("@permissionGuard.hasResource('roles', 'delete')")
    public Result<Void> deleteRole(@PathVariable String id) {
        platformAdministrationService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/configs")
    @PreAuthorize("@permissionGuard.hasResource('configs', 'list')")
    public Result<List<DynamicRecordResponse>> listConfigurations() {
        return listResponse(platformAdministrationService.listConfigurations());
    }

    @PostMapping("/configs")
    @PreAuthorize("@permissionGuard.hasResource('configs', 'create')")
    public Result<DynamicRecordResponse> createConfiguration(@RequestBody PlatformConfigurationRequest request) {
        return recordResponse(platformAdministrationService.createConfiguration(request));
    }

    @PutMapping("/configs/{id}")
    @PreAuthorize("@permissionGuard.hasResource('configs', 'update')")
    public Result<DynamicRecordResponse> updateConfiguration(@PathVariable String id,
                                                             @RequestBody PlatformConfigurationRequest request) {
        return recordResponse(platformAdministrationService.updateConfiguration(id, request));
    }

    @DeleteMapping("/configs/{id}")
    @PreAuthorize("@permissionGuard.hasResource('configs', 'delete')")
    public Result<Void> deleteConfiguration(@PathVariable String id) {
        platformAdministrationService.deleteConfiguration(id);
        return Result.success();
    }

    @GetMapping("/social-providers")
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'list')")
    public Result<List<DynamicRecordResponse>> listSocialProviders() {
        return listResponse(platformAdministrationService.listSocialProviders());
    }

    @PostMapping("/social-providers")
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'create')")
    public Result<DynamicRecordResponse> createSocialProvider(@RequestBody PlatformSocialProviderRequest request) {
        return recordResponse(platformAdministrationService.createSocialProvider(request));
    }

    @PutMapping("/social-providers/{id}")
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'update')")
    public Result<DynamicRecordResponse> updateSocialProvider(@PathVariable String id,
                                                              @RequestBody PlatformSocialProviderRequest request) {
        return recordResponse(platformAdministrationService.updateSocialProvider(id, request));
    }

    @DeleteMapping("/social-providers/{id}")
    @PreAuthorize("@permissionGuard.hasResource('social-providers', 'delete')")
    public Result<Void> deleteSocialProvider(@PathVariable String id) {
        platformAdministrationService.deleteSocialProvider(id);
        return Result.success();
    }

    @GetMapping("/login-logs")
    @PreAuthorize("@permissionGuard.hasResource('login-logs', 'list')")
    public Result<List<DynamicRecordResponse>> listLoginLogs() {
        return listResponse(platformAdministrationService.listLoginLogs());
    }

    @GetMapping("/tenants")
    @PreAuthorize("@permissionGuard.hasResource('tenants', 'list')")
    public Result<List<DynamicRecordResponse>> listTenants() {
        return listResponse(platformAdministrationService.listTenants());
    }

    @GetMapping("/posts")
    @PreAuthorize("@permissionGuard.hasResource('posts', 'list')")
    public Result<List<DynamicRecordResponse>> listPosts() {
        return listResponse(platformAdministrationService.listPosts());
    }

    @GetMapping("/dict-types")
    @PreAuthorize("@permissionGuard.hasResource('dict-types', 'list')")
    public Result<List<DynamicRecordResponse>> listDictTypes() {
        return listResponse(platformAdministrationService.listDictTypes());
    }

    @GetMapping("/dict-data")
    @PreAuthorize("@permissionGuard.hasResource('dict-data', 'list')")
    public Result<List<DynamicRecordResponse>> listDictData() {
        return listResponse(platformAdministrationService.listDictData());
    }

    @GetMapping("/notices")
    @PreAuthorize("@permissionGuard.hasResource('notices', 'list')")
    public Result<List<DynamicRecordResponse>> listNotices() {
        return listResponse(platformAdministrationService.listNotices());
    }

    @GetMapping("/oper-logs")
    @PreAuthorize("@permissionGuard.hasResource('oper-logs', 'list')")
    public Result<List<DynamicRecordResponse>> listOperationLogs() {
        return listResponse(platformAdministrationService.listOperationLogs());
    }

    @GetMapping("/online-sessions")
    @PreAuthorize("@permissionGuard.hasResource('online-sessions', 'list')")
    public Result<List<DynamicRecordResponse>> listOnlineSessions() {
        return listResponse(platformAdministrationService.listOnlineSessions());
    }

    @GetMapping("/oauth-clients")
    @PreAuthorize("@permissionGuard.hasResource('oauth-clients', 'list')")
    public Result<List<DynamicRecordResponse>> listOauthClients() {
        return listResponse(platformAdministrationService.listOauthClients());
    }

    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }

}
