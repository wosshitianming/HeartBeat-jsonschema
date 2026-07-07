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
 * 提供公众号管理 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/mp")
public class MpController {

    @Resource
    private MpService mpService;

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托公众号管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/accounts")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> accounts() {
        return listResponse(mpService.listAccounts());
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托公众号管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/accounts/{id}")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<DynamicRecordResponse> account(@PathVariable String id) {
        return recordResponse(mpService.getAccount(id));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托公众号管理应用服务完成处理。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/accounts")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号账号")
    public Result<DynamicRecordResponse> saveAccount(@RequestBody MpAccountRequest request) {
        return recordResponse(mpService.saveAccount(request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托公众号管理应用服务完成处理。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/accounts/{accountId}/menus")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> menus(@PathVariable String accountId) {
        return listResponse(mpService.listMenus(accountId));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托公众号管理应用服务完成处理。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/menus")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号菜单")
    public Result<DynamicRecordResponse> saveMenu(@RequestBody MpMenuRequest request) {
        return recordResponse(mpService.saveMenu(request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托公众号管理应用服务完成处理。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @PostMapping("/accounts/{accountId}/menus/sync")
    @PreAuthorize("@permissionGuard.has('biz:mp:sync')")
    @OperLog(module = "公众号", action = "同步公众号菜单")
    public Result<DynamicRecordResponse> syncMenu(@PathVariable String accountId) {
        return recordResponse(mpService.syncMenu(accountId));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托公众号管理应用服务完成处理。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/accounts/{accountId}/materials")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> materials(@PathVariable String accountId) {
        return listResponse(mpService.listMaterials(accountId));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托公众号管理应用服务完成处理。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/materials")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号素材")
    public Result<DynamicRecordResponse> saveMaterial(@RequestBody MpMaterialRequest request) {
        return recordResponse(mpService.saveMaterial(request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托公众号管理应用服务完成处理。
     *
     * @param accountId 业务记录标识。
     * @return 处理后的业务结果。
     */
    @GetMapping("/accounts/{accountId}/auto-replies")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> autoReplies(@PathVariable String accountId) {
        return listResponse(mpService.listAutoReplies(accountId));
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托公众号管理应用服务完成处理。
     *
     * @param request 公众号管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/auto-replies")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存自动回复")
    public Result<DynamicRecordResponse> saveAutoReply(@RequestBody MpAutoReplyRequest request) {
        return recordResponse(mpService.saveAutoReply(request));
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托公众号管理应用服务完成处理。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托公众号管理应用服务完成处理。
     *
     * @param record 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
