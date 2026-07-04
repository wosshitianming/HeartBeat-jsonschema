package top.kx.heartbeat.interfaces.flow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.flow.NodeComponentRegistryService;
import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;
import top.kx.heartbeat.interfaces.common.Result;

import javax.annotation.Resource;
import java.util.List;

/**
 * 流程节点组件接口控制器。
 *
 * <p>负责节点组件清单查询和节点组件注册入口。</p>
 */
@RestController
@RequestMapping("/api/v1/flow/components")
public class NodeComponentController {

    /**
     * 节点组件注册应用服务。
     */
    @Resource
    private NodeComponentRegistryService registryService;

    /**
     * 查询启用节点组件列表。
     *
     * @return 启用节点组件列表响应
     */
    @GetMapping
    @PreAuthorize("@permissionGuard.has('flow:studio:list')")
    public Result<List<NodeComponentManifest>> list() {
        // 查询启用节点组件列表。
        List<NodeComponentManifest> manifests = registryService.listActive();
        // 返回启用节点组件列表。
        return Result.success(manifests);
    }

    /**
     * 注册节点组件。
     *
     * @param manifest 节点组件清单
     * @return 节点组件注册结果
     */
    @PostMapping
    @PreAuthorize("@permissionGuard.has('flow:component:edit')")
    public Result<NodeComponentManifest> register(@RequestBody NodeComponentManifest manifest) {
        // 注册节点组件清单。
        NodeComponentManifest saved = registryService.register(manifest);
        // 返回节点组件注册结果。
        return Result.success(saved);
    }
}
