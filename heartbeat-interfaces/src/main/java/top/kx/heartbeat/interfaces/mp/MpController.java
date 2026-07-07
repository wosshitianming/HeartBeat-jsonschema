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

@RestController
@RequestMapping("/api/v1/mp")
public class MpController {

    @Resource
    private MpService mpService;

    @GetMapping("/accounts")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> accounts() {
        return listResponse(mpService.listAccounts());
    }

    @GetMapping("/accounts/{id}")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<DynamicRecordResponse> account(@PathVariable String id) {
        return recordResponse(mpService.getAccount(id));
    }

    @PostMapping("/accounts")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号账号")
    public Result<DynamicRecordResponse> saveAccount(@RequestBody MpAccountRequest request) {
        return recordResponse(mpService.saveAccount(request));
    }

    @GetMapping("/accounts/{accountId}/menus")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> menus(@PathVariable String accountId) {
        return listResponse(mpService.listMenus(accountId));
    }

    @PostMapping("/menus")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号菜单")
    public Result<DynamicRecordResponse> saveMenu(@RequestBody MpMenuRequest request) {
        return recordResponse(mpService.saveMenu(request));
    }

    @PostMapping("/accounts/{accountId}/menus/sync")
    @PreAuthorize("@permissionGuard.has('biz:mp:sync')")
    @OperLog(module = "公众号", action = "同步公众号菜单")
    public Result<DynamicRecordResponse> syncMenu(@PathVariable String accountId) {
        return recordResponse(mpService.syncMenu(accountId));
    }

    @GetMapping("/accounts/{accountId}/materials")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> materials(@PathVariable String accountId) {
        return listResponse(mpService.listMaterials(accountId));
    }

    @PostMapping("/materials")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号素材")
    public Result<DynamicRecordResponse> saveMaterial(@RequestBody MpMaterialRequest request) {
        return recordResponse(mpService.saveMaterial(request));
    }

    @GetMapping("/accounts/{accountId}/auto-replies")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> autoReplies(@PathVariable String accountId) {
        return listResponse(mpService.listAutoReplies(accountId));
    }

    @PostMapping("/auto-replies")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存自动回复")
    public Result<DynamicRecordResponse> saveAutoReply(@RequestBody MpAutoReplyRequest request) {
        return recordResponse(mpService.saveAutoReply(request));
    }

    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
