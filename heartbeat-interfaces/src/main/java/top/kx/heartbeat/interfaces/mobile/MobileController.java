// 注释：声明当前文件所属的包路径。
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
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@RestController
// 注释：声明当前元素使用的注解配置。
@RequestMapping("/api/v1/mobile")
public class MobileController {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MobileService mobileService;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/apps")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> apps() {
        // 注释：返回当前处理结果。
        return listResponse(mobileService.listApps());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/apps")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "移动端", action = "保存移动应用")
    public Result<DynamicRecordResponse> saveApp(@RequestBody MobileAppRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(mobileService.saveApp(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/apps/{appId}/pages")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> pages(@PathVariable String appId) {
        // 注释：返回当前处理结果。
        return listResponse(mobileService.listPages(appId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/pages")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "移动端", action = "保存移动页面")
    public Result<DynamicRecordResponse> savePage(@RequestBody MobilePageRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(mobileService.savePage(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/apps/{appId}/api-routes")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mobile:list')")
    public Result<List<DynamicRecordResponse>> apiRoutes(@PathVariable String appId) {
        // 注释：返回当前处理结果。
        return listResponse(mobileService.listApiRoutes(appId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/api-routes")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mobile:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "移动端", action = "保存移动 API 路由")
    public Result<DynamicRecordResponse> saveApiRoute(@RequestBody MobileApiRouteRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(mobileService.saveApiRoute(request));
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
