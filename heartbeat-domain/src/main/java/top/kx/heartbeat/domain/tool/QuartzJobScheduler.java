package top.kx.heartbeat.domain.tool;

import top.kx.heartbeat.domain.tool.model.ScheduledJob;

import java.util.List;

/**
 * 定时任务调度端口：将 sys_job 配置同步到 Quartz 并支持运行控制。
 */
public interface QuartzJobScheduler {

    /**
     * 根据任务配置全量刷新 Quartz 调度器。
     */
    void refreshJobs(List<ScheduledJob> jobs);

    /**
     * 立即触发一次任务。
     */
    void runNow(String jobId, String jobGroup);

    /**
     * 暂停任务。
     */
    void pause(String jobId, String jobGroup);

    /**
     * 恢复任务。
     */
    void resume(String jobId, String jobGroup);
}
