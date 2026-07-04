package top.kx.heartbeat.interfaces.tool;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.kx.heartbeat.application.tool.QuartzJobService;
import top.kx.heartbeat.interfaces.common.Result;

import javax.annotation.Resource;

/**
 * 定时任务工具接口控制器。
 *
 * <p>负责定时任务立即执行、暂停、恢复和调度刷新入口。</p>
 */
@RestController
@RequestMapping("/api/v1/tool/jobs")
public class ToolJobController {

    /**
     * 定时任务应用服务。
     */
    @Resource
    private QuartzJobService quartzJobService;

    /**
     * 立即执行一次任务。
     *
     * @param id 定时任务标识
     * @return 执行触发结果
     */
    @PostMapping("/{id}/run")
    @PreAuthorize("@permissionGuard.has('tool:job:run')")
    public Result<Void> run(@PathVariable String id) {
        // 触发定时任务立即执行。
        quartzJobService.runNow(id);
        // 返回触发成功响应。
        return Result.success();
    }

    /**
     * 暂停任务调度。
     *
     * @param id 定时任务标识
     * @return 暂停结果
     */
    @PostMapping("/{id}/pause")
    @PreAuthorize("@permissionGuard.has('tool:job:edit')")
    public Result<Void> pause(@PathVariable String id) {
        // 暂停定时任务调度。
        quartzJobService.pause(id);
        // 返回暂停成功响应。
        return Result.success();
    }

    /**
     * 恢复任务调度。
     *
     * @param id 定时任务标识
     * @return 恢复结果
     */
    @PostMapping("/{id}/resume")
    @PreAuthorize("@permissionGuard.has('tool:job:edit')")
    public Result<Void> resume(@PathVariable String id) {
        // 恢复定时任务调度。
        quartzJobService.resume(id);
        // 返回恢复成功响应。
        return Result.success();
    }

    /**
     * 刷新任务调度器。
     *
     * @return 调度器刷新结果
     */
    @PostMapping("/refresh")
    @PreAuthorize("@permissionGuard.has('tool:job:edit')")
    public Result<Void> refresh() {
        // 全量刷新 Quartz 调度器。
        quartzJobService.refreshScheduler();
        // 返回刷新成功响应。
        return Result.success();
    }
}
