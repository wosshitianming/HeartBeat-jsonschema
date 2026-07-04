package top.kx.heartbeat.interfaces.monitor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.kx.heartbeat.application.monitor.MonitorService;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 系统监控接口控制器。
 *
 * <p>负责服务器、缓存和数据源监控信息的 HTTP 协议适配。</p>
 */
@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorController {

    /**
     * 系统监控应用服务。
     */
    @Resource
    private MonitorService monitorService;

    /**
     * 查询服务器监控信息。
     *
     * @return 服务器监控响应
     */
    @GetMapping("/server")
    @PreAuthorize("@permissionGuard.has('monitor:server:list')")
    public Result<DynamicRecordResponse> server() {
        // 查询服务器动态监控记录。
        Map<String, Object> serverInfo = monitorService.serverInfo();
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(serverInfo);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询缓存监控信息。
     *
     * @return 缓存监控响应
     */
    @GetMapping("/cache")
    @PreAuthorize("@permissionGuard.has('monitor:cache:list')")
    public Result<DynamicRecordResponse> cache() {
        // 查询缓存动态监控记录。
        Map<String, Object> cacheInfo = monitorService.cacheInfo();
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(cacheInfo);
        // 返回统一接口响应。
        return Result.success(response);
    }

    /**
     * 查询数据源监控信息。
     *
     * @return 数据源监控响应
     */
    @GetMapping("/druid")
    @PreAuthorize("@permissionGuard.has('monitor:druid:list')")
    public Result<DynamicRecordResponse> dataSource() {
        // 查询数据源动态监控记录。
        Map<String, Object> dataSourceInfo = monitorService.dataSourceInfo();
        // 转换为统一动态响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(dataSourceInfo);
        // 返回统一接口响应。
        return Result.success(response);
    }
}
