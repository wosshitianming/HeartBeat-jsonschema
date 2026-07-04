package top.kx.heartbeat.interfaces.flow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.flow.ConnectionCredentialService;
import top.kx.heartbeat.domain.flow.model.ConnectionCredential;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 流程连接凭证接口控制器。
 *
 * <p>负责连接凭证的查询、保存、连通性测试和删除入口。</p>
 */
@RestController
@RequestMapping("/api/v1/flow/connections")
public class ConnectionCredentialController {

    /**
     * 连接凭证应用服务。
     */
    @Resource
    private ConnectionCredentialService credentialService;

    /**
     * 查询连接凭证列表。
     *
     * @return 连接凭证列表响应
     */
    @GetMapping
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<ConnectionCredential>> list() {
        // 查询连接凭证列表。
        List<ConnectionCredential> credentials = credentialService.list();
        // 返回连接凭证列表。
        return Result.success(credentials);
    }

    /**
     * 创建连接凭证。
     *
     * @param credential 连接凭证保存对象
     * @return 连接凭证创建结果
     */
    @PostMapping
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<ConnectionCredential> create(@RequestBody ConnectionCredential credential) {
        // 保存连接凭证。
        ConnectionCredential saved = credentialService.save(credential);
        // 返回连接凭证创建结果。
        return Result.success(saved);
    }

    /**
     * 修改连接凭证。
     *
     * @param id 连接凭证标识
     * @param credential 连接凭证保存对象
     * @return 连接凭证修改结果
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<ConnectionCredential> update(@PathVariable String id,
                                               @RequestBody ConnectionCredential credential) {
        // 使用路径标识覆盖请求体标识。
        credential.setId(id);
        // 保存连接凭证。
        ConnectionCredential saved = credentialService.save(credential);
        // 返回连接凭证修改结果。
        return Result.success(saved);
    }

    /**
     * 测试连接凭证。
     *
     * @param id 连接凭证标识
     * @return 连接凭证测试结果
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<DynamicRecordResponse> test(@PathVariable String id) {
        // 测试连接凭证并返回动态测试结果。
        Map<String, Object> tested = credentialService.test(id);
        // 将动态测试结果转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(tested);
        // 返回连接凭证测试结果。
        return Result.success(response);
    }

    /**
     * 删除连接凭证。
     *
     * @param id 连接凭证标识
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionGuard.has('flow:definition:edit')")
    public Result<Void> delete(@PathVariable String id) {
        // 删除连接凭证。
        credentialService.delete(id);
        // 返回删除成功响应。
        return Result.success();
    }
}
