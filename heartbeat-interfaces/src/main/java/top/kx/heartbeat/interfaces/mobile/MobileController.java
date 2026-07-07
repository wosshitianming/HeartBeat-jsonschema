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

@RestController
@RequestMapping("/api/v1/mobile")
public class MobileController {

    @Resource
    private MobileService mobileService;

    @GetMapping("/apps")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> apps() {
        return listResponse(mobileService.listApps());
    }

    @PostMapping("/apps")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动应用")
    public Result<DynamicRecordResponse> saveApp(@RequestBody MobileAppRequest request) {
        return recordResponse(mobileService.saveApp(request));
    }

    @GetMapping("/apps/{appId}/pages")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> pages(@PathVariable String appId) {
        return listResponse(mobileService.listPages(appId));
    }

    @PostMapping("/pages")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动页面")
    public Result<DynamicRecordResponse> savePage(@RequestBody MobilePageRequest request) {
        return recordResponse(mobileService.savePage(request));
    }

    @GetMapping("/apps/{appId}/api-routes")
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> apiRoutes(@PathVariable String appId) {
        return listResponse(mobileService.listApiRoutes(appId));
    }

    @PostMapping("/api-routes")
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    @OperLog(module = "移动端", action = "保存移动 API 路由")
    public Result<DynamicRecordResponse> saveApiRoute(@RequestBody MobileApiRouteRequest request) {
        return recordResponse(mobileService.saveApiRoute(request));
    }

    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
