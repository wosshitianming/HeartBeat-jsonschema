package top.kx.heartbeat.infrastructure.quartz;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.kx.heartbeat.domain.tool.QuartzJobScheduler;
import top.kx.heartbeat.domain.tool.model.ScheduledJob;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "heartbeat.middleware.quartz", name = "enabled", havingValue = "true")
public class QuartzSchedulerManager implements QuartzJobScheduler {

    @Resource
    private Scheduler scheduler;

    @Override
    public void refreshJobs(List<ScheduledJob> jobs) {
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (ScheduledJob job : jobs) {
            // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
            try {
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                scheduleJob(job);
            } catch (SchedulerException ex) {
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                log.warn("同步任务失败: {} - {}", job.getJobCode(), ex.getMessage());
            }
        }
    }

    @Override
    public void runNow(String jobId, String jobGroup) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            scheduler.triggerJob(JobKey.jobKey(jobId, jobGroup));
        } catch (SchedulerException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("手动执行任务失败: " + jobId, ex);
        }
    }

    @Override
    public void pause(String jobId, String jobGroup) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            scheduler.pauseJob(JobKey.jobKey(jobId, jobGroup));
        } catch (SchedulerException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("暂停任务失败: " + jobId, ex);
        }
    }

    @Override
    public void resume(String jobId, String jobGroup) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            scheduler.resumeJob(JobKey.jobKey(jobId, jobGroup));
        } catch (SchedulerException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("恢复任务失败: " + jobId, ex);
        }
    }

    private void scheduleJob(ScheduledJob job) throws SchedulerException {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String jobCode = job.getJobCode();
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        String jobGroup = hasText(job.getJobGroup()) ? job.getJobGroup() : "DEFAULT";
        // 根据当前业务条件选择对应处理路径。
        if (!hasText(job.getCronExpression()) || !hasText(job.getInvokeTarget())) {
            // 返回已经完成封装的业务结果。
            return;
        }
        // 根据当前业务条件选择对应处理路径。
        if (!"ACTIVE".equalsIgnoreCase(job.getStatus())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                && !"ENABLED".equalsIgnoreCase(job.getStatus())) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            deleteJob(jobCode, jobGroup);
            // 返回已经完成封装的业务结果。
            return;
        }
        // 计算当前分支的中间结果，供后续判断或组装使用。
        JobKey jobKey = JobKey.jobKey(jobCode, jobGroup);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        TriggerKey triggerKey = TriggerKey.triggerKey(jobCode, jobGroup);
        // 根据当前业务条件选择对应处理路径。
        if (scheduler.checkExists(jobKey)) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            scheduler.deleteJob(jobKey);
        }
        // 计算当前分支的中间结果，供后续判断或组装使用。
        JobDetail jobDetail = JobBuilder.newJob(HeartbeatQuartzJob.class)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .withIdentity(jobKey)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .usingJobData("invokeTarget", job.getInvokeTarget())
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .usingJobData("jobId", jobCode)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .storeDurably()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .build();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Trigger trigger = TriggerBuilder.newTrigger()
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .withIdentity(triggerKey)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .forJob(jobDetail)
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                .build();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        scheduler.scheduleJob(jobDetail, trigger);
    }

    private void deleteJob(String jobId, String jobGroup) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    private boolean hasText(String value) {
        return StringUtils.isNotBlank(value);
    }
}
