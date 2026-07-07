package top.kx.heartbeat.interfaces.mobile;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.mobile.MobileService;
import top.kx.heartbeat.application.mobile.request.MobileApiRouteRequest;
import top.kx.heartbeat.application.mobile.request.MobileAppRequest;
import top.kx.heartbeat.application.mobile.request.MobilePageRequest;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;

/**
 * 提供移动端配置 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {

    @Resource
    private MobileService mobileService;

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托移动端配置应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/apps")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> apps() {
        return listResponse(mobileService.listApps());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托移动端配置应用服务完成处理。
     *
     * @param request 移动端配置请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/apps")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动应用")
    public Result<DynamicRecordResponse> saveApp(@RequestBody MobileAppRequest request) {
        return recordResponse(mobileService.saveApp(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托移动端配置应用服务完成处理。
     *
     * @param appId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/apps/{appId}/pages")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> pages(@PathVariable String appId) {
        return listResponse(mobileService.listPages(appId));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托移动端配置应用服务完成处理。
     *
     * @param request 移动端配置请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/pages")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动页面")
    public Result<DynamicRecordResponse> savePage(@RequestBody MobilePageRequest request) {
        return recordResponse(mobileService.savePage(request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托移动端配置应用服务完成处理。
     *
     * @param appId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/apps/{appId}/api-routes")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> apiRoutes(@PathVariable String appId) {
        return listResponse(mobileService.listApiRoutes(appId));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托移动端配置应用服务完成处理。
     *
     * @param request 移动端配置请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/api-routes")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动 API 路由")
    public Result<DynamicRecordResponse> saveApiRoute(@RequestBody MobileApiRouteRequest request) {
        return recordResponse(mobileService.saveApiRoute(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托移动端配置应用服务完成处理。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托移动端配置应用服务完成处理。
     *
     * @param record 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
