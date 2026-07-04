package top.kx.heartbeat.interfaces.mp;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.mp.MpService;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.request.DynamicRecordRequest;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 公众号管理接口控制器。
 *
 * <p>负责公众号账号、菜单、素材和自动回复的后台管理入口。</p>
 */
@RestController
@RequestMapping("/api/v1/mp")
public class MpController {

    /**
     * 公众号应用服务。
     */
    @Resource
    private MpService mpService;

    /**
     * 查询公众号账号列表。
     *
     * @return 公众号账号列表响应
     */
    @GetMapping("/accounts")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> accounts() {
        // 查询公众号账号动态记录列表。
        List<Map<String, Object>> accounts = mpService.listAccounts();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(accounts);
        // 返回公众号账号列表。
        return Result.success(response);
    }

    /**
     * 查询公众号账号详情。
     *
     * @param id 公众号账号标识
     * @return 公众号账号详情响应
     */
    @GetMapping("/accounts/{id}")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<DynamicRecordResponse> account(@PathVariable String id) {
        // 查询公众号账号动态记录。
        Map<String, Object> account = mpService.getAccount(id);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(account);
        // 返回公众号账号详情。
        return Result.success(response);
    }

    /**
     * 保存公众号账号。
     *
     * @param request 公众号账号保存参数
     * @return 公众号账号保存结果
     */
    @PostMapping("/accounts")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号账号")
    public Result<DynamicRecordResponse> saveAccount(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存公众号账号动态记录。
        Map<String, Object> account = mpService.saveAccount(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(account);
        // 返回公众号账号保存结果。
        return Result.success(response);
    }

    /**
     * 查询公众号菜单列表。
     *
     * @param accountId 公众号账号标识
     * @return 公众号菜单列表响应
     */
    @GetMapping("/accounts/{accountId}/menus")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> menus(@PathVariable String accountId) {
        // 查询公众号菜单动态记录列表。
        List<Map<String, Object>> menus = mpService.listMenus(accountId);
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(menus);
        // 返回公众号菜单列表。
        return Result.success(response);
    }

    /**
     * 保存公众号菜单。
     *
     * @param request 公众号菜单保存参数
     * @return 公众号菜单保存结果
     */
    @PostMapping("/menus")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号菜单")
    public Result<DynamicRecordResponse> saveMenu(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存公众号菜单动态记录。
        Map<String, Object> menu = mpService.saveMenu(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(menu);
        // 返回公众号菜单保存结果。
        return Result.success(response);
    }

    /**
     * 同步公众号菜单。
     *
     * @param accountId 公众号账号标识
     * @return 公众号菜单同步结果
     */
    @PostMapping("/accounts/{accountId}/menus/sync")
    @PreAuthorize("@permissionGuard.has('biz:mp:sync')")
    @OperLog(module = "公众号", action = "同步公众号菜单")
    public Result<DynamicRecordResponse> syncMenu(@PathVariable String accountId) {
        // 同步公众号菜单动态记录。
        Map<String, Object> menu = mpService.syncMenu(accountId);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(menu);
        // 返回公众号菜单同步结果。
        return Result.success(response);
    }

    /**
     * 查询公众号素材列表。
     *
     * @param accountId 公众号账号标识
     * @return 公众号素材列表响应
     */
    @GetMapping("/accounts/{accountId}/materials")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> materials(@PathVariable String accountId) {
        // 查询公众号素材动态记录列表。
        List<Map<String, Object>> materials = mpService.listMaterials(accountId);
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(materials);
        // 返回公众号素材列表。
        return Result.success(response);
    }

    /**
     * 保存公众号素材。
     *
     * @param request 公众号素材保存参数
     * @return 公众号素材保存结果
     */
    @PostMapping("/materials")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存公众号素材")
    public Result<DynamicRecordResponse> saveMaterial(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存公众号素材动态记录。
        Map<String, Object> material = mpService.saveMaterial(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(material);
        // 返回公众号素材保存结果。
        return Result.success(response);
    }

    /**
     * 查询公众号自动回复列表。
     *
     * @param accountId 公众号账号标识
     * @return 公众号自动回复列表响应
     */
    @GetMapping("/accounts/{accountId}/auto-replies")
    @PreAuthorize("@permissionGuard.has('biz:mp:list')")
    public Result<List<DynamicRecordResponse>> autoReplies(@PathVariable String accountId) {
        // 查询公众号自动回复动态记录列表。
        List<Map<String, Object>> replies = mpService.listAutoReplies(accountId);
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(replies);
        // 返回公众号自动回复列表。
        return Result.success(response);
    }

    /**
     * 保存公众号自动回复。
     *
     * @param request 公众号自动回复保存参数
     * @return 公众号自动回复保存结果
     */
    @PostMapping("/auto-replies")
    @PreAuthorize("@permissionGuard.has('biz:mp:edit')")
    @OperLog(module = "公众号", action = "保存自动回复")
    public Result<DynamicRecordResponse> saveAutoReply(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存公众号自动回复动态记录。
        Map<String, Object> reply = mpService.saveAutoReply(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(reply);
        // 返回公众号自动回复保存结果。
        return Result.success(response);
    }
}
