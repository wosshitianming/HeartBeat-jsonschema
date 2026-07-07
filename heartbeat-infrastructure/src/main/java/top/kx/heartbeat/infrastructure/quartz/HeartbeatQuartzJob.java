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
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String invokeTarget = context.getMergedJobDataMap().getString("invokeTarget");
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String jobId = context.getMergedJobDataMap().getString("jobId");
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            quartzJobExecutor.execute(invokeTarget, jobId);
        } catch (RuntimeException ex) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            log.error("Quartz 任务执行失败: jobId={}, target={}", jobId, invokeTarget, ex);
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new JobExecutionException(ex);
        }
    }
}
