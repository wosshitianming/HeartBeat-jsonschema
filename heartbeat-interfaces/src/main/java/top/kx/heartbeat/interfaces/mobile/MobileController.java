package top.kx.heartbeat.interfaces.mobile;


import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.mobile.MobileService;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.request.DynamicRecordRequest;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 移动端管理接口控制器。
 *
 * <p>负责移动应用、页面和 API 路由的后台管理入口，统一将动态业务数据包装为规范响应对象。</p>
 */
@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {

    /**
     * 移动端应用服务。
     */
    @Resource
    private MobileService mobileService;

    /**
     * 查询移动应用列表。
     *
     * @return 移动应用列表响应
     */
    @GetMapping("/apps")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> apps() {
        // 查询移动应用动态记录列表。
        List<RecordResponse> apps = mobileService.listApps();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(apps);
        // 返回移动应用列表。
        return Result.success(response);
    }

    /**
     * 保存移动应用。
     *
     * @param request 移动应用保存参数
     * @return 移动应用保存结果
     */
    @PostMapping("/apps")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动应用")
    public Result<DynamicRecordResponse> saveApp(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存移动应用动态记录。
        RecordResponse app = mobileService.saveApp(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(app);
        // 返回移动应用保存结果。
        return Result.success(response);
    }

    /**
     * 查询移动应用页面列表。
     *
     * @param appId 移动应用标识
     * @return 移动页面列表响应
     */
    @GetMapping("/apps/{appId}/pages")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> pages(@PathVariable String appId) {
        // 查询指定应用下的页面动态记录列表。
        List<RecordResponse> pages = mobileService.listPages(appId);
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(pages);
        // 返回移动页面列表。
        return Result.success(response);
    }

    /**
     * 保存移动页面。
     *
     * @param request 移动页面保存参数
     * @return 移动页面保存结果
     */
    @PostMapping("/pages")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动页面")
    public Result<DynamicRecordResponse> savePage(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存移动页面动态记录。
        RecordResponse page = mobileService.savePage(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(page);
        // 返回移动页面保存结果。
        return Result.success(response);
    }

    /**
     * 查询移动应用 API 路由列表。
     *
     * @param appId 移动应用标识
     * @return 移动 API 路由列表响应
     */
    @GetMapping("/apps/{appId}/api-routes")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> apiRoutes(@PathVariable String appId) {
        // 查询指定应用下的 API 路由动态记录列表。
        List<RecordResponse> routes = mobileService.listApiRoutes(appId);
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromRecordList(routes);
        // 返回 API 路由列表。
        return Result.success(response);
    }

    /**
     * 保存移动 API 路由。
     *
     * @param request 移动 API 路由保存参数
     * @return 移动 API 路由保存结果
     */
    @PostMapping("/api-routes")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动 API 路由")
    public Result<DynamicRecordResponse> saveApiRoute(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存移动 API 路由动态记录。
        RecordResponse route = mobileService.saveApiRoute(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(route);
        // 返回移动 API 路由保存结果。
        return Result.success(response);
    }
}
