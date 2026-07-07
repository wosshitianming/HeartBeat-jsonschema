// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.interfaces.mp;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.mp.MpService;
import top.kx.heartbeat.application.mp.request.MpAccountRequest;
import top.kx.heartbeat.application.mp.request.MpAutoReplyRequest;
import top.kx.heartbeat.application.mp.request.MpMaterialRequest;
import top.kx.heartbeat.application.mp.request.MpMenuRequest;
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
@RequestMapping("/api/v1/mp")
public class MpController {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private MpService mpService;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/accounts")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> accounts() {
        // 注释：返回当前处理结果。
        return listResponse(mpService.listAccounts());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/accounts/{id}")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<DynamicRecordResponse> account(@PathVariable String id) {
        // 注释：返回当前处理结果。
        return recordResponse(mpService.getAccount(id));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/accounts")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "公众号", action = "保存公众号账号")
    public Result<DynamicRecordResponse> saveAccount(@RequestBody MpAccountRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(mpService.saveAccount(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/accounts/{accountId}/menus")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> menus(@PathVariable String accountId) {
        // 注释：返回当前处理结果。
        return listResponse(mpService.listMenus(accountId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/menus")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "公众号", action = "保存公众号菜单")
    public Result<DynamicRecordResponse> saveMenu(@RequestBody MpMenuRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(mpService.saveMenu(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/accounts/{accountId}/menus/sync")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:sync')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "公众号", action = "同步公众号菜单")
    public Result<DynamicRecordResponse> syncMenu(@PathVariable String accountId) {
        // 注释：返回当前处理结果。
        return recordResponse(mpService.syncMenu(accountId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/accounts/{accountId}/materials")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> materials(@PathVariable String accountId) {
        // 注释：返回当前处理结果。
        return listResponse(mpService.listMaterials(accountId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/materials")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "公众号", action = "保存公众号素材")
    public Result<DynamicRecordResponse> saveMaterial(@RequestBody MpMaterialRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(mpService.saveMaterial(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/accounts/{accountId}/auto-replies")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> autoReplies(@PathVariable String accountId) {
        // 注释：返回当前处理结果。
        return listResponse(mpService.listAutoReplies(accountId));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/auto-replies")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "公众号", action = "保存自动回复")
    public Result<DynamicRecordResponse> saveAutoReply(@RequestBody MpAutoReplyRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(mpService.saveAutoReply(request));
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
