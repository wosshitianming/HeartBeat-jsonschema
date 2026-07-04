package top.kx.heartbeat.infrastructure.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.annotation.Resource;

/**
 * Quartz 统一 Job 入口：从 JobDataMap 读取 invokeTarget 并委派执行。
 */
@Slf4j
public class HeartbeatQuartzJob extends QuartzJobBean {

    @Resource
    private QuartzJobExecutor quartzJobExecutor;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String invokeTarget = context.getMergedJobDataMap().getString("invokeTarget");
        String jobId = context.getMergedJobDataMap().getString("jobId");
        try {
            quartzJobExecutor.execute(invokeTarget, jobId);
        } catch (RuntimeException ex) {
            log.error("Quartz 任务执行失败: jobId={}, target={}", jobId, invokeTarget, ex);
            throw new JobExecutionException(ex);
        }
    }
}
